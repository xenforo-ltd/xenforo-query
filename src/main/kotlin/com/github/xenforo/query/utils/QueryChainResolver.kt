package com.github.xenforo.query.utils

import com.github.xenforo.query.constants.BuilderMethods
import com.intellij.database.util.DbUtil
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.ArrayHashElement
import com.jetbrains.php.lang.psi.elements.AssignmentExpression
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.elements.Variable
import java.util.ArrayDeque

object QueryChainResolver {
    data class TableContext(
        val baseTable: String,
        val alias: String?,
        val joinType: String? = null,
        val joinCondition: String? = null,
    )

    /**
     * Resolves all tables in the query builder chain, including those defined after the start method.
     * Uses two-phase resolution: finds the chain root, then traverses forward to collect all tables.
     * Results are cached at the file level for performance.
     */
    fun resolveTables(startMethod: MethodReference): List<TableContext> {
        val project = startMethod.project
        val file = startMethod.containingFile ?: return emptyList()

        // Store the offset of startMethod to find the chain root again inside the provider
        // This avoids capturing PSI elements in the lambda
        val startMethodOffset = startMethod.textOffset

        // Create a unique cache key for this specific chain (based on the start method offset)
        val cacheKey =
            Key.create<CachedValue<List<TableContext>>>(
                "xenforo.query.chain.tables.cache.$startMethodOffset",
            )

        return CachedValuesManager.getManager(project).getCachedValue(
            file,
            cacheKey,
            {
                // Find the element at the stored offset and traverse to find root
                // This is done INSIDE the lambda to avoid capturing PSI from outside
                var current: PsiElement? = file.findElementAt(startMethodOffset)

                // Walk up to find the MethodReference at this position
                while (current != null && current !is MethodReference) {
                    current = current.parent
                }

                val startRef = current
                val root = if (startRef != null) findChainRoot(startRef) else null

                // Collect all tables from root using forward traversal
                val tables = mutableListOf<TableContext>()
                val visited = mutableSetOf<PsiElement>()
                collectAllTablesFromRoot(root, tables, visited)

                // Create dependencies for cache invalidation
                val dependencies = mutableListOf<Any>(file)
                try {
                    dependencies.addAll(DbUtil.getDataSources(project).mapNotNull { it.modificationTracker })
                } catch (_: Exception) {
                    // Database plugin not available, ignore
                }

                CachedValueProvider.Result.create(tables.toList(), dependencies)
            },
            false,
        )
    }

    /**
     * Finds the root element of a method chain by walking backwards through classReferences.
     */
    private fun findChainRoot(startElement: PsiElement?): PsiElement? {
        var current = startElement
        while (current is MethodReference && current.classReference != null) {
            current = current.classReference
        }
        return current
    }

    /**
     * Collects all tables from the chain root using forward traversal.
     * Uses a queue-based approach to handle branching (e.g., when a method is used multiple times).
     */
    private fun collectAllTablesFromRoot(
        root: PsiElement?,
        tables: MutableList<TableContext>,
        visited: MutableSet<PsiElement>,
    ) {
        if (root == null) return

        val queue = ArrayDeque<PsiElement>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current in visited) continue
            visited.add(current)

            when (current) {
                is MethodReference -> {
                    // Extract table if this is a table-defining method
                    extractTableFromMethod(current, tables)

                    // Find all methods that use this as their classReference (forward traversal)
                    val nextMethods = findNextMethodsInChain(current, visited)
                    queue.addAll(nextMethods)

                    // Also continue backward traversal for variable assignments
                    if (current.classReference != null) {
                        queue.add(current.classReference!!)
                    }
                }
                is com.jetbrains.php.lang.psi.elements.ClassReference -> {
                    // For class references (like \XF), find methods that reference this class
                    // This handles the case where chain starts with \XF::query(...)
                    val nextMethods = findNextMethodsInChain(current, visited)
                    queue.addAll(nextMethods)
                }
                is Variable -> {
                    // First, try enhanced closure resolution (for closure parameters)
                    val closureTables = resolveClosureWithTables(current)
                    if (closureTables != null) {
                        // This is a closure parameter - add all tables from closure resolution
                        tables.addAll(closureTables)
                        // Also continue traversing the parent method chain
                        val parentMethod = resolveClosureParameterToMethod(current)
                        if (parentMethod != null && parentMethod !in visited) {
                            queue.add(parentMethod)
                        }
                    } else {
                        // Not a closure parameter - try variable assignment resolution
                        val resolved = resolveVariableAssignment(current)
                        if (resolved != null && resolved !in visited) {
                            // Find the root of the resolved chain to maintain correct order
                            val resolvedRoot =
                                if (resolved is MethodReference) {
                                    findChainRoot(resolved)
                                } else {
                                    resolved
                                }
                            if (resolvedRoot != null) {
                                queue.add(resolvedRoot)
                            }
                        }
                    }
                }
                is StringLiteralExpression -> {
                    extractTableAndAlias(current.contents, tables, null, null)
                }
            }
        }
    }

    /**
     * Finds all MethodReferences in the file that use the given element as their classReference.
     * This enables forward traversal of the method chain.
     */
    private fun findNextMethodsInChain(
        element: PsiElement,
        visited: MutableSet<PsiElement>,
    ): List<MethodReference> {
        val containingFile = element.containingFile ?: return emptyList()

        // Find all MethodReferences where this element is their classReference
        return PsiTreeUtil.findChildrenOfType(containingFile, MethodReference::class.java)
            .filter { methodRef ->
                methodRef.classReference == element && methodRef !in visited
            }
    }

    /**
     * Extracts table information from a method call if it's a table-defining method.
     */
    private fun extractTableFromMethod(
        methodRef: MethodReference,
        tables: MutableList<TableContext>,
    ) {
        val name = methodRef.name
        val args = methodRef.parameterList?.parameters

        if (name != null && name in BuilderMethods.TableMethods && args?.isNotEmpty() == true) {
            when {
                name == "query" || name == "table" -> {
                    extractTableAndAlias(args[0].text, tables, null, null)
                }
                name.lowercase().endsWith("join") && args.size >= 4 -> {
                    val joinType =
                        when (name.lowercase()) {
                            "leftjoin" -> "LEFT JOIN"
                            "rightjoin" -> "RIGHT JOIN"
                            else -> "JOIN"
                        }
                    val leftCol = extractStringContent(args[1])
                    val operator = args[2].text
                    val rightCol = extractStringContent(args[3])
                    val joinCondition = "$leftCol$operator$rightCol"
                    extractTableAndAlias(args[0].text, tables, joinType, joinCondition)
                }
            }
        }
    }

    /**
     * Finds the latest assignment to a variable that appears before its usage.
     */
    private fun resolveVariableAssignment(variable: Variable): PsiElement? {
        val variableName = variable.name
        val containingFile = variable.containingFile ?: return null

        val assignments = PsiTreeUtil.findChildrenOfType(containingFile, AssignmentExpression::class.java)

        var latestAssignment: PsiElement? = null
        var latestOffset = -1

        for (assignment in assignments) {
            val assignedVar = assignment.variable
            if (assignedVar is Variable && assignedVar.name == variableName) {
                if (assignment.textOffset < variable.textOffset && assignment.textOffset > latestOffset) {
                    val value = assignment.value
                    if (value is MethodReference) {
                        latestAssignment = value
                        latestOffset = assignment.textOffset
                    }
                }
            }
        }

        return latestAssignment
    }

    /**
     * When a variable is a closure parameter, resolves to the method call containing the closure.
     *
     * Example: `->where(function (Builder $query) { $query->... })` - when resolving `$query`,
     * this returns the `->where()` MethodReference.
     */
    private fun resolveClosureParameterToMethod(variable: Variable): MethodReference? {
        val variableName = variable.name

        val enclosingFunction = PsiTreeUtil.getParentOfType(variable, Function::class.java) ?: return null

        val isParameter = enclosingFunction.parameters.any { param -> param.name == variableName }
        if (!isParameter) return null

        return PsiTreeUtil.getParentOfType(enclosingFunction, MethodReference::class.java)
    }

    /**
     * Enhanced closure resolution that finds tables from both the outer chain and inside the closure.
     *
     * When a join is made inside a closure (e.g., $query->join(...) inside a where() closure),
     * this method merges tables from:
     * 1. The chain leading to the method containing the closure
     * 2. Any table-defining calls (join, etc.) made on the closure parameter inside the closure
     */
    fun resolveClosureWithTables(variable: Variable): List<TableContext>? {
        val variableName = variable.name

        // Find the enclosing closure/function
        val enclosingFunction = PsiTreeUtil.getParentOfType(variable, Function::class.java) ?: return null

        // Verify this variable is actually a parameter of the closure
        val isParameter = enclosingFunction.parameters.any { param -> param.name == variableName }
        if (!isParameter) return null

        // Get the method that contains this closure (e.g., the ->where() method)
        val parentMethod =
            PsiTreeUtil.getParentOfType(enclosingFunction, MethodReference::class.java)
                ?: return null

        // Phase 1: Get tables from the chain leading to the parent method
        val outerTables = resolveTables(parentMethod).toMutableList()

        // Phase 2: Scan the closure body for any table-defining calls on this parameter
        val closureTables = mutableListOf<TableContext>()
        scanClosureForTables(enclosingFunction, variableName, closureTables)

        // Phase 3: Merge tables (closure tables override outer tables with same alias)
        val tableMap = outerTables.associateBy { it.alias ?: it.baseTable }.toMutableMap()
        closureTables.forEach { ctx ->
            val key = ctx.alias ?: ctx.baseTable
            tableMap[key] = ctx
        }

        return tableMap.values.toList()
    }

    /**
     * Scans a closure/function body for table-defining method calls on a specific variable.
     *
     * This finds calls like $query->join('xf_user', ...) inside the closure body.
     */
    private fun scanClosureForTables(
        function: Function,
        paramName: String,
        tables: MutableList<TableContext>,
    ) {
        // Find all method calls inside this function
        val methodCalls = PsiTreeUtil.findChildrenOfType(function, MethodReference::class.java)

        methodCalls.forEach { methodRef ->
            val methodName = methodRef.name

            // Only process table-defining methods
            if (methodName != null && methodName in BuilderMethods.TableMethods) {
                // Check if this method is called on the closure parameter
                val classRef = methodRef.classReference
                if (classRef is Variable && classRef.name == paramName) {
                    val args = methodRef.parameterList?.parameters
                    if (args?.isNotEmpty() == true) {
                        when {
                            methodName == "query" || methodName == "table" -> {
                                extractTableAndAlias(args[0].text, tables, null, null)
                            }
                            methodName.lowercase().endsWith("join") && args.size >= 4 -> {
                                val joinType =
                                    when (methodName.lowercase()) {
                                        "leftjoin" -> "LEFT JOIN"
                                        "rightjoin" -> "RIGHT JOIN"
                                        else -> "JOIN"
                                    }
                                val leftCol = extractStringContent(args[1])
                                val operator = args[2].text
                                val rightCol = extractStringContent(args[3])
                                val joinCondition = "$leftCol$operator$rightCol"
                                extractTableAndAlias(args[0].text, tables, joinType, joinCondition)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun extractTableAndAlias(
        tableArgumentText: String,
        tables: MutableList<TableContext>,
        joinType: String? = null,
        joinCondition: String? = null,
    ) {
        val text = tableArgumentText.trim('\"', '\'').trim()
        val parts = text.split(Regex("(?i)\\s+as\\s+|\\s+"))
        val baseTable = parts.getOrNull(0)?.trim()
        val alias = parts.getOrNull(1)?.trim()

        if (!baseTable.isNullOrEmpty()) {
            tables.add(TableContext(baseTable, alias, joinType, joinCondition))
        }
    }

    private fun extractStringContent(element: PsiElement): String {
        return if (element is StringLiteralExpression) {
            element.contents
        } else {
            element.text.trim('\"', '\'')
        }
    }

    fun findMethodReference(position: PsiElement): MethodReference? {
        var current: PsiElement? = position.parent
        var depth = 0

        while (current != null && depth < 20) {
            if (current is MethodReference) return current
            current = current.parent
            depth++
        }

        return null
    }

    fun isStringLiteralArrayKeyInColumnArray(element: PsiElement): Boolean {
        return getColumnArrayPosition(element) == ColumnArrayPosition.KEY
    }

    fun isStringLiteralArrayValueInColumnArray(element: PsiElement): Boolean {
        return getColumnArrayPosition(element) == ColumnArrayPosition.VALUE
    }

    enum class ColumnArrayPosition {
        KEY, // First arg of upsert/update/insert - array keys are columns
        VALUE, // Second/third arg of upsert - array values are columns
        NONE, // Not a column array position
    }

    /**
     * Determines if a string literal is in a position that represents a column name
     * in an upsert/update/insert method call.
     *
     * For upsert(array $values, array $uniqueBy, ?array $update):
     * - First argument ($values): array keys are column names
     * - Second argument ($uniqueBy): array values are column names
     * - Third argument ($update): array values are column names
     *
     * This method only looks at direct children of the array, not nested structures
     * (e.g., arrays inside json_encode() calls or nested arrays are ignored).
     */
    fun getColumnArrayPosition(element: PsiElement): ColumnArrayPosition {
        val literal =
            PsiTreeUtil.getParentOfType(element, StringLiteralExpression::class.java, false)
                ?: return ColumnArrayPosition.NONE

        // Find the containing array creation first
        val arrayCreation =
            PsiTreeUtil.getParentOfType(
                literal,
                com.jetbrains.php.lang.psi.elements.ArrayCreationExpression::class.java,
            ) ?: return ColumnArrayPosition.NONE

        // Check if the literal is inside an ArrayHashElement (associative array)
        // or directly in the array (list array like ['a', 'b'])
        val arrayHash = PsiTreeUtil.getParentOfType(literal, ArrayHashElement::class.java)

        // Find the method reference that contains this array
        val methodRef =
            findMethodReference(arrayCreation)
                ?: return ColumnArrayPosition.NONE

        // Check if this is an upsert/update/insert method
        val methodName =
            methodRef.name
                ?: return ColumnArrayPosition.NONE
        if (!BuilderMethods.ColumnArrayMethods.contains(methodName)) {
            return ColumnArrayPosition.NONE
        }

        // Find which argument this array is
        val paramList =
            methodRef.parameterList
                ?: return ColumnArrayPosition.NONE
        val args = paramList.parameters

        val argIndex = args.indexOf(arrayCreation)
        if (argIndex < 0) {
            return ColumnArrayPosition.NONE
        }

        // For upsert:
        // - Arg 0: values array, keys are columns
        // - Arg 1: uniqueBy array, values are columns
        // - Arg 2: update array, values are columns
        // For insert/update: only arg 0 matters, keys are columns
        return when {
            argIndex == 0 -> {
                // First argument: keys are columns (for associative arrays)
                // List arrays in first arg are not columns
                if (arrayHash != null) {
                    val keyExpr = arrayHash.key
                    if (keyExpr != null && PsiTreeUtil.isAncestor(keyExpr, literal, false)) {
                        ColumnArrayPosition.KEY
                    } else {
                        ColumnArrayPosition.NONE
                    }
                } else {
                    // In a list array in first arg - not a column position
                    ColumnArrayPosition.NONE
                }
            }
            methodName == "upsert" && (argIndex == 1 || argIndex == 2) -> {
                // Second or third argument of upsert: values are columns
                if (arrayHash != null) {
                    // Associative array: check if literal is the value
                    val valueExpr = arrayHash.value
                    if (valueExpr != null && PsiTreeUtil.isAncestor(valueExpr, literal, false)) {
                        ColumnArrayPosition.VALUE
                    } else {
                        ColumnArrayPosition.NONE
                    }
                } else {
                    // List array (e.g., ['username', 'email']): items are column names
                    // The literal is directly in the array creation
                    ColumnArrayPosition.VALUE
                }
            }
            else -> ColumnArrayPosition.NONE
        }
    }
}

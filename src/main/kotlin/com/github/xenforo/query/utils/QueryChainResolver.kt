package com.github.xenforo.query.utils

import com.github.xenforo.query.constants.BuilderMethods
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.ArrayHashElement
import com.jetbrains.php.lang.psi.elements.AssignmentExpression
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.elements.Variable

object QueryChainResolver {
    data class TableContext(
        val baseTable: String,
        val alias: String?,
        val joinType: String? = null,
        val joinCondition: String? = null,
    )

    fun resolveTables(startMethod: MethodReference): List<TableContext> {
        val tables = mutableListOf<TableContext>()
        resolveTablesRecursive(startMethod, tables, mutableSetOf())
        return tables.reversed()
    }

    private fun resolveTablesRecursive(
        element: PsiElement?,
        tables: MutableList<TableContext>,
        visited: MutableSet<PsiElement>,
    ) {
        if (element == null || element in visited) return
        visited.add(element)

        when (element) {
            is MethodReference -> {
                val name = element.name
                val args = element.parameterList?.parameters

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

                resolveTablesRecursive(element.classReference, tables, visited)
            }

            is Variable -> {
                val closureMethod = resolveClosureParameterToMethod(element)
                if (closureMethod != null) {
                    resolveTablesRecursive(closureMethod, tables, visited)
                } else {
                    val resolved = resolveVariableAssignment(element)
                    if (resolved != null) {
                        resolveTablesRecursive(resolved, tables, visited)
                    }
                }
            }

            is StringLiteralExpression -> {
                extractTableAndAlias(element.contents, tables, null, null)
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
        KEY,      // First arg of upsert/update/insert - array keys are columns
        VALUE,    // Second/third arg of upsert - array values are columns
        NONE,     // Not a column array position
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
            PsiTreeUtil.getParentOfType(literal, com.jetbrains.php.lang.psi.elements.ArrayCreationExpression::class.java)
                ?: return ColumnArrayPosition.NONE

        // Check if the literal is inside an ArrayHashElement (associative array)
        // or directly in the array (list array like ['a', 'b'])
        val arrayHash = PsiTreeUtil.getParentOfType(literal, ArrayHashElement::class.java)

        // Find the method reference that contains this array
        val methodRef = findMethodReference(arrayCreation)
            ?: return ColumnArrayPosition.NONE

        // Check if this is an upsert/update/insert method
        val methodName = methodRef.name
            ?: return ColumnArrayPosition.NONE
        if (!BuilderMethods.ColumnArrayMethods.contains(methodName)) {
            return ColumnArrayPosition.NONE
        }

        // Find which argument this array is
        val paramList = methodRef.parameterList
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

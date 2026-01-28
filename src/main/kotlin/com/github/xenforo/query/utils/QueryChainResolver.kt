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
        if (element == null || element in visited) {
            return
        }
        visited.add(element)

        when (element) {
            is MethodReference ->
                {
                    val name = element.name
                    val args = element.parameterList?.parameters

                    if (name != null && name in BuilderMethods.TableMethods && args?.isNotEmpty() == true) {
                        when {
                            name == "query" || name == "table" ->
                                {
                                    extractTableAndAlias(args[0].text, tables, null, null)
                                }

                            name.lowercase().endsWith("join") && args.size >= 4 ->
                                {
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

                    // Continue up the chain
                    resolveTablesRecursive(element.classReference, tables, visited)
                }

            is Variable ->
                {
                    // First, check if this variable is a closure parameter
                    // e.g., ->where(function (Builder $query) { $query->... })
                    val closureMethod = resolveClosureParameterToMethod(element)
                    if (closureMethod != null) {
                        resolveTablesRecursive(closureMethod, tables, visited)
                    } else {
                        // Try to find the variable's assignment to trace back to its value
                        val resolved = resolveVariableAssignment(element)
                        if (resolved != null) {
                            resolveTablesRecursive(resolved, tables, visited)
                        }
                    }
                }

            is StringLiteralExpression ->
                {
                    extractTableAndAlias(element.contents, tables, null, null)
                }
        }
    }

    /**
     * Try to find the assignment expression for a variable and return the assigned value.
     * For example: $query = \XF::query('xf_user') -> returns the MethodReference
     *
     * If the variable is assigned multiple times, returns the latest assignment
     * that appears before the variable usage.
     */
    private fun resolveVariableAssignment(variable: Variable): PsiElement? {
        val variableName = variable.name ?: return null
        val containingFile = variable.containingFile ?: return null

        // Search for assignments to this variable in the same scope
        // We look for assignments that appear before this usage
        val assignments = PsiTreeUtil.findChildrenOfType(containingFile, AssignmentExpression::class.java)

        var latestAssignment: PsiElement? = null
        var latestOffset = -1

        for (assignment in assignments) {
            val assignedVar = assignment.variable
            if (assignedVar is Variable && assignedVar.name == variableName) {
                // Check if this assignment is before our variable usage
                // and is later than any previous matching assignment
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
     * Check if a variable is a closure parameter and resolve to the enclosing method call.
     *
     * For example, in:
     * ```php
     * \XF::query('xf_post')->where(function (Builder $query) {
     *     $query->where('col', 1);
     * });
     * ```
     *
     * When $query is used inside the closure, this method finds the ->where() method
     * that the closure is passed to, allowing us to trace back to the table.
     */
    private fun resolveClosureParameterToMethod(variable: Variable): MethodReference? {
        val variableName = variable.name ?: return null

        // Find the enclosing function/closure
        val enclosingFunction =
            PsiTreeUtil.getParentOfType(variable, Function::class.java)
                ?: return null

        // Check if this variable matches a parameter of the closure
        val isParameter =
            enclosingFunction.parameters.any { param ->
                param.name == variableName
            }

        if (!isParameter) {
            return null
        }

        // The closure should be an argument to a method call
        // Walk up to find the MethodReference that contains this closure as an argument
        val methodRef = PsiTreeUtil.getParentOfType(enclosingFunction, MethodReference::class.java)

        return methodRef
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

        if (baseTable != null && baseTable.isNotEmpty()) {
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
            if (current is MethodReference) {
                return current
            }

            current = current.parent
            depth++
        }

        return null
    }

    fun isStringLiteralArrayKeyInColumnArray(element: PsiElement): Boolean {
        // Use strict=false to include the element itself if it's already a StringLiteralExpression
        val literal =
            PsiTreeUtil.getParentOfType(element, StringLiteralExpression::class.java, false)
                ?: return false

        val arrayHash =
            PsiTreeUtil.getParentOfType(literal, ArrayHashElement::class.java)
                ?: return false

        val keyExpr = arrayHash.key ?: return false
        return PsiTreeUtil.isAncestor(keyExpr, literal, false)
    }
}

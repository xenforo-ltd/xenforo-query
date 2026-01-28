package com.github.xenforo.query.utils

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.AssignmentExpression
import com.jetbrains.php.lang.psi.elements.ClassReference
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpTypedElement
import com.jetbrains.php.lang.psi.elements.Variable
import com.jetbrains.php.lang.psi.resolve.types.PhpType

object XenForoClassDetector {
    private const val BUILDER_FQN = "\\XF\\Mvc\\Query\\Builder"
    private const val XF_CLASS_FQN = "\\XF"

    /**
     * Checks if the given method reference is being called on XenForo's Query Builder.
     *
     * This walks up the method chain to find the root and verifies:
     * 1. Direct calls like \XF::query()
     * 2. Chained calls like \XF::query('table')->where()
     * 3. Variable assignments like $query = \XF::query(); $query->where()
     * 4. Method chain patterns that look like Query Builder usage
     */
    fun isXenForoQueryBuilder(methodRef: MethodReference): Boolean {
        return isXenForoQueryBuilderRecursive(methodRef, mutableSetOf())
    }

    private fun isXenForoQueryBuilderRecursive(
        element: PsiElement?,
        visited: MutableSet<PsiElement>,
    ): Boolean {
        if (element == null || element in visited) {
            return false
        }
        visited.add(element)

        when (element) {
            is MethodReference ->
                {
                    val methodName = element.name
                    val classRef = element.classReference

                    // Check if this is a static call to \XF::query() or \XF::table()
                    if (classRef != null) {
                        // Check for \XF class reference by name (works without type resolution)
                        if (isXfClassReference(classRef) && (methodName == "query" || methodName == "table")) {
                            return true
                        }

                        val type = (classRef as? PhpTypedElement)?.type

                        if (type != null) {
                            // Check for \XF class with query() method
                            if (methodName == "query" && isXfClass(type)) {
                                return true
                            }

                            // Check if type resolves to Builder
                            if (isBuilderType(type, element.project)) {
                                return true
                            }
                        }
                    }

                    // Check if this method is a known entry point (query/table) being called statically
                    if (methodName == "query" || methodName == "table") {
                        // If we find query() or table() in the chain, it's likely XenForo's builder
                        // This is a heuristic for when type resolution isn't available
                        if (classRef != null && looksLikeXfReference(classRef)) {
                            return true
                        }
                    }

                    // Continue up the chain
                    return isXenForoQueryBuilderRecursive(classRef, visited)
                }

            is Variable ->
                {
                    // First try type-based resolution
                    val type = element.type
                    if (isBuilderType(type, element.project)) {
                        return true
                    }

                    // Check if this is a closure parameter
                    val closureMethod = resolveClosureParameterToMethod(element)
                    if (closureMethod != null) {
                        return isXenForoQueryBuilderRecursive(closureMethod, visited)
                    }

                    // Fall back to AST-based variable assignment resolution
                    val resolved = resolveVariableAssignment(element)
                    if (resolved != null) {
                        return isXenForoQueryBuilderRecursive(resolved, visited)
                    }
                }

            is PhpTypedElement ->
                {
                    return isBuilderType(element.type, element.project)
                }
        }

        return false
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

    /**
     * Check if the class reference is literally \XF or XF
     */
    private fun isXfClassReference(classRef: PsiElement): Boolean {
        if (classRef is ClassReference) {
            val name = classRef.name ?: return false
            return name == "XF" || name == "\\XF"
        }
        // Also check the text representation for cases where it's not a ClassReference
        val text = classRef.text?.trim()
        return text == "XF" || text == "\\XF"
    }

    /**
     * Heuristic check: does this look like it could be \XF?
     */
    private fun looksLikeXfReference(classRef: PsiElement): Boolean {
        val text = classRef.text?.trim() ?: return false
        // Match \XF, XF, or any method chain starting from something that ends with XF
        return text == "XF" || text == "\\XF" || text.endsWith("::query") || text.endsWith("::table")
    }

    private fun isXfClass(type: PhpType): Boolean {
        val resolved = type.filterUnknown().filterMixed().filterNull()
        return resolved.types.any { typeName ->
            typeName.equals(XF_CLASS_FQN, ignoreCase = true) ||
                typeName.equals("XF", ignoreCase = true)
        }
    }

    private fun isBuilderType(
        type: PhpType,
        project: Project,
    ): Boolean {
        val resolved = type.filterUnknown().filterMixed().filterNull()

        // Direct type match
        if (resolved.types.any { it.equals(BUILDER_FQN, ignoreCase = true) }) {
            return true
        }

        // Check via PhpIndex for type hierarchy
        val phpIndex = PhpIndex.getInstance(project)
        return resolved.types.any { typeName ->
            phpIndex.getAnyByFQN(typeName).any { phpClass ->
                phpClass.fqn.equals(BUILDER_FQN, ignoreCase = true) ||
                    phpClass.superFQN?.equals(BUILDER_FQN, ignoreCase = true) == true
            }
        }
    }
}

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

    fun isXenForoQueryBuilder(methodRef: MethodReference): Boolean =
        isXenForoQueryBuilderRecursive(methodRef, mutableSetOf())

    private fun isXenForoQueryBuilderRecursive(element: PsiElement?, visited: MutableSet<PsiElement>): Boolean {
        if (element == null || element in visited) {
            return false
        }
        visited.add(element)

        when (element) {
            is MethodReference -> {
                val methodName = element.name
                val classRef = element.classReference

                if (classRef != null) {
                    if (isXfClassReference(classRef) && (methodName == "query" || methodName == "table")) {
                        return true
                    }

                    val type = (classRef as? PhpTypedElement)?.type
                    if (type != null) {
                        if (methodName == "query" && isXfClass(type)) {
                            return true
                        }
                        if (isBuilderType(type, element.project)) {
                            return true
                        }
                    }
                }

                if (
                    (methodName == "query" || methodName == "table") &&
                        classRef != null &&
                        looksLikeXfReference(classRef)
                ) {
                    return true
                }

                return isXenForoQueryBuilderRecursive(classRef, visited)
            }

            is Variable -> {
                val type = element.type
                if (isBuilderType(type, element.project)) {
                    return true
                }

                resolveClosureParameterToMethod(element)?.let {
                    return isXenForoQueryBuilderRecursive(it, visited)
                }

                resolveVariableAssignment(element)?.let {
                    return isXenForoQueryBuilderRecursive(it, visited)
                }
            }

            is PhpTypedElement -> return isBuilderType(element.type, element.project)
        }

        return false
    }

    /**
     * Resolves the latest assignment to this variable that appears before its usage. Example: $query =
     * \XF::query('xf_user') -> returns the MethodReference
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
                    (assignment.value as? MethodReference)?.let {
                        latestAssignment = it
                        latestOffset = assignment.textOffset
                    }
                }
            }
        }

        return latestAssignment
    }

    /**
     * Resolves closure parameters to their enclosing method call. Allows tracing `$query` inside `->where(function
     * (Builder $query) { ... })` back to the table.
     */
    private fun resolveClosureParameterToMethod(variable: Variable): MethodReference? {
        val variableName = variable.name

        val enclosingFunction = PsiTreeUtil.getParentOfType(variable, Function::class.java) ?: return null

        val isParameter = enclosingFunction.parameters.any { it.name == variableName }
        if (!isParameter) return null

        return PsiTreeUtil.getParentOfType(enclosingFunction, MethodReference::class.java)
    }

    private fun isXfClassReference(classRef: PsiElement): Boolean {
        if (classRef is ClassReference) {
            val name = classRef.name ?: return false
            return name == "XF" || name == "\\XF"
        }
        val text = classRef.text?.trim()
        return text == "XF" || text == "\\XF"
    }

    private fun looksLikeXfReference(classRef: PsiElement): Boolean {
        val text = classRef.text?.trim() ?: return false
        return text == "XF" || text == "\\XF" || text.endsWith("::query") || text.endsWith("::table")
    }

    private fun isXfClass(type: PhpType): Boolean {
        val resolved = type.filterUnknown().filterMixed().filterNull()
        return resolved.types.any { typeName ->
            typeName.equals(XF_CLASS_FQN, ignoreCase = true) || typeName.equals("XF", ignoreCase = true)
        }
    }

    private fun isBuilderType(type: PhpType, project: Project): Boolean {
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

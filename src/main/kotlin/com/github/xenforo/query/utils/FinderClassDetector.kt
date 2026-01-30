package com.github.xenforo.query.utils

import com.github.xenforo.query.constants.FinderMethods
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.AssignmentExpression
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpTypedElement
import com.jetbrains.php.lang.psi.elements.Variable
import com.jetbrains.php.lang.psi.resolve.types.PhpType

/**
 * Detects if a method call is being made on a XenForo Entity Finder.
 *
 * Uses string pattern matching on type names rather than PhpIndex lookups to avoid PSI invalidation issues during
 * background processing.
 */
object FinderClassDetector {
    fun isXenForoFinder(methodRef: MethodReference): Boolean {
        if (!methodRef.isValid) return false
        return isXenForoFinderRecursive(methodRef, mutableSetOf(), 0)
    }

    private fun isXenForoFinderRecursive(element: PsiElement?, visited: MutableSet<PsiElement>, depth: Int): Boolean {
        if (element == null || !element.isValid || element in visited || depth > 20) {
            return false
        }
        visited.add(element)

        when (element) {
            is MethodReference -> {
                val methodName = element.name
                val classRef = element.classReference

                if (methodName == "finder" || methodName == "getFinder") {
                    return true
                }

                if (isFinderType(element.type)) {
                    return true
                }

                if (classRef is PhpTypedElement && classRef.isValid && isFinderType(classRef.type)) {
                    return true
                }

                if (classRef != null && classRef.isValid) {
                    return isXenForoFinderRecursive(classRef, visited, depth + 1)
                }
            }

            is Variable -> {
                if (!element.isValid) return false

                if (isFinderType(element.type)) {
                    return true
                }

                val resolved = resolveVariableAssignment(element)
                if (resolved != null) {
                    return isXenForoFinderRecursive(resolved, visited, depth + 1)
                }
            }

            is PhpTypedElement -> {
                if (!element.isValid) return false
                return isFinderType(element.type)
            }
        }

        return false
    }

    /**
     * Checks if a PhpType represents a Finder class.
     *
     * Matches:
     * - \XF\Mvc\Entity\Finder (base class)
     * - Classes in \Finder\ namespace (e.g., \XF\Finder\ThreadFinder)
     * - Classes ending with "Finder" in \XF\ namespace
     */
    private fun isFinderType(type: PhpType): Boolean {
        val resolved = type.filterUnknown().filterMixed().filterNull()

        return resolved.types.any { typeName ->
            val normalizedType = if (typeName.startsWith("\\")) typeName else "\\$typeName"

            normalizedType.equals(FinderMethods.FINDER_FQN, ignoreCase = true) ||
                normalizedType.contains("\\Finder\\", ignoreCase = true) ||
                (normalizedType.startsWith("\\XF\\", ignoreCase = true) &&
                    normalizedType.endsWith("Finder", ignoreCase = true))
        }
    }

    private fun resolveVariableAssignment(variable: Variable): PsiElement? {
        if (!variable.isValid) return null

        val variableName = variable.name
        val containingFile = variable.containingFile ?: return null
        if (!containingFile.isValid) return null

        val assignments = PsiTreeUtil.findChildrenOfType(containingFile, AssignmentExpression::class.java)

        var latestAssignment: PsiElement? = null
        var latestOffset = -1

        for (assignment in assignments) {
            if (!assignment.isValid) continue

            val assignedVar = assignment.variable
            if (assignedVar is Variable && assignedVar.isValid && assignedVar.name == variableName) {
                if (assignment.textOffset < variable.textOffset && assignment.textOffset > latestOffset) {
                    val value = assignment.value
                    if (value is MethodReference && value.isValid) {
                        latestAssignment = value
                        latestOffset = assignment.textOffset
                    }
                }
            }
        }

        return latestAssignment
    }
}

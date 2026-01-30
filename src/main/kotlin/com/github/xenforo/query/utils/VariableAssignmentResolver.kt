package com.github.xenforo.query.utils

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.AssignmentExpression
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.Variable

object VariableAssignmentResolver {
    /**
     * Resolves the most recent assignment to the given variable that appears before its usage. You can pass an optional
     * predicate to filter acceptable assignment values.
     */
    fun resolveLatestAssignment(variable: Variable, acceptValue: (PsiElement) -> Boolean = { true }): PsiElement? {
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
                val assignmentOffset = assignment.textOffset
                if (assignmentOffset < variable.textOffset && assignmentOffset > latestOffset) {
                    val value = assignment.value
                    if (value != null && value.isValid && acceptValue(value)) {
                        latestAssignment = value
                        latestOffset = assignmentOffset
                    }
                }
            }
        }

        return latestAssignment
    }

    fun resolveLatestMethodReference(variable: Variable): MethodReference? {
        return resolveLatestAssignment(variable) { it is MethodReference } as? MethodReference
    }
}

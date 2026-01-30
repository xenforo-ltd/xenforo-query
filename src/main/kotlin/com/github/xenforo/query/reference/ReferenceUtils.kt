package com.github.xenforo.query.reference

import com.github.xenforo.query.settings.XenForoQuerySettings
import com.github.xenforo.query.utils.QueryChainResolver
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

object ReferenceUtils {
    /**
     * Returns the nearest StringLiteralExpression for the given element, checking the element itself and its direct
     * parent.
     */
    fun getStringLiteral(element: PsiElement): StringLiteralExpression? {
        return when (element) {
            is StringLiteralExpression -> element
            else -> element.parent as? StringLiteralExpression
        }
    }

    data class ReferenceContext(
        val literal: StringLiteralExpression,
        val project: Project,
        val settings: XenForoQuerySettings,
        val method: MethodReference,
        val methodName: String,
    )

    fun getReferenceContext(element: PsiElement): ReferenceContext? {
        val literal = getStringLiteral(element) ?: return null
        val project = literal.project
        val settings = XenForoQuerySettings.getInstance(project)
        val method = QueryChainResolver.findMethodReference(literal) ?: return null
        val methodName = method.name ?: return null

        return ReferenceContext(
            literal = literal,
            project = project,
            settings = settings,
            method = method,
            methodName = methodName,
        )
    }
}

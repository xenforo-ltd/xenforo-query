package com.github.xenforo.query.reference

import com.intellij.database.model.DasColumn
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.sql.symbols.DasPsiWrappingSymbol

class ColumnPsiReference(
    element: PsiElement,
    private val targetColumn: DasColumn,
    private val project: Project,
) : PsiReferenceBase<PsiElement>(element, true) {
    override fun resolve(): PsiElement? {
        val symbol = DasPsiWrappingSymbol(targetColumn, project)
        val navElement = symbol.navigationElement
        if (navElement !== symbol) {
            return navElement
        }
        return null
    }
}

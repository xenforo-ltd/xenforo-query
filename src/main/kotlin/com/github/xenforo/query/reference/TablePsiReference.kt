package com.github.xenforo.query.reference

import com.intellij.database.model.DasTable
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.sql.symbols.DasPsiWrappingSymbol

class TablePsiReference(
    element: PsiElement,
    private val targetTable: DasTable,
    private val project: Project,
) : PsiReferenceBase<PsiElement>(element, true) {
    override fun resolve(): PsiElement? {
        val symbol = DasPsiWrappingSymbol(targetTable, project)
        val navElement = symbol.navigationElement
        if (navElement !== symbol) {
            return navElement
        }
        return null
    }
}

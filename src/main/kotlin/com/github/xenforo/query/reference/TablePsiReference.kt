package com.github.xenforo.query.reference

import com.intellij.database.model.DasTable
import com.intellij.database.util.DbUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

class TablePsiReference(
    element: PsiElement,
    private val targetTable: DasTable,
    private val project: Project,
) : PsiReferenceBase<PsiElement>(element, true) {
    override fun resolve(): PsiElement? {
        // Find the DbElement via the containing data source (non-deprecated API)
        return DbUtil.getDataSources(project)
            .asSequence()
            .mapNotNull { dataSource -> dataSource.findElement(targetTable) }
            .firstOrNull()
    }
}

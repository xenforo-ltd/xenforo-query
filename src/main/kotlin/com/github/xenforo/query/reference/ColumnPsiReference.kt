package com.github.xenforo.query.reference

import com.intellij.database.model.DasColumn
import com.intellij.database.util.DbUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

class ColumnPsiReference(element: PsiElement, private val targetColumn: DasColumn, private val project: Project) :
    PsiReferenceBase<PsiElement>(element, true) {
    override fun resolve(): PsiElement? {
        // Find the DbElement via the containing data source (non-deprecated API)
        return DbUtil.getDataSources(project).firstNotNullOfOrNull { dataSource ->
            dataSource.findElement(targetColumn)
        }
    }
}

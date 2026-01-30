package com.github.xenforo.query.reference

import com.github.xenforo.query.constants.BuilderMethods
import com.github.xenforo.query.utils.LookupBuilder
import com.github.xenforo.query.utils.XenForoClassDetector
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext

class TableReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val referenceContext = ReferenceUtils.getReferenceContext(element) ?: return PsiReference.EMPTY_ARRAY

        if (!referenceContext.settings.isTableReferencesEnabled) return PsiReference.EMPTY_ARRAY

        val lit = referenceContext.literal
        val method = referenceContext.method
        val methodName = referenceContext.methodName
        val project = referenceContext.project
        if (!BuilderMethods.TableMethods.contains(methodName)) return PsiReference.EMPTY_ARRAY
        if (!XenForoClassDetector.isXenForoQueryBuilder(method)) return PsiReference.EMPTY_ARRAY

        val literalContent = lit.contents.trim()
        val parts = literalContent.split(Regex("(?i)\\s+as\\s+|\\s+"))
        val baseTable = parts.getOrNull(0)?.trim() ?: return PsiReference.EMPTY_ARRAY

        val table = LookupBuilder.findTable(project, baseTable) ?: return PsiReference.EMPTY_ARRAY

        return arrayOf(TablePsiReference(lit, table, project))
    }
}

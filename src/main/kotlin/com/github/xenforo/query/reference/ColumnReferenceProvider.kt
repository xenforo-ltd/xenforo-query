package com.github.xenforo.query.reference

import com.github.xenforo.query.constants.BuilderMethods
import com.github.xenforo.query.constants.FinderMethods
import com.github.xenforo.query.utils.FinderClassDetector
import com.github.xenforo.query.utils.FinderEntityResolver
import com.github.xenforo.query.utils.LookupBuilder
import com.github.xenforo.query.utils.QueryChainResolver
import com.github.xenforo.query.utils.XenForoClassDetector
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class ColumnReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val referenceContext = ReferenceUtils.getReferenceContext(element) ?: return PsiReference.EMPTY_ARRAY

        if (!referenceContext.settings.isColumnReferencesEnabled) return PsiReference.EMPTY_ARRAY

        val lit = referenceContext.literal
        val method = referenceContext.method
        val methodName = referenceContext.methodName

        if (XenForoClassDetector.isXenForoQueryBuilder(method)) {
            return getQueryBuilderReferences(lit, method, methodName)
        }

        if (FinderClassDetector.isXenForoFinder(method)) {
            return getFinderReferences(lit, method, methodName)
        }

        return PsiReference.EMPTY_ARRAY
    }

    private fun getQueryBuilderReferences(
        lit: StringLiteralExpression,
        method: com.jetbrains.php.lang.psi.elements.MethodReference,
        methodName: String,
    ): Array<PsiReference> {
        val isValidMethod =
            BuilderMethods.ColumnMethods.contains(methodName) ||
                BuilderMethods.TableMethods.filter { it.endsWith("join", true) }.contains(methodName) ||
                BuilderMethods.ColumnArrayMethods.contains(methodName)

        if (!isValidMethod) return PsiReference.EMPTY_ARRAY

        // For upsert/update/insert methods, check if the literal is in a valid column position
        if (BuilderMethods.ColumnArrayMethods.contains(methodName)) {
            val position = QueryChainResolver.getColumnArrayPosition(lit)
            // Only create references for KEY (1st arg) or VALUE (2nd/3rd arg of upsert) positions
            if (position == QueryChainResolver.ColumnArrayPosition.NONE) {
                return PsiReference.EMPTY_ARRAY
            }
        }

        val literalContent = lit.contents.trim()
        val (givenAlias, givenColumnName) =
            if (literalContent.contains(".")) {
                val parts = literalContent.split(".", limit = 2)
                parts[0].trim() to parts[1].trim()
            } else {
                null to literalContent
            }

        val project = lit.project
        val tables = QueryChainResolver.resolveTables(method)
        if (tables.isEmpty()) return PsiReference.EMPTY_ARRAY

        val refs = mutableListOf<PsiReference>()

        tables.forEach { ctx ->
            if (givenAlias != null && (ctx.alias == null || !ctx.alias.equals(givenAlias, ignoreCase = true))) {
                return@forEach
            }
            val tbl = LookupBuilder.findTable(project, ctx.baseTable) ?: return@forEach
            val cols = LookupBuilder.getColumnsForTable(project, tbl.name)
            val col = cols.firstOrNull { it.name.equals(givenColumnName, ignoreCase = true) } ?: return@forEach

            refs += ColumnPsiReference(lit, col, project)
        }

        return refs.toTypedArray()
    }

    private fun getFinderReferences(
        lit: StringLiteralExpression,
        method: com.jetbrains.php.lang.psi.elements.MethodReference,
        methodName: String,
    ): Array<PsiReference> {
        if (!FinderMethods.ColumnMethods.contains(methodName)) return PsiReference.EMPTY_ARRAY

        val project = lit.project
        val columnName = lit.contents.trim()

        val tableInfo = FinderEntityResolver.resolveTable(method) ?: return PsiReference.EMPTY_ARRAY
        val table = LookupBuilder.findTable(project, tableInfo.tableName) ?: return PsiReference.EMPTY_ARRAY
        val columns = LookupBuilder.getColumnsForTable(project, table.name)
        val column =
            columns.firstOrNull { it.name.equals(columnName, ignoreCase = true) } ?: return PsiReference.EMPTY_ARRAY

        return arrayOf(ColumnPsiReference(lit, column, project))
    }
}

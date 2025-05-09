package com.github.xenforo.query.reference

import com.github.xenforo.query.constants.BuilderMethods
import com.github.xenforo.query.utils.LookupBuilder
import com.github.xenforo.query.utils.QueryChainResolver
import com.intellij.database.util.DbUtil
import com.intellij.database.util.DasUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class TableReferenceProvider : PsiReferenceProvider()
{
	override fun getReferencesByElement(
		element: PsiElement,
		context: ProcessingContext,
	): Array<PsiReference>
	{
		val lit = element as? StringLiteralExpression
			?: (element.parent as? StringLiteralExpression ?: return PsiReference.EMPTY_ARRAY)

		val method = QueryChainResolver.findMethodReference(lit) ?: return PsiReference.EMPTY_ARRAY
		val methodName = method.name ?: return PsiReference.EMPTY_ARRAY

		if (!BuilderMethods.TableMethods.contains(methodName))
		{
			return PsiReference.EMPTY_ARRAY
		}

		val literalContent = lit.contents.trim()
		val parts = literalContent.split(Regex("(?i)\\s+as\\s+|\\s+"))
		val baseTable = parts.getOrNull(0)?.trim() ?: return PsiReference.EMPTY_ARRAY

		val project = lit.project
		val allTables = LookupBuilder.getCachedTables(project) {
			DbUtil.getDataSources(project)
				.asSequence()
				.flatMap { ds -> DasUtil.getTables(ds).asSequence() }
				.toList()
		}
		val table = allTables.firstOrNull { it.name.equals(baseTable, ignoreCase = true) }
			?: return PsiReference.EMPTY_ARRAY

		return arrayOf(TablePsiReference(lit, table, project))
	}
}

package com.github.xenforo.query.reference

import com.github.xenforo.query.constants.BuilderMethods
import com.github.xenforo.query.utils.LookupBuilder
import com.github.xenforo.query.utils.QueryChainResolver
import com.intellij.database.util.DasUtil
import com.intellij.database.util.DbUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.psi.elements.ArrayHashElement
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.intellij.psi.util.PsiTreeUtil

class ColumnReferenceProvider : PsiReferenceProvider()
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

		val isValidMethod =
			BuilderMethods.ColumnMethods.contains(methodName) ||
					BuilderMethods.TableMethods.filter { it.endsWith("join", true) }.contains(methodName) ||
					BuilderMethods.ColumnArrayMethods.contains(methodName)

		if (!isValidMethod)
		{
			return PsiReference.EMPTY_ARRAY
		}

		if (BuilderMethods.ColumnArrayMethods.contains(methodName) && !isArrayKey(lit))
		{
			return PsiReference.EMPTY_ARRAY
		}

		val literalContent = lit.contents.trim()
		val (givenAlias, givenColumnName) = if (literalContent.contains("."))
		{
			val parts = literalContent.split(".", limit = 2)
			parts[0].trim() to parts[1].trim()
		}
		else
		{
			null to literalContent
		}

		val tables = QueryChainResolver.resolveTables(method)
		if (tables.isEmpty())
		{
			return PsiReference.EMPTY_ARRAY
		}
		val project = lit.project
		val refs = mutableListOf<PsiReference>()

		tables.forEach { ctx ->
			if (givenAlias != null)
			{
				if (ctx.alias == null || !ctx.alias.equals(givenAlias, ignoreCase = true))
				{
					return@forEach
				}
			}
			val allTables = LookupBuilder.getCachedTables(project) {
				DbUtil.getDataSources(project)
					.asSequence()
					.flatMap { ds -> DasUtil.getTables(ds).asSequence() }
					.toList()
			}
			val tbl = allTables.firstOrNull { it.name.equals(ctx.baseTable, ignoreCase = true) }
				?: return@forEach

			val cols = LookupBuilder.getCachedColumns(project, tbl.name) {
				DasUtil.getColumns(tbl).toList()
			}
			val col = cols.firstOrNull { it.name.equals(givenColumnName, ignoreCase = true) }
				?: return@forEach

			refs += ColumnPsiReference(lit, col, project)
		}

		return refs.toTypedArray()
	}

	private fun isArrayKey(literal: StringLiteralExpression): Boolean
	{
		val arrayHash = PsiTreeUtil.getParentOfType(literal, ArrayHashElement::class.java)
		return if (arrayHash != null)
		{
			PsiTreeUtil.isAncestor(arrayHash.key, literal, false)
		}
		else
		{
			false
		}
	}
}

package com.github.xenforo.query.completion

import com.github.xenforo.query.db.DatabaseInfoProvider
import com.github.xenforo.query.lookup.QueryLookupElementFactory
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.ParameterList
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class QueryCompletionProvider : CompletionProvider<CompletionParameters>()
{
	private enum class CompletionContext
	{
		TABLE_NAME,
		UNKNOWN,
	}

	override fun addCompletions(
		parameters: CompletionParameters,
		context: ProcessingContext,
		result: CompletionResultSet
	)
	{
		val completionContext = detectCompletionContext(parameters.position)

		when (completionContext)
		{
			CompletionContext.TABLE_NAME -> addTableCompletions(parameters, result)
			CompletionContext.UNKNOWN ->
			{
			}
		}
	}

	private fun detectCompletionContext(position: PsiElement): CompletionContext
	{
		val stringLiteral = position.parent as? StringLiteralExpression ?: return CompletionContext.UNKNOWN
		val paramList = stringLiteral.parent as? ParameterList ?: return CompletionContext.UNKNOWN
		val methodRef =
			PsiTreeUtil.getParentOfType(paramList, MethodReference::class.java) ?: return CompletionContext.UNKNOWN

		val isXfQuery = methodRef.name == "query" &&
				(methodRef.classReference?.text?.endsWith("XF") == true ||
						methodRef.text.contains("XF::query") ||
						methodRef.text.contains("\\XF::query"))

		if (!isXfQuery)
		{
			return CompletionContext.UNKNOWN
		}

		val parameters = methodRef.parameters
		val index = parameters.indexOf(stringLiteral)
		if (index == 0)
		{
			return CompletionContext.TABLE_NAME
		}

		return CompletionContext.UNKNOWN
	}

	private fun addTableCompletions(parameters: CompletionParameters, result: CompletionResultSet)
	{
		val project = parameters.position.project
		val dbInfoProvider = DatabaseInfoProvider(project)
		val lookupElementFactory = QueryLookupElementFactory(project)

		val tables = dbInfoProvider.getDatabaseTables()

		for (table in tables)
		{
			val lookupElement = lookupElementFactory.buildTableLookup(table)
			result.addElement(lookupElement)
		}
	}
}
package com.github.xenforo.query.completion

import com.github.xenforo.query.constants.BuilderMethods
import com.github.xenforo.query.utils.LookupBuilder
import com.github.xenforo.query.utils.QueryChainResolver
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.database.util.DasUtil
import com.intellij.database.util.DbUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.ProcessingContext

class ColumnCompletionProvider : CompletionProvider<CompletionParameters>()
{
	override fun addCompletions(
		parameters: CompletionParameters,
		context: ProcessingContext,
		result: CompletionResultSet,
	)
	{
		val position = parameters.position
		val method = QueryChainResolver.findMethodReference(position) ?: return
		val methodName = method.name ?: return

		if (!isColumnAcceptingMethod(methodName))
		{
			return
		}

		val tableContexts = QueryChainResolver.resolveTables(method)
		if (tableContexts.isEmpty()) return

		val project = position.project

		ApplicationManager.getApplication().runReadAction {
			val tables = LookupBuilder.getCachedTables(project) {
				DbUtil.getDataSources(project)
					.asSequence()
					.flatMap { DasUtil.getTables(it) }
					.toList()
			}

			for (tableContext in tableContexts)
			{
				val table = tables.firstOrNull { it.name.equals(tableContext.baseTable, ignoreCase = true) } ?: continue
				val columns = LookupBuilder.getCachedColumns(project, table.name) {
					DasUtil.getColumns(table).toList()
				}
				columns.forEach { column ->
					result.addElement(LookupBuilder.forColumn(column, project, tableContext.alias))
				}
			}
		}
	}

	private fun isColumnAcceptingMethod(methodName: String): Boolean
	{
		return BuilderMethods.ColumnMethods.contains(methodName)
	}
}

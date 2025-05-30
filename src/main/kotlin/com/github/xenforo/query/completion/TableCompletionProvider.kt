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
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.util.ProcessingContext

class TableCompletionProvider : CompletionProvider<CompletionParameters>()
{
	companion object
	{
		private val systemSchemas = setOf("information_schema", "mysql", "performance_schema", "sys")
	}

	override fun addCompletions(
		parameters: CompletionParameters,
		context: ProcessingContext,
		result: CompletionResultSet,
	)
	{
		val method = QueryChainResolver.findMethodReference(parameters.position) ?: return
		val methodName = method.name ?: return

		if (!isTableAcceptingMethod(methodName))
		{
			return
		}

		val project = parameters.position.project

		ApplicationManager.getApplication().runReadAction {
			populateCompletions(project, result)
		}
	}

	private fun isTableAcceptingMethod(methodName: String): Boolean
	{
		return BuilderMethods.TableMethods.contains(methodName)
	}

	private fun populateCompletions(project: Project, result: CompletionResultSet)
	{
		ProgressManager.checkCanceled()

		val tables = LookupBuilder.getCachedTables(project) {
			DbUtil.getDataSources(project)
				.asSequence()
				.flatMap { dataSource ->
					DasUtil.getSchemas(dataSource)
						.filter { schema -> !systemSchemas.contains(schema.name.lowercase()) }
						.flatMap { schema ->
							DasUtil.getTables(dataSource)
								.filter { table -> !table.isSystem }
						}
				}
				.distinctBy { it.name }
				.sortedBy { it.name }
				.toList()
		}

		tables.forEach { table ->
			ProgressManager.checkCanceled()
			result.addElement(LookupBuilder.forTable(table, project))
		}
	}
}

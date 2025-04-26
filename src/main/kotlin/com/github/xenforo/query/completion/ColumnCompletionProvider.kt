package com.github.xenforo.query.completion

import com.github.xenforo.query.utils.LookupBuilder
import com.github.xenforo.query.utils.MethodUtils.resolveMethodReference
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.database.util.DasUtil
import com.intellij.database.util.DbUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.psi.elements.MethodReference

class ColumnCompletionProvider : CompletionProvider<CompletionParameters>()
{
	data class TableContext(val baseTable: String, val alias: String?)

	override fun addCompletions(
		parameters: CompletionParameters,
		context: ProcessingContext,
		result: CompletionResultSet,
	)
	{
		val position = parameters.position
		val method = resolveMethodReference(position) ?: return
		val methodName = method.name ?: return

		if (!isColumnAcceptingMethod(methodName))
		{
			return
		}

		val tableContext = resolveTableFromChain(method) ?: return
		val tableName = tableContext.baseTable
		val alias = tableContext.alias

		val project = parameters.position.project

		val tables = LookupBuilder.getCachedTables(project)
		{
			DbUtil.getDataSources(project)
				.asSequence()
				.flatMap { dataSource ->
					DasUtil.getTables(dataSource)
				}
				.toList()
		}

		val table = tables.firstOrNull { it.name.equals(tableName, ignoreCase = true) } ?: return

		val columns = LookupBuilder.getCachedColumns(project, table) {
			DasUtil.getColumns(table).toList()
		}

		for (column in columns)
		{
			result.addElement(LookupBuilder.forColumn(column, project, alias))
		}
	}

	private fun isColumnAcceptingMethod(methodName: String): Boolean
	{
		return methodName in listOf(
			"select", "where", "whereIn", "orWhere", "orderBy", "groupBy", "having"
		)
	}

	private fun resolveTableFromChain(method: MethodReference): TableContext?
	{
		var current: MethodReference? = method

		var depth = 0
		while (current != null && depth < 10)
		{
			val name = current.name ?: break

			if (name == "query" || name == "from")
			{
				val args = current.parameterList?.parameters
				if (!args.isNullOrEmpty())
				{
					val tableLiteral = args[0]
					val text = tableLiteral.text.trim('\"', '\'').trim()
					val parts = text.split(Regex("(?i)\\s+as\\s+|\\s+"))
					val baseTable = parts.getOrNull(0)?.trim()
					val alias = parts.getOrNull(1)?.trim()

					return TableContext(baseTable.toString(), alias)
				}
			}

			current = current.firstChild as? MethodReference
			depth++
		}

		return null
	}
}

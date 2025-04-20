package com.github.xenforo.query.utils

import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.database.model.DasColumn
import com.intellij.database.model.DasTable
import com.intellij.openapi.project.Project
import com.intellij.sql.symbols.DasPsiWrappingSymbol

object LookupBuilder
{
	private const val TABLE_PRIORITY = 100.0
	private const val COLUMN_PRIORITY = 90.0

	fun forTable(table: DasTable, project: Project): LookupElement
	{
		val builder = LookupElementBuilder.create(table.name)
			.withIcon(DasPsiWrappingSymbol(table, project).getIcon(false))
			.withTailText("  (${table.dasParent?.name ?: ""})", true)
			.withInsertHandler(
				DeclarativeInsertHandler.Builder()
					.insertOrMove(table.name)
					.triggerAutoPopup()
					.build()
			)

		return PrioritizedLookupElement.withPriority(builder, TABLE_PRIORITY)
	}

	fun forColumn(column: DasColumn, project: Project, alias: String? = null): LookupElement
	{
		val lookupString = if (alias != null) "$alias.${column.name}" else column.name

		val builder = LookupElementBuilder.create(lookupString)
			.withPresentableText(column.name)
			.withTailText(if (alias != null) " ($alias)" else "", true)
			.withIcon(DasPsiWrappingSymbol(column, project).getIcon(false))
			.withTypeText(column.dasType.toDataType().toString(), true)
			.withInsertHandler(
				DeclarativeInsertHandler.Builder()
					.insertOrMove(lookupString)
					.build()
			)

		return PrioritizedLookupElement.withPriority(builder, COLUMN_PRIORITY)
	}
}

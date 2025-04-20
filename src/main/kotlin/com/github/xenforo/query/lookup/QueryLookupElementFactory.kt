package com.github.xenforo.query.lookup

import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.database.model.DasTable
import com.intellij.database.model.DasNamespace
import com.intellij.sql.symbols.DasPsiWrappingSymbol
import com.intellij.openapi.project.Project

class QueryLookupElementFactory(private val project: Project)
{
	fun buildTableLookup(table: DasTable): LookupElement
	{
		val tableName = table.name
		val schemaName = (table.dasParent as? DasNamespace)?.name

		var lookupElementBuilder =
			LookupElementBuilder.create(table, tableName) // Associate the DasTable object
				.withIcon(
					DasPsiWrappingSymbol(table, project).getIcon(false)
				)
				.withTypeText(schemaName ?: "", true) // Add schema/database name as type text

		if (schemaName != null)
		{
			lookupElementBuilder = lookupElementBuilder.withLookupString("$schemaName.$tableName")
		}

		lookupElementBuilder =
			lookupElementBuilder.withInsertHandler { context: InsertionContext, item: LookupElement ->
				val editor = context.editor
				val document = context.document
				val tailOffset = context.tailOffset
				val lookupString = item.lookupString

				document.replaceString(context.startOffset, tailOffset, lookupString)

				editor.caretModel.moveToOffset(context.startOffset + lookupString.length)
			}

		return lookupElementBuilder
	}
}

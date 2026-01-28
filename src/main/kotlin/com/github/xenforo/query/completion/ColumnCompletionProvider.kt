package com.github.xenforo.query.completion

import com.github.xenforo.query.constants.BuilderMethods
import com.github.xenforo.query.settings.XenForoQuerySettings
import com.github.xenforo.query.utils.LookupBuilder
import com.github.xenforo.query.utils.QueryChainResolver
import com.github.xenforo.query.utils.XenForoClassDetector
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.ProcessingContext

class ColumnCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val position = parameters.position
        val project = position.project
        val settings = XenForoQuerySettings.getInstance(project)

        // Check if feature is enabled
        if (!settings.isColumnCompletionEnabled) {
            return
        }

        val method = QueryChainResolver.findMethodReference(position) ?: return
        val methodName = method.name ?: return

        // Only trigger for XenForo's Query Builder
        if (!XenForoClassDetector.isXenForoQueryBuilder(method)) {
            return
        }

        if (BuilderMethods.ColumnArrayMethods.contains(methodName) &&
            !QueryChainResolver.isStringLiteralArrayKeyInColumnArray(position)
        ) {
            return
        }

        if (
            !isColumnAcceptingMethod(methodName) &&
            !isMethodAcceptingColumnArguments(methodName) &&
            !isColumnArrayMethod(methodName)
        ) {
            return
        }

        val tableContexts = QueryChainResolver.resolveTables(method)

        if (tableContexts.isEmpty()) {
            return
        }

        ApplicationManager.getApplication().runReadAction {
            ProgressManager.checkCanceled()

            for (tableContext in tableContexts) {
                ProgressManager.checkCanceled()

                val table = LookupBuilder.findTable(project, tableContext.baseTable) ?: continue
                val columns = LookupBuilder.getColumnsForTable(project, table.name)
				
                columns.forEach { column ->
                    ProgressManager.checkCanceled()
                    val lookupElement = LookupBuilder.forColumn(column, project, tableContext.alias)
                    result.addElement(lookupElement)
                }
            }
        }
    }

    private fun isColumnAcceptingMethod(methodName: String): Boolean {
        return BuilderMethods.ColumnMethods.contains(methodName)
    }

    private fun isMethodAcceptingColumnArguments(methodName: String): Boolean {
        return BuilderMethods.TableMethods.filter { it.endsWith("join", true) }.contains(methodName)
    }

    private fun isColumnArrayMethod(methodName: String): Boolean {
        return BuilderMethods.ColumnArrayMethods.contains(methodName)
    }
}

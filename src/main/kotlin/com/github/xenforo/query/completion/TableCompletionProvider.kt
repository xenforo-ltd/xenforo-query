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
import com.intellij.openapi.project.Project
import com.intellij.util.ProcessingContext

class TableCompletionProvider : CompletionProvider<CompletionParameters>() {
    companion object {
        private val systemSchemas = setOf("information_schema", "mysql", "performance_schema", "sys")
    }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val project = parameters.position.project
        val settings = XenForoQuerySettings.getInstance(project)

        // Check if feature is enabled
        if (!settings.isTableCompletionEnabled) {
            return
        }

        val method = QueryChainResolver.findMethodReference(parameters.position) ?: return
        val methodName = method.name ?: return

        if (!isTableAcceptingMethod(methodName)) {
            return
        }

        // Only trigger for XenForo's Query Builder
        if (!XenForoClassDetector.isXenForoQueryBuilder(method)) {
            return
        }

        ApplicationManager.getApplication().runReadAction {
            populateCompletions(project, settings, result)
        }
    }

    private fun isTableAcceptingMethod(methodName: String): Boolean {
        return BuilderMethods.TableMethods.contains(methodName)
    }

    private fun populateCompletions(
        project: Project,
        settings: XenForoQuerySettings,
        result: CompletionResultSet,
    ) {
        ProgressManager.checkCanceled()

        val excludedSchemas =
            if (settings.shouldExcludeSystemSchemas) {
                systemSchemas + settings.excludedSchemas.map { it.lowercase() }
            } else {
                settings.excludedSchemas.map { it.lowercase() }.toSet()
            }

        val tables =
            LookupBuilder.getAllTables(project)
                .filter { table ->
                    val schemaName = table.dasParent?.name?.lowercase() ?: ""
                    !table.isSystem &&
                        !excludedSchemas.contains(schemaName) &&
                        // Apply table prefix filter if enabled
                        (
                            !settings.requiresTablePrefix ||
                                table.name.startsWith(settings.tablePrefix, ignoreCase = true)
                        )
                }
                .let { tableList ->
                    // Apply data source filter if enabled
                    if (settings.shouldFilterDataSources && settings.allowedDataSources.isNotEmpty()) {
                        tableList.filter { table ->
                            val dsName = table.dasParent?.dasParent?.name ?: ""
                            settings.allowedDataSources.contains(dsName)
                        }
                    } else {
                        tableList
                    }
                }

        tables.forEach { table ->
            ProgressManager.checkCanceled()
            result.addElement(LookupBuilder.forTable(table, project))
        }
    }
}

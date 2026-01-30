package com.github.xenforo.query.completion

import com.github.xenforo.query.constants.BuilderMethods
import com.github.xenforo.query.constants.FinderMethods
import com.github.xenforo.query.settings.XenForoQuerySettings
import com.github.xenforo.query.utils.FinderClassDetector
import com.github.xenforo.query.utils.FinderEntityResolver
import com.github.xenforo.query.utils.LookupBuilder
import com.github.xenforo.query.utils.QueryChainResolver
import com.github.xenforo.query.utils.XenForoClassDetector
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.util.ProcessingContext

class ColumnCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val position = parameters.position
        if (!position.isValid) return

        val project = position.project
        val settings = XenForoQuerySettings.getInstance(project)

        if (!settings.isColumnCompletionEnabled) return

        val method = QueryChainResolver.findMethodReference(position) ?: return
        if (!method.isValid) return

        val methodName = method.name ?: return

        if (XenForoClassDetector.isXenForoQueryBuilder(method)) {
            addQueryBuilderCompletions(method, methodName, position, result)
            return
        }

        try {
            if (FinderClassDetector.isXenForoFinder(method)) {
                addFinderCompletions(method, methodName, result)
                return
            }
        } catch (e: PsiInvalidElementAccessException) {
            // PSI became invalid during processing (e.g., file being edited) - safe to ignore
        }
    }

    private fun addQueryBuilderCompletions(
        method: com.jetbrains.php.lang.psi.elements.MethodReference,
        methodName: String,
        position: com.intellij.psi.PsiElement,
        result: CompletionResultSet,
    ) {
        if (BuilderMethods.ColumnArrayMethods.contains(methodName)) {
            // Check if position is in a column-relevant array position:
            // - First arg: keys are columns
            // - Second/third arg of upsert: values are columns
            val isKeyPosition = QueryChainResolver.isStringLiteralArrayKeyInColumnArray(position)
            val isValuePosition = QueryChainResolver.isStringLiteralArrayValueInColumnArray(position)
            if (!isKeyPosition && !isValuePosition) {
                return
            }
        }

        if (
            !isBuilderColumnAcceptingMethod(methodName) &&
                !isBuilderMethodAcceptingColumnArguments(methodName) &&
                !isBuilderColumnArrayMethod(methodName)
        ) {
            return
        }

        val tableContexts = QueryChainResolver.resolveTables(method)
        if (tableContexts.isEmpty()) return

        val project = method.project

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

    private fun addFinderCompletions(
        method: com.jetbrains.php.lang.psi.elements.MethodReference,
        methodName: String,
        result: CompletionResultSet,
    ) {
        if (!FinderMethods.ColumnMethods.contains(methodName)) return

        val tableInfo =
            try {
                FinderEntityResolver.resolveTable(method)
            } catch (e: PsiInvalidElementAccessException) {
                null
            } ?: return

        val project = method.project

        ApplicationManager.getApplication().runReadAction {
            ProgressManager.checkCanceled()

            val table = LookupBuilder.findTable(project, tableInfo.tableName) ?: return@runReadAction
            val columns = LookupBuilder.getColumnsForTable(project, table.name)

            columns.forEach { column ->
                ProgressManager.checkCanceled()
                result.addElement(LookupBuilder.forColumn(column, project, null))
            }
        }
    }

    private fun isBuilderColumnAcceptingMethod(methodName: String) = BuilderMethods.ColumnMethods.contains(methodName)

    private fun isBuilderMethodAcceptingColumnArguments(methodName: String) =
        BuilderMethods.TableMethods.filter { it.endsWith("join", true) }.contains(methodName)

    private fun isBuilderColumnArrayMethod(methodName: String) = BuilderMethods.ColumnArrayMethods.contains(methodName)
}

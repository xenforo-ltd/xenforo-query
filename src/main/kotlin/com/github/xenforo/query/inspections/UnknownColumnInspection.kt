package com.github.xenforo.query.inspections

import com.github.xenforo.query.constants.BuilderMethods
import com.github.xenforo.query.settings.XenForoQuerySettings
import com.github.xenforo.query.utils.LookupBuilder
import com.github.xenforo.query.utils.QueryChainResolver
import com.github.xenforo.query.utils.XenForoClassDetector
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor

class UnknownColumnInspection : LocalInspectionTool() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
    ): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpStringLiteralExpression(expression: StringLiteralExpression) {
                val project = expression.project
                val settings = XenForoQuerySettings.getInstance(project)

                if (!settings.isInspectionsEnabled) return

                val method = QueryChainResolver.findMethodReference(expression) ?: return
                val methodName = method.name ?: return

                val isColumnMethod = BuilderMethods.ColumnMethods.contains(methodName)
                val isArrayMethod = BuilderMethods.ColumnArrayMethods.contains(methodName)
                if (!isColumnMethod && !isArrayMethod) return
                if (!XenForoClassDetector.isXenForoQueryBuilder(method)) return

                if (isArrayMethod && !QueryChainResolver.isStringLiteralArrayKeyInColumnArray(expression)) return
                if (isColumnMethod) {
                    val args = method.parameters
                    if (args.isEmpty() || args[0] !== expression) return
                }

                val tableContexts = QueryChainResolver.resolveTables(method)
                if (tableContexts.isEmpty()) return

                val columnRef = expression.contents.trim()
                if (columnRef.isEmpty()) return

                val (alias, columnName) =
                    if (columnRef.contains(".")) {
                        val parts = columnRef.split(".")
                        parts[0] to parts.getOrNull(1)
                    } else {
                        null to columnRef
                    }

                if (columnName.isNullOrEmpty()) return

                val availableColumns =
                    tableContexts.flatMap { ctx ->
                        if (alias != null && ctx.alias != alias && ctx.baseTable != alias) {
                            return@flatMap emptyList()
                        }

                        val table =
                            LookupBuilder.findTable(project, ctx.baseTable)
                                ?: return@flatMap emptyList()

                        LookupBuilder.getColumnsForTable(project, table.name).map { it.name }
                    }

                // Can't validate if no tables resolved - avoid false positives
                if (availableColumns.isEmpty() && tableContexts.isNotEmpty()) return

                if (!availableColumns.any { it.equals(columnName, ignoreCase = true) }) {
                    holder.registerProblem(
                        expression,
                        "Unknown column: $columnRef",
                        ProblemHighlightType.WARNING,
                    )
                }
            }
        }
    }
}

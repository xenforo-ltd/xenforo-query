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

class UnknownTableInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpStringLiteralExpression(expression: StringLiteralExpression) {
                val project = expression.project
                val settings = XenForoQuerySettings.getInstance(project)

                if (!settings.isInspectionsEnabled) return

                val method = QueryChainResolver.findMethodReference(expression) ?: return
                val methodName = method.name ?: return
                if (!BuilderMethods.TableMethods.contains(methodName)) return
                if (!XenForoClassDetector.isXenForoQueryBuilder(method)) return

                val args = method.parameters
                if (args.isEmpty() || args[0] !== expression) return

                val tableName = expression.contents.split(Regex("(?i)\\s+as\\s+|\\s+")).firstOrNull()?.trim()
                if (tableName.isNullOrEmpty()) return

                val tableExists = LookupBuilder.findTable(project, tableName) != null
                if (!tableExists) {
                    holder.registerProblem(expression, "Unknown table: $tableName", ProblemHighlightType.WARNING)
                }
            }
        }
    }
}

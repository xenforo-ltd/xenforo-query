package com.github.xenforo.query.injector

import com.github.xenforo.query.settings.XenForoQuerySettings
import com.github.xenforo.query.utils.QueryChainResolver
import com.github.xenforo.query.utils.XenForoClassDetector
import com.intellij.lang.Language
import com.intellij.openapi.util.TextRange
import com.intellij.psi.InjectedLanguagePlaces
import com.intellij.psi.LanguageInjector
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.sql.psi.SqlLanguage
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.NewExpression
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class SqlInjector : LanguageInjector {
    override fun getLanguagesToInject(host: PsiLanguageInjectionHost, registrar: InjectedLanguagePlaces) {
        if (host !is StringLiteralExpression) {
            return
        }

        val project = host.project
        val settings = XenForoQuerySettings.getInstance(project)
        if (!settings.isSqlInjectionEnabled) return

        val psiElement = host as PsiElement

        val parentMethodCall = PsiTreeUtil.getParentOfType(psiElement, MethodReference::class.java)
        if (parentMethodCall != null) {
            if (!XenForoClassDetector.isXenForoQueryBuilder(parentMethodCall)) return

            val methodName = parentMethodCall.name
            val rawMethods = setOf("selectRaw", "whereRaw", "havingRaw", "orderByRaw", "groupByRaw")

            if (methodName in rawMethods) {
                val parameterList = parentMethodCall.parameterList
                if (parameterList != null) {
                    val parameters = parameterList.parameters
                    if (parameters.isNotEmpty() && parameters[0] == psiElement) {
                        injectSql(registrar, host, parentMethodCall)
                        return
                    }
                }
            }

            val nonRawMethods = setOf("select", "where", "having", "orderBy", "groupBy")
            if (methodName in nonRawMethods) {
                val parameterList = parentMethodCall.parameterList
                if (parameterList != null) {
                    val parameters = parameterList.parameters
                    if (parameters.isNotEmpty()) {
                        val firstArg = parameters[0]
                        if (firstArg is NewExpression) {
                            val classReference = firstArg.classReference
                            if (classReference != null && classReference.text == "RawExpression") {
                                val rawExpressionParameterList = firstArg.parameterList
                                if (rawExpressionParameterList != null) {
                                    val rawExpressionParameters = rawExpressionParameterList.parameters
                                    val isFirstArg =
                                        rawExpressionParameters.isNotEmpty() && rawExpressionParameters[0] == psiElement
                                    if (isFirstArg) {
                                        injectSql(registrar, host, parentMethodCall)
                                        return
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun injectSql(
        registrar: InjectedLanguagePlaces,
        host: PsiLanguageInjectionHost,
        methodCall: MethodReference,
    ) {
        val sqlLanguage: Language = SqlLanguage.INSTANCE
        val textRange = host.textRange
        val injectionRange = TextRange(1, textRange.length - 1)

        val prefix = "SELECT "

        val tableContexts = QueryChainResolver.resolveTables(methodCall)
        val suffix = buildFromClause(tableContexts)

        registrar.addPlace(sqlLanguage, injectionRange, prefix, suffix)
    }

    private fun buildFromClause(tableContexts: List<QueryChainResolver.TableContext>): String {
        if (tableContexts.isEmpty()) {
            return ""
        }

        val mainTableContext = tableContexts.first()
        val mainTable = mainTableContext.baseTable
        val mainTableAlias = mainTableContext.alias
        val fromClause = mutableListOf<String>()

        fromClause.add("FROM $mainTable" + if (mainTableAlias != null) " AS $mainTableAlias" else "")

        tableContexts.drop(1).forEach { context ->
            if (context.baseTable.isNotEmpty() && context.joinType != null && context.joinCondition != null) {
                val tableWithAlias = context.baseTable + if (context.alias != null) " AS ${context.alias}" else ""
                fromClause.add("${context.joinType} $tableWithAlias ON ${context.joinCondition}")
            }
        }

        return " ${fromClause.joinToString(" ")}"
    }
}

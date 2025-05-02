package com.github.xenforo.query.utils

import com.github.xenforo.query.constants.BuilderMethods
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression
import com.jetbrains.php.lang.psi.elements.ArrayHashElement
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

object QueryChainResolver
{
	data class TableContext(
		val baseTable: String,
		val alias: String?,
		val joinType: String? = null,
		val joinCondition: String? = null,
	)

	fun resolveTables(startMethod: MethodReference): List<TableContext>
	{
		val tables = mutableListOf<TableContext>()
		var current: PsiElement? = startMethod

		while (current != null)
		{
			if (current is MethodReference)
			{
				val name = current.name ?: break
				val args = current.parameterList?.parameters

				if (name in BuilderMethods.TableMethods)
				{
					if (args?.isNotEmpty() == true)
					{
						if (name == "query" || name == "table")
						{
							extractTableAndAlias(args[0].text, tables, null, null)
						}
						else if (name.lowercase().endsWith("join"))
						{
							if (args.size >= 4)
							{
								val joinType = when (name.lowercase())
								{
									"leftjoin" -> "LEFT JOIN"
									"rightjoin" -> "RIGHT JOIN"
									else -> "JOIN"
								}
								val joinCondition =
									"${args[1].text.trim('\"', '\'')}${args[2].text}${args[3].text.trim('\"', '\'')}"
								extractTableAndAlias(args[0].text, tables, joinType, joinCondition)
							}
						}
					}
				}

				current = current.classReference
			}
			else if (current is StringLiteralExpression)
			{
				extractTableAndAlias(current.contents, tables, null, null)
				current = current.parent
			}
			else
			{
				break
			}
		}

		return tables.reversed()
	}

	private fun extractTableAndAlias(
		tableArgumentText: String,
		tables: MutableList<TableContext>,
		joinType: String? = null,
		joinCondition: String? = null,
	)
	{
		val text = tableArgumentText.trim('\"', '\'').trim()
		val parts = text.split(Regex("(?i)\\s+as\\s+|\\s+"))
		val baseTable = parts.getOrNull(0)?.trim()
		val alias = parts.getOrNull(1)?.trim()

		if (baseTable != null && baseTable.isNotEmpty())
		{
			tables.add(TableContext(baseTable, alias, joinType, joinCondition))
		}
	}

	fun findMethodReference(position: PsiElement): MethodReference?
	{
		var current: PsiElement? = position.parent
		var depth = 0

		while (current != null && depth < 20)
		{
			if (current is MethodReference)
			{
				return current
			}

			current = current.parent
			depth++
		}

		return null
	}

	fun isStringLiteralArrayKeyInColumnArray(element: PsiElement): Boolean
	{
		val literalParent = PsiTreeUtil.getParentOfType(
			element, StringLiteralExpression::class.java
		) ?: return false

		PsiTreeUtil.getParentOfType(
			literalParent,
			ArrayHashElement::class.java
		)?.let { hash ->
			hash.key?.let { keyExpr ->
				return keyExpr === literalParent
			}
			return hash.value === literalParent
		}

		return PsiTreeUtil.getParentOfType(
			literalParent,
			ArrayCreationExpression::class.java
		) != null
	}
}
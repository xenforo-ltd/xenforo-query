package com.github.xenforo.query.utils

import com.github.xenforo.query.constants.BuilderMethods
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

object QueryChainResolver
{
	data class TableContext(val baseTable: String, val alias: String?)

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
						extractTableAndAlias(args[0].text, tables)
					}
				}

				current = current.classReference
			}
			else if (current is StringLiteralExpression)
			{
				extractTableAndAlias(current.contents, tables)
				current = current.parent
			}
			else
			{
				break
			}
		}

		return tables.reversed()
	}

	private fun extractTableAndAlias(tableArgumentText: String, tables: MutableList<TableContext>)
	{
		val text = tableArgumentText.trim('\"', '\'').trim()
		val parts = text.split(Regex("(?i)\\s+as\\s+|\\s+"))
		val baseTable = parts.getOrNull(0)?.trim()
		val alias = parts.getOrNull(1)?.trim()

		if (baseTable != null && baseTable.isNotEmpty())
		{
			tables.add(TableContext(baseTable, alias))
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
}

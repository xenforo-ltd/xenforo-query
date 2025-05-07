package com.github.xenforo.query.reference

import com.intellij.database.model.DasColumn
import com.intellij.database.model.DasTable
import com.intellij.database.psi.DbDataSource
import com.intellij.database.psi.DbElement
import com.intellij.database.psi.DbPsiFacade
import com.intellij.database.util.DbUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.sql.symbols.DasPsiWrappingSymbol

class ColumnPsiReference(
	element: PsiElement,
	private val targetColumn: DasColumn,
	private val project: Project,
) : PsiReferenceBase<PsiElement>(element, true)
{

	override fun resolve(): PsiElement?
	{
		val columnName = targetColumn.name

		val table = targetColumn.dasParent as? DasTable

		val dbPsiFacade = DbPsiFacade.getInstance(project)
		val dbElement = dbPsiFacade.findElement(targetColumn)
		if (dbElement != null)
		{
			return dbElement
		}

		DbUtil.getDataSources(project).forEach { dataSource ->
			if (dataSource is DbDataSource)
			{
				val dbTable = dbPsiFacade.findElement(table)
				if (dbTable is DbElement)
				{
					val children = dbTable.children
					val columnElement = children.find { child ->
						when
						{
							child is PsiNamedElement -> child.name?.equals(columnName, ignoreCase = true) ?: false
							else -> false
						}
					}

					if (columnElement != null)
					{
						return columnElement
					}
				}
			}
		}

		val symbol = DasPsiWrappingSymbol(targetColumn, project)
		val navElement = symbol.navigationElement
		if (navElement !== symbol)
		{
			return navElement
		}

		return null
	}
}

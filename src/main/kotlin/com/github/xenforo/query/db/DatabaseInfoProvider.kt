package com.github.xenforo.query.db

import com.intellij.database.model.DasNamespace
import com.intellij.database.model.DasTable
import com.intellij.database.psi.DbPsiFacade
import com.intellij.database.util.DasUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.containers.JBIterable

class DatabaseInfoProvider(private val project: Project)
{
	private val LOG = Logger.getInstance(DatabaseInfoProvider::class.java)

	private val systemSchemaNames =
		setOf(
			"information_schema",
			"mysql",
			"performance_schema",
			"sys",
		)
			.map { it.lowercase() }
			.toSet()

	fun getDatabaseTables(): List<DasTable>
	{
		val dbPsiFacade = DbPsiFacade.getInstance(project)
		val tables = mutableListOf<DasTable>()
		val dataSources = dbPsiFacade.dataSources

		if (LOG.isDebugEnabled)
		{
			LOG.debug("Found ${dataSources.size} data sources")
		}

		for (dataSource in dataSources)
		{
			if (LOG.isDebugEnabled)
			{
				LOG.debug("Processing data source: ${dataSource.name}")
			}
			val dbTables: JBIterable<out DasTable> =
				DasUtil.getTables(dataSource)

			for (table in dbTables)
			{
				val schema = table.dasParent as? DasNamespace
				val schemaName = schema?.name?.lowercase()

				if (schemaName != null && systemSchemaNames.contains(schemaName))
				{
					if (LOG.isDebugEnabled)
					{
						LOG.debug("Skipping table in system schema: ${table.name} (schema: $schemaName)")
					}
					continue
				}

				if (LOG.isDebugEnabled)
				{
					LOG.debug("Found user table: ${table.name} (schema: $schemaName)")
				}
				tables.add(table)
			}
		}
		return tables
	}
}

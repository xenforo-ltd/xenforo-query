package com.github.xenforo.query.utils

import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.database.model.DasColumn
import com.intellij.database.model.DasTable
import com.intellij.database.util.DasUtil
import com.intellij.database.util.DbUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.sql.symbols.DasPsiWrappingSymbol

object LookupBuilder {
    private const val TABLE_PRIORITY = 100.0
    private const val COLUMN_PRIORITY = 90.0

    private val CACHED_TABLES_KEY = Key.create<CachedValue<List<DasTable>>>("com.github.xenforo.query.CACHED_TABLES")
    private val CACHED_COLUMNS_KEY =
        Key.create<CachedValue<Map<String, List<DasColumn>>>>("com.github.xenforo.query.CACHED_COLUMNS")

    fun forTable(table: DasTable, project: Project): LookupElement {
        val builder =
            LookupElementBuilder.create(table.name)
                .withIcon(DasPsiWrappingSymbol(table, project).getIcon(false))
                .withTailText("  (${table.dasParent?.name ?: ""})", true)
                .withInsertHandler(
                    DeclarativeInsertHandler.Builder().insertOrMove(table.name).triggerAutoPopup().build()
                )

        return PrioritizedLookupElement.withPriority(builder, TABLE_PRIORITY)
    }

    fun forColumn(column: DasColumn, project: Project, alias: String? = null): LookupElement {
        val lookupString = if (alias != null) "$alias.${column.name}" else column.name

        val builder =
            LookupElementBuilder.create(lookupString)
                .withPresentableText(column.name)
                .withTailText(if (alias != null) " ($alias)" else "", true)
                .withIcon(DasPsiWrappingSymbol(column, project).getIcon(false))
                .withTypeText(column.dasType.toDataType().toString(), true)
                .withInsertHandler(DeclarativeInsertHandler.Builder().insertOrMove(lookupString).build())

        return PrioritizedLookupElement.withPriority(builder, COLUMN_PRIORITY)
    }

    fun getAllTables(project: Project): List<DasTable> {
        val manager = CachedValuesManager.getManager(project)
        return manager.getCachedValue(
            project,
            CACHED_TABLES_KEY,
            {
                val tables = fetchAllTables(project)
                val trackers = DbUtil.getDataSources(project).mapNotNull { it.modificationTracker }
                val dependencies = trackers.ifEmpty { listOf(ModificationTracker.NEVER_CHANGED) }
                CachedValueProvider.Result.create(tables, dependencies)
            },
            false,
        )
    }

    fun getColumnsForTable(project: Project, tableName: String): List<DasColumn> {
        val manager = CachedValuesManager.getManager(project)
        val columnsMap =
            manager.getCachedValue(
                project,
                CACHED_COLUMNS_KEY,
                {
                    val map = buildColumnsMap(project)
                    val trackers = DbUtil.getDataSources(project).mapNotNull { it.modificationTracker }
                    val dependencies = trackers.ifEmpty { listOf(ModificationTracker.NEVER_CHANGED) }
                    CachedValueProvider.Result.create(map, dependencies)
                },
                false,
            )

        return columnsMap[tableName.lowercase()] ?: emptyList()
    }

    fun findTable(project: Project, tableName: String): DasTable? =
        getAllTables(project).firstOrNull { it.name.equals(tableName, ignoreCase = true) }

    private fun fetchAllTables(project: Project): List<DasTable> {
        return DbUtil.getDataSources(project)
            .asSequence()
            .flatMap { ds -> DasUtil.getTables(ds).asSequence() }
            .distinctBy { it.name.lowercase() }
            .sortedBy { it.name }
            .toList()
    }

    private fun buildColumnsMap(project: Project): Map<String, List<DasColumn>> {
        val result = mutableMapOf<String, List<DasColumn>>()

        DbUtil.getDataSources(project).forEach { ds ->
            DasUtil.getTables(ds).forEach { table ->
                val key = table.name.lowercase()
                if (!result.containsKey(key)) {
                    result[key] = DasUtil.getColumns(table).toList()
                }
            }
        }

        return result
    }
}

package com.github.xenforo.query.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "XenForoQuerySettings",
    storages = [Storage("xenforoQuery.xml")],
)
@Service(Service.Level.PROJECT)
class XenForoQuerySettings : PersistentStateComponent<XenForoQuerySettings.State> {
    data class State(
        var enableTableCompletion: Boolean = true,
        var enableColumnCompletion: Boolean = true,
        var enableTableReferences: Boolean = true,
        var enableColumnReferences: Boolean = true,
        var enableSqlInjection: Boolean = true,
        var enableInspections: Boolean = true,
        // Data source filtering
        var filterDataSources: Boolean = false,
        var allowedDataSourceNames: MutableList<String> = mutableListOf(),
        // Schema filtering
        var excludeSystemSchemas: Boolean = true,
        var customExcludedSchemas: MutableList<String> = mutableListOf(),
        // Table prefix handling
        var tablePrefix: String = "xf_",
        var requireTablePrefix: Boolean = false,
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, this.myState)
    }

    companion object {
        fun getInstance(project: Project): XenForoQuerySettings {
            return project.service()
        }
    }

    // Convenience accessors
    val isTableCompletionEnabled: Boolean get() = myState.enableTableCompletion
    val isColumnCompletionEnabled: Boolean get() = myState.enableColumnCompletion
    val isTableReferencesEnabled: Boolean get() = myState.enableTableReferences
    val isColumnReferencesEnabled: Boolean get() = myState.enableColumnReferences
    val isSqlInjectionEnabled: Boolean get() = myState.enableSqlInjection
    val isInspectionsEnabled: Boolean get() = myState.enableInspections

    val shouldFilterDataSources: Boolean get() = myState.filterDataSources
    val allowedDataSources: List<String> get() = myState.allowedDataSourceNames

    val shouldExcludeSystemSchemas: Boolean get() = myState.excludeSystemSchemas
    val excludedSchemas: List<String> get() = myState.customExcludedSchemas

    val tablePrefix: String get() = myState.tablePrefix
    val requiresTablePrefix: Boolean get() = myState.requireTablePrefix
}

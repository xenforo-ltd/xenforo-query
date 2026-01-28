package com.github.xenforo.query.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel

class XenForoQueryConfigurable(project: Project) : BoundConfigurable("XenForo Query") {
    private val settings = XenForoQuerySettings.getInstance(project)

    override fun createPanel(): DialogPanel =
        panel {
            group("Features") {
                row {
                    checkBox("Enable table name completion")
                        .bindSelected(settings.state::enableTableCompletion)
                }
                row {
                    checkBox("Enable column name completion")
                        .bindSelected(settings.state::enableColumnCompletion)
                }
                row {
                    checkBox("Enable table references (Go to Definition)")
                        .bindSelected(settings.state::enableTableReferences)
                }
                row {
                    checkBox("Enable column references (Go to Definition)")
                        .bindSelected(settings.state::enableColumnReferences)
                }
                row {
                    checkBox("Enable SQL language injection")
                        .bindSelected(settings.state::enableSqlInjection)
                }
                row {
                    checkBox("Enable inspections for unknown tables/columns")
                        .bindSelected(settings.state::enableInspections)
                }
            }

            group("Schema Filtering") {
                row {
                    checkBox("Exclude system schemas (information_schema, mysql, performance_schema, sys)")
                        .bindSelected(settings.state::excludeSystemSchemas)
                }
                row("Additional excluded schemas:") {
                    textField()
                        .bindText(
                            { settings.state.customExcludedSchemas.joinToString(", ") },
                            { value ->
                                settings.state.customExcludedSchemas =
                                    value
                                        .split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                        .toMutableList()
                            },
                        )
                        .comment("Comma-separated list of schema names to exclude")
                        .align(AlignX.FILL)
                }
            }

            group("Table Prefix") {
                row("Default table prefix:") {
                    textField()
                        .bindText(settings.state::tablePrefix)
                        .comment("Common prefix for XenForo tables (e.g., xf_)")
                }
                row {
                    checkBox("Only show tables with this prefix")
                        .bindSelected(settings.state::requireTablePrefix)
                }
            }
        }
}

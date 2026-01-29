package com.github.xenforo.query.constants

object FinderMethods {
    /** Methods that accept a column name as the first string argument. */
    val ColumnMethods =
        setOf(
            "where",
            "whereOr",
            "whereId",
            "whereIds",
            "order",
            "setDefaultOrder",
            "columnSqlName",
            "columnUtf8",
            "caseInsensitive",
            "fetchColumns",
        )

    const val FINDER_FQN = "\\XF\\Mvc\\Entity\\Finder"
    const val ENTITY_FQN = "\\XF\\Mvc\\Entity\\Entity"
}

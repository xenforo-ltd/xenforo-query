package com.github.xenforo.query.constants

object BuilderMethods
{
	val TableMethods = listOf(
		"query", "table", "join", "leftJoin", "rightJoin"
	)

	val ColumnMethods = listOf(
		"select", "where", "whereIn", "whereLike", "whereNotIn", "whereNotLike",
		"whereNull", "whereNotNull", "whereExists", "whereNotExists",
		"orWhere", "orWhereLike", "orWhereNotLike",
		"groupBy", "orderBy", "orderByDesc",
		"find", "having", "orHaving",
	)

	val ColumnArrayMethods = listOf(
		"insert", "insertOrIgnore", "insertGetId", "upsert", "update"
	)
}
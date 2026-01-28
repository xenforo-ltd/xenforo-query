<?php

namespace XF\Mvc\Query;

/**
 * XenForo Query Builder stub for testing
 */
class Builder
{
    // Entry point methods
    public function query(?string $table = null): self {}
    public function table(string $table): self {}
    public function from(string $table): self {}

    // Select methods
    public function select(string|array $columns): self {}
    public function addSelect(string|array $columns): self {}
    public function selectRaw(string $expression, array $bindings = []): self {}
    public function distinct(): self {}

    // Where methods
    public function where(string|array|\Closure $column, $operator = null, $value = null): self {}
    public function orWhere(string|array|\Closure $column, $operator = null, $value = null): self {}
    public function whereIn(string $column, array $values): self {}
    public function whereNotIn(string $column, array $values): self {}
    public function orWhereIn(string $column, array $values): self {}
    public function orWhereNotIn(string $column, array $values): self {}
    public function whereLike(string $column, string $value): self {}
    public function whereNotLike(string $column, string $value): self {}
    public function orWhereLike(string $column, string $value): self {}
    public function orWhereNotLike(string $column, string $value): self {}
    public function whereNull(string $column): self {}
    public function whereNotNull(string $column): self {}
    public function orWhereNull(string $column): self {}
    public function orWhereNotNull(string $column): self {}
    public function whereExists(\Closure $callback): self {}
    public function whereNotExists(\Closure $callback): self {}
    public function whereBetween(string $column, array $values): self {}
    public function whereNotBetween(string $column, array $values): self {}
    public function orWhereBetween(string $column, array $values): self {}
    public function orWhereNotBetween(string $column, array $values): self {}
    public function whereDate(string $column, string $operator, $value = null): self {}
    public function whereMonth(string $column, string $operator, $value = null): self {}
    public function whereDay(string $column, string $operator, $value = null): self {}
    public function whereYear(string $column, string $operator, $value = null): self {}
    public function whereTime(string $column, string $operator, $value = null): self {}
    public function whereColumn(string $first, string $operator, string $second): self {}
    public function orWhereColumn(string $first, string $operator, string $second): self {}
    public function whereRaw(string $sql, array $bindings = []): self {}
    public function orWhereRaw(string $sql, array $bindings = []): self {}

    // Join methods
    public function join(string $table, string $first, string $operator, string $second): self {}
    public function leftJoin(string $table, string $first, string $operator, string $second): self {}
    public function rightJoin(string $table, string $first, string $operator, string $second): self {}
    public function crossJoin(string $table): self {}
    public function joinSub($query, string $as, string $first, string $operator, string $second): self {}
    public function leftJoinSub($query, string $as, string $first, string $operator, string $second): self {}
    public function rightJoinSub($query, string $as, string $first, string $operator, string $second): self {}

    // Grouping and ordering
    public function groupBy(string|array $columns): self {}
    public function having(string $column, $operator = null, $value = null): self {}
    public function orHaving(string $column, $operator = null, $value = null): self {}
    public function havingRaw(string $sql, array $bindings = []): self {}
    public function orderBy(string $column, string $direction = 'asc'): self {}
    public function orderByDesc(string $column): self {}
    public function orderByRaw(string $sql, array $bindings = []): self {}
    public function latest(string $column = 'created_at'): self {}
    public function oldest(string $column = 'created_at'): self {}

    // Limit and offset
    public function limit(int $value): self {}
    public function offset(int $value): self {}
    public function skip(int $value): self {}
    public function take(int $value): self {}
    public function forPage(int $page, int $perPage = 15): self {}

    // Fetching results
    public function get(): array {}
    public function first(): ?array {}
    public function find(mixed $id): ?array {}
    public function value(string $column): mixed {}
    public function pluck(string $column, ?string $key = null): array {}
    public function count(string $column = '*'): int {}
    public function max(string $column): mixed {}
    public function min(string $column): mixed {}
    public function avg(string $column): float {}
    public function sum(string $column): float {}
    public function exists(): bool {}
    public function doesntExist(): bool {}

    // Insert methods
    public function insert(array $values): bool {}
    public function insertOrIgnore(array $values): int {}
    public function insertGetId(array $values, ?string $sequence = null): int {}
    public function upsert(array $values, array $uniqueBy, ?array $update = null): int {}
    public function updateOrInsert(array $attributes, array $values = []): bool {}

    // Update methods
    public function update(array $values): int {}
    public function increment(string $column, int $amount = 1, array $extra = []): int {}
    public function decrement(string $column, int $amount = 1, array $extra = []): int {}

    // Delete methods
    public function delete(): int {}
    public function deleteWhere(string $column, $operator = null, $value = null): int {}
    public function truncate(): void {}

    // Conditional methods
    public function when(bool $condition, callable $callback, ?callable $default = null): self {}
    public function unless(bool $condition, callable $callback, ?callable $default = null): self {}
    public function tap(callable $callback): self {}

    // Locking
    public function lock(bool $value = true): self {}
    public function lockForUpdate(): self {}
    public function sharedLock(): self {}
}

namespace XF\Db;

/**
 * XenForo Database Adapter stub
 */
abstract class AbstractAdapter
{
    public function fetchRow(string $sql, array $params = []): ?array {}
    public function fetchAll(string $sql, array $params = []): array {}
    public function fetchOne(string $sql, array $params = []): mixed {}
    public function fetchCol(string $sql, array $params = []): array {}
    public function query(string $sql, array $params = []): mixed {}
    public function insert(string $table, array $data): int {}
    public function update(string $table, array $data, string $where): int {}
    public function delete(string $table, string $where): int {}
}

namespace {
    /**
     * Main XenForo application class stub
     */
    class XF
    {
        public static function query(?string $table = null): \XF\Mvc\Query\Builder {}
        public static function db(): \XF\Db\AbstractAdapter {}
        public static function app(): object {}
        public static function options(): object {}
        public static function visitor(): object {}
        public static function language(): object {}
        public static function phrase(string $name, array $params = []): string {}
    }
}

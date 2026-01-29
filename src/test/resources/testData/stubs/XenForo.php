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

namespace XF\Mvc\Entity;

/**
 * XenForo Entity Structure stub
 */
class Structure
{
    public $shortName;
    public $contentType;
    public $table;
    public $primaryKey;
    public $columns = [];
    public $relations = [];
    public $getters = [];
    public $defaultWith = [];
    public $options = [];
    public $behaviors = [];
    public $columnAliases = [];
    public $withAliases = [];
}

/**
 * XenForo base Entity stub
 */
abstract class Entity implements \ArrayAccess
{
    public const INT = 0x0001;
    public const UINT = 0x0002;
    public const FLOAT = 0x0003;
    public const BOOL = 0x10004;
    public const STR = 0x0005;
    public const BINARY = 0x0006;
    public const SERIALIZED = 0x10007;
    public const JSON = 0x10009;
    public const JSON_ARRAY = 0x10010;
    
    abstract public static function getStructure(Structure $structure);
}

/**
 * XenForo base Finder stub
 * @template T of Entity
 */
class Finder implements \IteratorAggregate
{
    protected $structure;
    
    public function where($condition, $operator = null, $value = null): static {}
    public function whereOr(array $conditionA, ?array $conditionB = null): static {}
    public function whereId($id): static {}
    public function whereIds(array $ids): static {}
    public function order($field, $direction = 'ASC'): static {}
    public function setDefaultOrder($field, $direction = 'ASC'): static {}
    public function with($name, $mustExist = false): static {}
    public function limit($limit, $offset = null): static {}
    public function offset($offset): static {}
    public function fetch($limit = null, $offset = null): AbstractCollection {}
    public function fetchOne($offset = null): ?Entity {}
    public function total(): int {}
    public function getIterator(): \Traversable {}
}

/**
 * XenForo AbstractCollection stub
 * @template T of Entity
 */
class AbstractCollection implements \IteratorAggregate, \Countable, \ArrayAccess
{
    public function getIterator(): \Traversable {}
    public function count(): int {}
    public function offsetExists($offset): bool {}
    public function offsetGet($offset): mixed {}
    public function offsetSet($offset, $value): void {}
    public function offsetUnset($offset): void {}
}

/**
 * XenForo Entity Manager stub
 */
class Manager
{
    /**
     * @template T of Finder
     * @param class-string<T> $class
     * @return T
     */
    public function getFinder(string $class): Finder {}
    
    public function find(string $shortName, $id): ?Entity {}
    public function create(string $shortName): Entity {}
}

namespace XF\Entity;

use XF\Mvc\Entity\Entity;
use XF\Mvc\Entity\Structure;

/**
 * Test Thread Entity stub
 * 
 * @property int|null $thread_id
 * @property int $node_id
 * @property string $title
 * @property int $reply_count
 * @property int $view_count
 * @property int $user_id
 * @property string $username
 * @property int $post_date
 * @property bool $sticky
 * @property string $discussion_state
 * @property bool $discussion_open
 * @property string $discussion_type
 */
class Thread extends Entity
{
    public static function getStructure(Structure $structure)
    {
        $structure->table = 'xf_thread';
        $structure->shortName = 'XF:Thread';
        $structure->primaryKey = 'thread_id';
        $structure->columns = [
            'thread_id' => ['type' => self::UINT, 'autoIncrement' => true, 'nullable' => true],
            'node_id' => ['type' => self::UINT, 'required' => true],
            'title' => ['type' => self::STR, 'maxLength' => 150, 'required' => true],
            'reply_count' => ['type' => self::UINT, 'default' => 0],
            'view_count' => ['type' => self::UINT, 'default' => 0],
            'user_id' => ['type' => self::UINT, 'required' => true],
            'username' => ['type' => self::STR, 'maxLength' => 50, 'required' => true],
            'post_date' => ['type' => self::UINT, 'default' => 0],
            'sticky' => ['type' => self::BOOL, 'default' => false],
            'discussion_state' => ['type' => self::STR, 'default' => 'visible'],
            'discussion_open' => ['type' => self::BOOL, 'default' => true],
            'discussion_type' => ['type' => self::STR, 'maxLength' => 50, 'default' => ''],
        ];
        return $structure;
    }
}

/**
 * Test User Entity stub
 *
 * @property int|null $user_id
 * @property string $username
 * @property string $email
 * @property string $user_state
 * @property int $register_date
 * @property int $message_count
 */
class User extends Entity
{
    public static function getStructure(Structure $structure)
    {
        $structure->table = 'xf_user';
        $structure->shortName = 'XF:User';
        $structure->primaryKey = 'user_id';
        $structure->columns = [
            'user_id' => ['type' => self::UINT, 'autoIncrement' => true, 'nullable' => true],
            'username' => ['type' => self::STR, 'maxLength' => 50, 'required' => true],
            'email' => ['type' => self::STR, 'maxLength' => 120, 'required' => true],
            'user_state' => ['type' => self::STR, 'default' => 'valid'],
            'register_date' => ['type' => self::UINT, 'default' => 0],
            'message_count' => ['type' => self::UINT, 'default' => 0],
        ];
        return $structure;
    }
}

namespace XF\Finder;

use XF\Entity\Thread;
use XF\Entity\User;
use XF\Mvc\Entity\AbstractCollection;
use XF\Mvc\Entity\Finder;

/**
 * Test ThreadFinder stub
 * 
 * @method AbstractCollection<Thread> fetch(?int $limit = null, ?int $offset = null)
 * @method Thread|null fetchOne(?int $offset = null)
 * @extends Finder<Thread>
 */
class ThreadFinder extends Finder
{
}

/**
 * Test UserFinder stub
 *
 * @method AbstractCollection<User> fetch(?int $limit = null, ?int $offset = null)
 * @method User|null fetchOne(?int $offset = null)
 * @extends Finder<User>
 */
class UserFinder extends Finder
{
}

namespace XF\Mvc;

use XF\Mvc\Entity\Entity;
use XF\Mvc\Entity\Finder;
use XF\Mvc\Entity\Manager;

/**
 * XenForo base Controller stub
 */
abstract class Controller
{
    protected $app;
    
    /**
     * @template T of Finder
     * @param class-string<T> $type
     * @return T
     */
    public function finder(string $type): Finder {}
    
    public function em(): Manager {}
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
        public static function em(): \XF\Mvc\Entity\Manager {}
    }
}

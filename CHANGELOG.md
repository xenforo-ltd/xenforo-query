<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# XenForo Query Changelog

## [Unreleased]

### Fixed

- Fixed false positive "Unknown column" warnings for keys inside nested arrays (e.g., `json_encode(['key' => ...])`)
- Fixed inspections, completions and Go To Definition for `upsert()` second and third argument arrays where values are column names
- Fixed table resolution for joins defined after select/where clauses
- Fixed inspections to validate columns from tables that ARE in the database, even when other tables are not
- Added enhanced closure resolution for joins inside closures

### Changed

- Switched to ktfmt for formatting (non-blocking)

## [1.1.3] - 2026-01-29

### Added

- Entity Finder column completion for `where()`, `order()`, `whereOr()`, and other Finder methods
- Go To Definition support for column names in Finder method calls

## [1.1.2] - 2026-01-28

### Fixed

- Removed usage of deprecated `AutoPopupController.autoPopupMemberLookup()` API
- Fixed Qodana warnings: redundant null checks and unused code

## [1.1.1] - 2026-01-28

### Fixed

- Removed usage of deprecated `DbPsiFacade.findElement()` API for better forward compatibility
- Fixed version compatibility to support PhpStorm versions 2025.1 through 2029.x

### Changed

- Added disk space cleanup to CI build and test jobs for improved reliability

## [1.1.0] - 2026-01-28

### Added

- **Closure parameter support**: Completions and inspections now work inside closure parameters passed to `where()`, `whereOr()`, and similar methods
- **Variable assignment resolution**: Completions work when the query builder is assigned to a variable (e.g., `$query = \XF::query('table'); $query->where(...)`)
- **Inspections**: New inspections warn about unknown tables and unknown columns in query builder calls
- **Auto-popup on quotes**: Completion popup automatically appears when typing inside quote characters
- Support for completion in the new `increment` and `decrement` methods
- ktlint code style enforcement for development

### Fixed

- Fixed `CachedValue` exceptions that could occur during indexing
- Improved reliability of table resolution across complex method chains

## [1.0.2] - 2025-05-09

- Add support for completion in the new `increment` and `decrement` methods

## [1.0.1] - 2025-05-09

- Updated README.md
- Add support for completion in the `deleteWhere` helper method

## [1.0.0] - 2025-05-09

- Initial release
- PhpStorm development helper for the new XenForo Query Builder
- Supports column and table completions
- Supports navigating to table and column references

[Unreleased]: https://github.com/xenforo-ltd/xenforo-query/compare/v1.1.3...HEAD
[1.1.3]: https://github.com/xenforo-ltd/xenforo-query/compare/v1.1.2...v1.1.3
[1.1.2]: https://github.com/xenforo-ltd/xenforo-query/compare/v1.1.1...v1.1.2
[1.1.1]: https://github.com/xenforo-ltd/xenforo-query/compare/v1.1.0...v1.1.1
[1.1.0]: https://github.com/xenforo-ltd/xenforo-query/compare/v1.0.2...v1.1.0
[1.0.2]: https://github.com/xenforo-ltd/xenforo-query/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/xenforo-ltd/xenforo-query/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/xenforo-ltd/xenforo-query/commits/v1.0.0

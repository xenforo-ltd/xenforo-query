# XenForo Query

![Build](https://github.com/xenforo-ltd/xenforo-query/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/27119.svg)](https://plugins.jetbrains.com/plugin/27119)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/27119.svg)](https://plugins.jetbrains.com/plugin/27119)

<img src="https://raw.githubusercontent.com/xenforo-ltd/xenforo-query/main/src/main/resources/META-INF/pluginIcon.svg" width="256" height="256" alt="XenForo Query" />

<!-- Plugin description -->
Supercharge your XenForo projects in PhpStorm with the XenForo Query plugin! It connects directly to your project's database, giving you a powerful toolkit for building queries, including:

*   **Smart Code Completion:** Get smart suggestions for table and column names right as you type. Whether you're using XenForo's query builder methods like `query`, `table`, or `join` for tables, or `select`, `where`, `orderBy`, and various insert/update functions for columns, the plugin knows your database schema and offers accurate autocompletion.
*   **Easy Navigation & References:** Jump straight from a table or column name in your PHP code to its definition in your database. No more hunting around: get to the schema details you need, fast.

The XenForo Query plugin uses your live database schema (with smart caching for speed!) to make writing, navigating, and checking your XenForo database queries a breeze.
<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "XenForo Query"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [XenForo Query on JetBrains Marketplace](https://plugins.jetbrains.com/plugin/27119) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/27119/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/xenforo-ltd/xenforo-query/releases/latest) from GitHub and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template

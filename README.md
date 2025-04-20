# XenForo Query

![Build](https://github.com/xenforo-ltd/xenforo-query/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/27119.svg)](https://plugins.jetbrains.com/plugin/27119)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/27119.svg)](https://plugins.jetbrains.com/plugin/27119)

<img src="https://raw.githubusercontent.com/xenforo-ltd/xenforo-query/main/src/main/resources/META-INF/pluginIcon.svg" width="256" height="256" alt="XenForo Query" />

<!-- Plugin description -->
The XenForo Query plugin provides assistance for writing database queries within XenForo PHP projects using PhpStorm.

Currently, the plugin offers completion suggestions for database table names when using the `\XF::query('')` method. It integrates with your IDE's configured database connections to provide relevant table names.
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

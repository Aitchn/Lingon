# Lingon
[![](https://jitpack.io/v/Aitchn/Lingon.svg)](https://jitpack.io/#Aitchn/Lingon)
> 🌐 **Languages**: [繁體中文](md/zh_tw.md) | **English** (current)

A tiny Java 11 i18n loader. Lingon reads JSON translation files from a simple folder layout, maps file paths to **dotted names** (e.g., `command/help.json` → `command.help`), and resolves keys with **primary → default locale** fallback.

> Status: early alpha; API may change slightly while we iterate.

---

## Usage

Initialize Lingon once and access it globally:

```java
Path baseDir = Paths.get("D:/Test/lingon");
Locale defaultLocale = Locale.forLanguageTag("en-US");

// Initialize
Lingon.getInstance(MyApp.class, baseDir, defaultLocale);

// Access anywhere
Lingon lingon = Lingon.getInstance();
```

---

## Features

- **JSON-first**: no DTOs required — values are read from a Jackson `JsonNode` tree.
- **Clear structure**:
  ```
  languages/
    zh_TW/
      ui.json
      command/
        help.json
    en_US/
      ui.json
  ```
- **File path → dotted name**: `ui.json` → `ui`, `command/help.json` → `command.help`.
- **Key lookup with fallback**: check the requested locale first; if missing, fall back to the default locale.
- **Dot-path access** inside a file (arrays supported): `main.title`, `menu.file`, `items[1].name`.

---

## Requirements

- **Java 11+**
- Dependencies:
    - `com.fasterxml.jackson.core:jackson-databind`
    - `org.slf4j:slf4j-api`

---

## Installation (via JitPack)

```kotlin
repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation("com.github.Aitchn:Lingon:-SNAPSHOT")
}
```

jitpack.yml:

```yaml
jdk:
  - openjdk11
```

Gradle settings:

```kotlin
java {
    toolchain { languageVersion = JavaLanguageVersion.of(11) }
    withSourcesJar()
    withJavadocJar()
}
tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}
```

---

## Directory Layout

```
src/main/resources/languages/<LOCALE>/.../*.json
```

Example:

```
src/main/resources/languages/zh_TW/ui.json
src/main/resources/languages/en_US/ui.json
```

JSON example:

```json
{
  "main": {
    "title": "標題",
    "welcome": "哈囉，{name}"
  },
  "menu": { "file": "檔案" },
  "items": [ "a", "b", "c" ]
}
```

---

## Quick Start

```java
Locale zhTW = Locale.forLanguageTag("zh-TW");
Lingon lingon = Lingon.getInstance();

LingonLang ui = lingon.get(zhTW, "ui");
String title = ui.get("main.title");
String fileLbl = ui.get("menu.file");
String second = ui.get("items[1]");
```

---

## API

### Lingon

- `getInstance(Class<?> clazz, Path baseDir, Locale defaultLocale)` – initialize
- `getInstance()` – retrieve
- `get(Locale locale, String dottedFile)` – view file
- `setDefaultLocale(Locale locale)` / `getDefaultLocale()`
- `reload()` / `reloadLocale(String)`
- `getLoadedLocales()` / `isLocaleLoaded(String)`
- `getLanguagePath()`, `getLogger()`

### LingonLang

- `get(String keyPath)` – dot-path with fallback
- `getPrimaryLocale()`, `getFallbackLocale()`
- `getPrimaryLanguageData()`, `getFallbackLanguageData()`
- Deprecated aliases: `getLocale()`, `getLang()`, `getDefaultLang()`, `getDefaultLocale()`

---

## Conventions

- Locale folder: `xx_YY` (e.g., `zh_TW`, `en_US`)
- File dotted names: `ui.json` → `ui`, `command/help.json` → `command.help`
- Key path: `a.b.c`, `arr[2].name`

---

## License

Apache-2.0. See [LICENSE](./LICENSE).

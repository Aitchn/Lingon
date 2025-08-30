# Lingon

> 🌐 **Languages**: [繁體中文](md/zh_tw.md) | **English** (current)

A tiny Java 11 i18n loader. Lingon reads JSON translation files from a simple folder layout, maps file paths to **dotted names** (e.g., `command/help.json` → `command.help`), and resolves keys with **primary → default locale** fallback.

> Status: early alpha; API may change slightly while we iterate.

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
- Dependencies used by Lingon:
    - `com.fasterxml.jackson.core:jackson-databind`
    - `org.slf4j:slf4j-api` (you choose the logging backend in your app)

---

## Installation (via JitPack)

Add JitPack to your repositories and depend on this project. Replace `USER`/`REPO`/`TAG` with your coordinates.

**Gradle (Kotlin DSL)**

```kotlin
repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation("com.github.USER:REPO:TAG")
}
```

If your build runs on JitPack and you target **Java 11**, include a minimal `jitpack.yml` in the repo:

```yaml
jdk:
  - openjdk11
```

**Recommended Gradle Java settings**

```kotlin
java {
    toolchain { languageVersion = JavaLanguageVersion.of(11) }
    withSourcesJar()
    withJavadocJar()
}
tasks.withType<JavaCompile>().configureEach {
    options.release.set(11) // produce Java 11 bytecode
}
```

---

## Directory Layout

Place translation resources in your application/module under:

```
src/main/resources/languages/<LOCALE>/.../*.json
```

Examples:
```
src/main/resources/languages/zh_TW/ui.json
src/main/resources/languages/zh_TW/command/help.json
src/main/resources/languages/en_US/ui.json
```

JSON example (`languages/zh_TW/ui.json`):

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

### 1) (Optional) Import embedded resources to a target folder

Lingon ships a small helper that **only scans the caller's JAR/classes** for `/languages/**.json` and copies missing files to your target `languages` directory.

```java
import io.aitchn.lingon.LingonResources;

Path baseDir = Paths.get("D:/Test/lingon");
Path targetLanguagesDir = baseDir.resolve("languages");

// Copies any /languages/**.json from your app/module into targetLanguagesDir (skips existing files)
LingonResources.importFromOwner(MyApp.class, targetLanguagesDir);
```

> This step is optional. If you already manage files on disk yourself, skip it.

### 2) Initialize Lingon

```java
import io.aitchn.lingon.Lingon;

Path baseDir = Paths.get("D:/Test/lingon");
Locale defaultLocale = Locale.forLanguageTag("en-US");

// Initialize Lingon with your class, base directory, and default locale
Lingon lingon = new Lingon(MyApp.class, baseDir, defaultLocale);
```

> Note: Lingon automatically loads translations from `<baseDir>/languages` during initialization.

### 3) Read a file (by dotted name) and fetch keys

`Lingon.get(locale, dottedFile)` returns a `LingonLang` view over **one file** (primary + fallback).

```java
import io.aitchn.lingon.LingonLang;

Locale zhTW = Locale.forLanguageTag("zh-TW");

// File dotted names:
//   "ui"                -> languages/<locale>/ui.json
//   "command.help"      -> languages/<locale>/command/help.json
LingonLang ui = lingon.get(zhTW, "ui");
LingonLang help = lingon.get(zhTW, "command.help");

// Dot-path lookups inside the JSON file (arrays supported with [index])
String title   = ui.get("main.title");      // tries zh_TW first, then default (en_US), otherwise returns "main.title"
String fileLbl = ui.get("menu.file");       // "檔案" (if present)
String second  = ui.get("items[1]");        // "b" (array example)
String intro   = help.get("intro");         // from zh_TW, else default locale
```

**Lookup behavior**:
- If the key exists in the primary file → return it.
- Else if it exists in the default file → return it.
- Else → return the **key string itself** (useful to spot missing strings).

---

## API Overview

### `class Lingon`

- **Constructor**  
  `Lingon(Class<?> clazz, Path baseDir, Locale defaultLocale)`  
  Uses `<baseDir>/languages` as the root directory and imports resources from the specified class.

- **LingonLang get(Locale locale, String dottedFile)**  
  Returns a file view composed of *(primary JSON, default JSON)* for that file.

- **void setDefaultLocale(Locale defaultLocale)**  
  Updates the default locale.

- **Locale getDefaultLocale()**  
  Returns the current default locale.

- **Path getLanguagePath()**  
  Returns the path to the languages directory.

### `class LingonLang`

- **String get(String keyPath)**  
  Dot-path within the JSON (supports array indices like `items[0]`).  
  Primary → default fallback; returns the key if both missing.

- **String getPrimaryLocale()**  
  Returns the primary locale identifier.

- **String getFallbackLocale()**  
  Returns the fallback locale identifier.

- **JsonNode getPrimaryLanguageData()**  
  Returns the primary language JSON data.

- **JsonNode getFallbackLanguageData()**  
  Returns the fallback language JSON data.

### `class LingonResources` (helper)

- **static void importFromOwner(Class<?> ownerClass, Path targetLanguagesDir)**  
  Scans only the **owner's** JAR/classes for `/languages/**.json` and copies them into `targetLanguagesDir`.  
  Existing files are never overwritten.

---

## Conventions

- **Locale folder name**: `xx_YY` (`[a-z]{2}_[A-Z]{2}`), e.g., `zh_TW`, `en_US`.
- **Dotted file name**:
    - `ui.json` → `ui`
    - `command/help.json` → `command.help`
- **Key path format**: `"a.b.c"` and `"arr[2].name"` → JSON Pointer `"/a/b/c"` and `"/arr/2/name"` under the hood.

---

## Troubleshooting

- **Nothing is imported from resources**  
  Ensure your files live under `src/main/resources/languages/**` (or `src/test/resources/languages/**` for tests).  
  At runtime they resolve to `build/resources/<sourceSet>/languages/**`.

- **I see the key returned verbatim**  
  That means the key was missing in both primary and default files. Add it to either file.

- **JSON5 support?**  
  Lingon expects standard JSON. You can pre-parse JSON5 and feed the normalized JSON string to Jackson if needed.

---

## License

This project is released under the **Apache-2.0** License. See [LICENSE](./LICENSE) for details.
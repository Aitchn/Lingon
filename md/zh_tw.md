# Lingon
[![](https://jitpack.io/v/Aitchn/Lingon.svg)](https://jitpack.io/#Aitchn/Lingon)
> 🌐 **語言版本**: **繁體中文** (目前) | [English](../README.md)

輕量級 Java 11 國際化載入器。Lingon 從簡單的資料夾結構中讀取 JSON 翻譯檔案，將檔案路徑對應到 **點號命名**（例如：`command/help.json` → `command.help`），並提供 **主要語言 → 預設語言** 的備援機制。

> 狀態：早期 Alpha 版本；API 可能會在迭代過程中稍作調整。

---

## 使用方式

初始化一次，之後即可全域存取：

```java
Path baseDir = Paths.get("D:/Test/lingon");
Locale defaultLocale = Locale.forLanguageTag("en-US");

// 初始化
Lingon.getInstance(MyApp.class, baseDir, defaultLocale);

// 在任何地方存取
Lingon lingon = Lingon.getInstance();
```

---

## 功能特色

- **JSON 優先**：無需 DTO —— 直接從 Jackson `JsonNode` 樹結構讀取值。
- **清晰的結構**：
  ```
  languages/
    zh_TW/
      ui.json
      command/
        help.json
    en_US/
      ui.json
  ```
- **檔案路徑 → 點號命名**：`ui.json` → `ui`，`command/help.json` → `command.help`。
- **具備備援的鍵值查詢**：先檢查請求的語言環境；如果缺失，則回退到預設語言環境。
- **檔案內點路徑存取**（支援陣列）：`main.title`、`menu.file`、`items[1].name`。

---

## 系統需求

- **Java 11+**
- 相依套件：
    - `com.fasterxml.jackson.core:jackson-databind`
    - `org.slf4j:slf4j-api`

---

## 安裝（透過 JitPack）

```kotlin
repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation("com.github.Aitchn:Lingon:-SNAPSHOT")
}
```

`jitpack.yml` 範例：

```yaml
jdk:
  - openjdk11
```

Gradle 設定：

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

## 目錄佈局

```
src/main/resources/languages/<LOCALE>/.../*.json
```

範例：

```
src/main/resources/languages/zh_TW/ui.json
src/main/resources/languages/en_US/ui.json
```

JSON 範例：

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

## 快速開始

```java
Locale zhTW = Locale.forLanguageTag("zh-TW");
Lingon lingon = Lingon.getInstance();

LingonLang ui = lingon.get(zhTW, "ui");
String title = ui.get("main.title");
String fileLbl = ui.get("menu.file");
String second = ui.get("items[1]");
```

---

## API 概覽

### Lingon

- `getInstance(Class<?> clazz, Path baseDir, Locale defaultLocale)` – 初始化
- `getInstance()` – 取得現有單例
- `get(Locale locale, String dottedFile)` – 取得檔案視圖
- `setDefaultLocale(Locale locale)` / `getDefaultLocale()`
- `reload()` / `reloadLocale(String)`
- `getLoadedLocales()` / `isLocaleLoaded(String)`
- `getLanguagePath()`, `getLogger()`

### LingonLang

- `get(String keyPath)` – JSON 點路徑查詢（支援陣列索引）
- `getPrimaryLocale()`、`getFallbackLocale()`
- `getPrimaryLanguageData()`、`getFallbackLanguageData()`
- 已棄用別名：`getLocale()`、`getLang()`、`getDefaultLang()`、`getDefaultLocale()`

---

## 慣例

- **語言環境資料夾名稱**：`xx_YY`（如 `zh_TW`, `en_US`）
- **檔案點號命名**：`ui.json` → `ui`，`command/help.json` → `command.help`
- **鍵值路徑格式**：`a.b.c`、`arr[2].name`

---

## 授權條款

此專案依據 **Apache-2.0** 授權條款發布。詳情請參閱 [LICENSE](./LICENSE)。

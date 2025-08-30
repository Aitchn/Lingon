# Lingon

> 🌐 **語言版本**: **繁體中文** (目前) | [English](../README.md)

輕量級 Java 11 國際化載入器。Lingon 從簡單的資料夾結構中讀取 JSON 翻譯檔案，將檔案路徑對應到**點號命名**（例如：`command/help.json` → `command.help`），並提供**主要語言 → 預設語言**的備援機制。

> 狀態：早期 Alpha 版本；API 可能會在迭代過程中稍作調整。

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
- Lingon 使用的相依套件：
    - `com.fasterxml.jackson.core:jackson-databind`
    - `org.slf4j:slf4j-api`（你可以在應用程式中選擇日誌後端）

---

## 安裝（透過 JitPack）

將 JitPack 加入你的倉庫並依賴此專案。將 `USER`/`REPO`/`TAG` 替換為你的座標。

**Gradle（Kotlin DSL）**

```kotlin
repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation("com.github.USER:REPO:TAG")
}
```

如果你的建置在 JitPack 上執行且目標為 **Java 11**，請在倉庫中包含最小的 `jitpack.yml`：

```yaml
jdk:
  - openjdk11
```

**建議的 Gradle Java 設定**

```kotlin
java {
    toolchain { languageVersion = JavaLanguageVersion.of(11) }
    withSourcesJar()
    withJavadocJar()
}
tasks.withType<JavaCompile>().configureEach {
    options.release.set(11) // 產生 Java 11 位元組碼
}
```

---

## 目錄佈局

將翻譯資源放在你的應用程式/模組下的：

```
src/main/resources/languages/<LOCALE>/.../*.json
```

範例：
```
src/main/resources/languages/zh_TW/ui.json
src/main/resources/languages/zh_TW/command/help.json
src/main/resources/languages/en_US/ui.json
```

JSON 範例（`languages/zh_TW/ui.json`）：

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

### 1) （可選）將內嵌資源匯入到目標資料夾

Lingon 提供一個小工具，**僅掃描呼叫者的 JAR/類別**中的 `/languages/**.json` 並將缺失的檔案複製到你的目標 `languages` 目錄。

```java
import io.aitchn.lingon.LingonResources;

Path baseDir = Paths.get("D:/Test/lingon");
Path targetLanguagesDir = baseDir.resolve("languages");

// 將你的應用程式/模組中的任何 /languages/**.json 複製到 targetLanguagesDir（跳過已存在的檔案）
LingonResources.importFromOwner(MyApp.class, targetLanguagesDir);
```

> 此步驟為可選。如果你已經自行管理磁碟上的檔案，可以跳過此步驟。

### 2) 初始化 Lingon

```java
import io.aitchn.lingon.Lingon;

Path baseDir = Paths.get("D:/Test/lingon");
Locale defaultLocale = Locale.forLanguageTag("en-US");

// 使用你的類別、基礎目錄和預設語言環境初始化 Lingon
Lingon lingon = new Lingon(MyApp.class, baseDir, defaultLocale);
```

> 注意：Lingon 在初始化期間會自動從 `<baseDir>/languages` 載入翻譯。

### 3) 讀取檔案（透過點號名稱）並取得鍵值

`Lingon.get(locale, dottedFile)` 回傳一個 `LingonLang` 視圖，包含**一個檔案**（主要 + 備援）。

```java
import io.aitchn.lingon.LingonLang;

Locale zhTW = Locale.forLanguageTag("zh-TW");

// 檔案點號名稱：
//   "ui"                -> languages/<locale>/ui.json
//   "command.help"      -> languages/<locale>/command/help.json
LingonLang ui = lingon.get(zhTW, "ui");
LingonLang help = lingon.get(zhTW, "command.help");

// JSON 檔案內的點路徑查詢（支援陣列索引）
String title   = ui.get("main.title");      // 先嘗試 zh_TW，然後預設 (en_US)，否則回傳 "main.title"
String fileLbl = ui.get("menu.file");       // "檔案"（如果存在）
String second  = ui.get("items[1]");        // "b"（陣列範例）
String intro   = help.get("intro");         // 來自 zh_TW，否則使用預設語言環境
```

**查詢行為**：
- 如果鍵值存在於主要檔案中 → 回傳該值。
- 否則如果存在於預設檔案中 → 回傳該值。
- 否則 → 回傳**鍵值字串本身**（有助於發現缺失的字串）。

---

## API 概覽

### `class Lingon`

- **建構子**  
  `Lingon(Class<?> clazz, Path baseDir, Locale defaultLocale)`  
  使用 `<baseDir>/languages` 作為根目錄，並從指定類別匯入資源。

- **LingonLang get(Locale locale, String dottedFile)**  
  回傳該檔案的檔案視圖，包含 *（主要 JSON，預設 JSON）*。

- **void setDefaultLocale(Locale defaultLocale)**  
  更新預設語言環境。

- **Locale getDefaultLocale()**  
  回傳目前的預設語言環境。

- **Path getLanguagePath()**  
  回傳語言目錄的路徑。

### `class LingonLang`

- **String get(String keyPath)**  
  JSON 內的點路徑（支援陣列索引如 `items[0]`）。  
  主要 → 預設備援；如果兩者都缺失則回傳鍵值。

- **String getPrimaryLocale()**  
  回傳主要語言環境識別碼。

- **String getFallbackLocale()**  
  回傳備援語言環境識別碼。

- **JsonNode getPrimaryLanguageData()**  
  回傳主要語言的 JSON 資料。

- **JsonNode getFallbackLanguageData()**  
  回傳備援語言的 JSON 資料。

### `class LingonResources`（工具類別）

- **static void importFromOwner(Class<?> ownerClass, Path targetLanguagesDir)**  
  僅掃描**擁有者的** JAR/類別中的 `/languages/**.json` 並將它們複製到 `targetLanguagesDir`。  
  現有檔案永遠不會被覆寫。

---

## 慣例

- **語言環境資料夾名稱**：`xx_YY`（`[a-z]{2}_[A-Z]{2}`），例如：`zh_TW`、`en_US`。
- **點號檔案名稱**：
    - `ui.json` → `ui`
    - `command/help.json` → `command.help`
- **鍵值路徑格式**：`"a.b.c"` 和 `"arr[2].name"` → 底層使用 JSON Pointer `"/a/b/c"` 和 `"/arr/2/name"`。

---

## 疑難排解

- **沒有從資源匯入任何內容**  
  確保你的檔案位於 `src/main/resources/languages/**`（或測試用的 `src/test/resources/languages/**`）。  
  在執行時期它們會解析為 `build/resources/<sourceSet>/languages/**`。

- **我看到鍵值原樣回傳**  
  這表示鍵值在主要和預設檔案中都缺失。請將其加入任一檔案中。

- **JSON5 支援？**  
  Lingon 期望標準 JSON。如果需要，你可以預先解析 JSON5 並將正規化的 JSON 字串提供給 Jackson。

---

## 授權條款

此專案依據 **Apache-2.0** 授權條款發布。詳情請參閱 [LICENSE](./LICENSE)。
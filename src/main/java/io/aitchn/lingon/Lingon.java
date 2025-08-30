package io.aitchn.lingon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class Lingon {
    private static final Logger LOGGER = LoggerFactory.getLogger(Lingon.class);
    private static final Pattern LOCALE_DIRECTORY_PATTERN = Pattern.compile("^[a-z]{2}_[A-Z]{2}$");

    private final Path languagePath;
    private final Map<String, Map<String, JsonNode>> rawTextsByLocale = new HashMap<>();
    private Locale defaultLocale;

    /**
     * Initialize Lingon with specified class, path, and default locale.
     *
     * @param clazz the class to import resources from
     * @param path the base path for language files
     * @param defaultLocale the default locale to use as fallback
     */
    public Lingon(Class<?> clazz, Path path, Locale defaultLocale) {
        this.languagePath = path.resolve("languages");
        if (languagePath.toFile().mkdirs()) {
            LOGGER.info("Created {}", languagePath);
        }

        this.defaultLocale = defaultLocale;
        load();
        LingonResources.importFromOwner(clazz, languagePath);
        LOGGER.info("Lingon initialized default locale: {}", defaultLocale);
    }

    /**
     * Load all locales and their corresponding raw text data.
     */
    private void load() {
        List<String> localeNames = loadLocales();
        for (String localeName : localeNames) {
            rawTextsByLocale.put(localeName, loadRawText(localeName));
        }
    }

    /**
     * Get a localized string for the specified locale and path.
     *
     * @param locale the locale to use for localization
     * @param path the path to the localized string
     * @return the localized string wrapper
     */
    public LingonLang get(Locale locale, String path) {
        final String primaryKey = toDirectoryName(locale);
        final String fallbackKey = toDirectoryName(defaultLocale);

        Map<String, JsonNode> primaryMap = rawTextsByLocale.getOrDefault(primaryKey, Collections.emptyMap());
        Map<String, JsonNode> fallbackMap = rawTextsByLocale.getOrDefault(fallbackKey, Collections.emptyMap());

        JsonNode primaryNode = primaryMap.get(path);
        JsonNode fallbackNode = fallbackMap.get(path);

        if (primaryNode == null && fallbackNode == null) {
            LOGGER.warn("Missing file '{}' for locales primary={} fallback={}", path, primaryKey, fallbackKey);
            return new LingonLang(
                    primaryKey, MissingNode.getInstance(),
                    fallbackKey, MissingNode.getInstance()
            );
        }

        return new LingonLang(primaryKey, primaryNode, fallbackKey, fallbackNode);
    }

    /**
     * Load all available locale directory names from the language path.
     *
     * @return a list of locale directory names
     * @throws UncheckedIOException if unable to read the directory
     */
    private List<String> loadLocales() {
        if (!Files.isDirectory(languagePath)) {
            return List.of();
        }

        List<String> localeNames = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(languagePath)) {
            for (Path path : directoryStream) {
                String fileName = path.getFileName().toString();
                if (Files.isDirectory(path) && LOCALE_DIRECTORY_PATTERN.matcher(fileName).matches()) {
                    localeNames.add(fileName);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list locales in " + languagePath, e);
        }
        return localeNames;
    }

    /**
     * Load raw text data from JSON files for a specific locale.
     *
     * @param localeName the locale directory name to load from
     * @return an unmodifiable map of dotted paths to JSON nodes
     * @throws UncheckedIOException if unable to read files
     */
    private Map<String, JsonNode> loadRawText(String localeName) {
        Path localePath = languagePath.resolve(localeName);
        if (!Files.isDirectory(localePath)) {
            return Map.of();
        }

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, JsonNode> textData = new LinkedHashMap<>();

        try (Stream<Path> pathStream = Files.walk(localePath)) {
            pathStream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .forEach(filePath -> {
                        String dottedName = toDottedName(localePath.relativize(filePath));
                        try (var reader = Files.newBufferedReader(filePath, java.nio.charset.StandardCharsets.UTF_8)) {
                            JsonNode node = objectMapper.readTree(reader);
                            textData.put(dottedName, node);
                        } catch (IOException e) {
                            throw new UncheckedIOException("Failed to read " + filePath, e);
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to walk " + localePath, e);
        }

        return Collections.unmodifiableMap(textData);
    }

    /**
     * Convert a relative file path to a dotted name format.
     *
     * @param relativePath the relative path to convert
     * @return the dotted name (e.g., "command/help.json" -> "command.help")
     */
    private static String toDottedName(Path relativePath) {
        String name = relativePath.toString().replace('\\', '/');
        if (name.toLowerCase(Locale.ROOT).endsWith(".json")) {
            name = name.substring(0, name.length() - 5);
        }
        return name.replace('/', '.');
    }

    /**
     * Convert a locale to its corresponding directory name.
     *
     * @param locale the locale to convert
     * @return the directory name (e.g., "en_US") or null if locale is null
     */
    private static String toDirectoryName(Locale locale) {
        if (locale == null) {
            return null;
        }

        String language = locale.getLanguage().toLowerCase(Locale.ROOT);
        String country = locale.getCountry().toUpperCase(Locale.ROOT);
        return country.isEmpty() ? language : (language + "_" + country);
    }

    /**
     * Set the default locale for fallback purposes.
     *
     * @param defaultLocale the default locale to set
     */
    public void setDefaultLocale(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    /**
     * Reload all locale data from the file system.
     * This method clears the existing rawTextsByLocale cache and reloads all language files.
     * Useful when language files have been modified at runtime.
     */
    public void reload() {
        LOGGER.info("Reloading language data from {}", languagePath);
        rawTextsByLocale.clear();
        load();
        LOGGER.info("Language data reloaded successfully for {} locales", rawTextsByLocale.size());
    }

    /**
     * Reload data for a specific locale only.
     * This method updates only the specified locale's data without affecting other locales.
     *
     * @param locale the locale to reload
     * @return true if the locale was successfully reloaded, false if no data was found
     */
    public boolean reloadLocale(Locale locale) {
        if (locale == null) {
            LOGGER.warn("Cannot reload null locale");
            return false;
        }

        String localeName = toDirectoryName(locale);
        if (localeName == null) {
            LOGGER.warn("Cannot convert locale {} to directory name", locale);
            return false;
        }

        LOGGER.debug("Reloading locale data for {}", localeName);
        Map<String, JsonNode> localeData = loadRawText(localeName);

        if (localeData.isEmpty()) {
            LOGGER.warn("No data found for locale {}", localeName);
            rawTextsByLocale.remove(localeName);
            return false;
        }

        rawTextsByLocale.put(localeName, localeData);
        LOGGER.info("Successfully reloaded locale {}", localeName);
        return true;
    }

    /**
     * Get all currently loaded locale names.
     *
     * @return an unmodifiable set of loaded locale names
     */
    public Set<String> getLoadedLocales() {
        return Collections.unmodifiableSet(rawTextsByLocale.keySet());
    }

    /**
     * Get the current default locale.
     *
     * @return the default locale
     */
    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    /**
     * Get the language files directory path.
     *
     * @return the language path
     */
    public Path getLanguagePath() {
        return languagePath;
    }

    /**
     * Get the logger instance for this class.
     *
     * @return the logger instance
     */
    public Logger getLogger() {
        return LOGGER;
    }
}

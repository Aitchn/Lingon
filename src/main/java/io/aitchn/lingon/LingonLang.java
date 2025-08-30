package io.aitchn.lingon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;

/**
 * Represents a localized language data holder with primary and fallback locale support.
 */
public final class LingonLang {
    private final String primaryLocale;
    private final JsonNode primaryLanguageData;
    private final String fallbackLocale;
    private final JsonNode fallbackLanguageData;

    /**
     * Creates a new LingonLang instance with primary and fallback language data.
     *
     * @param primaryLocale the primary locale identifier
     * @param primaryLanguageData the JSON data for the primary locale
     * @param fallbackLocale the fallback locale identifier
     * @param fallbackLanguageData the JSON data for the fallback locale
     */
    public LingonLang(String primaryLocale, JsonNode primaryLanguageData,
                      String fallbackLocale, JsonNode fallbackLanguageData) {
        this.primaryLocale = primaryLocale;
        this.primaryLanguageData = primaryLanguageData;
        this.fallbackLocale = fallbackLocale;
        this.fallbackLanguageData = fallbackLanguageData;
    }

    /**
     * Get a localized string by key, with automatic fallback to default locale.
     *
     * @param key the key to lookup in the language data
     * @return the localized string, or the key itself if not found in any locale
     */
    public String get(String key) {
        JsonNode node = getNodeAtPath(primaryLanguageData, key);
        if (isMissingOrNull(node)) {
            node = getNodeAtPath(fallbackLanguageData, key);
        }
        if (isMissingOrNull(node)) {
            return key;
        }
        return node.isTextual() ? node.asText() : node.toString();
    }

    /**
     * Check if a JSON node is missing, null, or represents a null value.
     *
     * @param node the node to check
     * @return true if the node is missing, null, or represents null
     */
    private static boolean isMissingOrNull(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull();
    }

    /**
     * Get a JSON node at the specified path using JSON Pointer syntax.
     *
     * @param rootNode the root node to search in
     * @param path the dot-separated path to the desired node
     * @return the node at the specified path, or MissingNode if not found
     */
    private static JsonNode getNodeAtPath(JsonNode rootNode, String path) {
        if (rootNode == null || rootNode.isMissingNode()) {
            return MissingNode.getInstance();
        }
        if (path == null || path.isEmpty()) {
            return rootNode;
        }

        // Convert dot notation to JSON Pointer format
        // e.g., "command.help" -> "/command/help"
        // e.g., "items[0].name" -> "/items/0/name"
        String jsonPointer = "/" + path.replace(".", "/").replaceAll("\\[(\\d+)]", "/$1");
        return rootNode.at(jsonPointer);
    }

    /**
     * Get the primary locale identifier.
     *
     * @return the primary locale identifier
     */
    public String getPrimaryLocale() {
        return primaryLocale;
    }

    /**
     * Get the primary language data.
     *
     * @return the JSON node containing primary language data
     */
    public JsonNode getPrimaryLanguageData() {
        return primaryLanguageData;
    }

    /**
     * Get the fallback language data.
     *
     * @return the JSON node containing fallback language data
     */
    public JsonNode getFallbackLanguageData() {
        return fallbackLanguageData;
    }

    /**
     * Get the fallback locale identifier.
     *
     * @return the fallback locale identifier
     */
    public String getFallbackLocale() {
        return fallbackLocale;
    }

    // Deprecated methods for backward compatibility

    /**
     * @deprecated Use {@link #getPrimaryLocale()} instead
     */
    @Deprecated
    public String getLocale() {
        return getPrimaryLocale();
    }

    /**
     * @deprecated Use {@link #getPrimaryLanguageData()} instead
     */
    @Deprecated
    public JsonNode getLang() {
        return getPrimaryLanguageData();
    }

    /**
     * @deprecated Use {@link #getFallbackLanguageData()} instead
     */
    @Deprecated
    public JsonNode getDefaultLang() {
        return getFallbackLanguageData();
    }

    /**
     * @deprecated Use {@link #getFallbackLocale()} instead
     */
    @Deprecated
    public String getDefaultLocale() {
        return getFallbackLocale();
    }
}

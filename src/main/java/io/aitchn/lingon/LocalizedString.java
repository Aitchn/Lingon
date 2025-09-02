package io.aitchn.lingon;

import java.util.Map;

public class LocalizedString {
    private final String template;

    public LocalizedString(String template) {
        this.template = template;
    }

    /**
     * Returns the underlying template string without any substitutions applied.
     *
     * @return the raw template string
     */
    public String raw() {
        return template;
    }

    /**
     * Replaces placeholders in the template with values from the provided map.
     * Placeholders use the format {key}, where key corresponds to an entry in the map.
     *
     * @param values a map containing key-value pairs for substitution
     * @return the resulting string after performing substitutions
     */
    public String substitute(Map<String, Object> values) {
        String result = template;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            result = result.replace(placeholder, String.valueOf(entry.getValue()));
        }
        return result;
    }

    /**
     * Formats the template string using positional arguments based on MessageFormat.
     * Placeholders in the template should follow the format {0}, {1}, etc.
     *
     * @param args the arguments to be applied to the template
     * @return the formatted string
     */
    public String format(Object... args) {
        return java.text.MessageFormat.format(template, args);
    }

    /**
     * Returns the template string as a plain string representation of this object.
     *
     * @return the template string
     */
    @Override
    public String toString() {
        return template;
    }
}

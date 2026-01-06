package net.enabify.mcfactor.language;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    private static final Yaml YAML = new Yaml();
    private final Map<String, Map<String, String>> languages;
    private String currentLanguage;
    
    private static final String[] SUPPORTED_LANGUAGES = {
        "en", "hi", "zh", "es", "ar", "fr", "ru", "pt", "id", "de", "ja"
    };
    
    public LanguageManager() {
        this.languages = new HashMap<>();
        loadLanguages();
        setLanguage("en");
    }
    
    private void loadLanguages() {
        for (String lang : SUPPORTED_LANGUAGES) {
            try (InputStream stream = getClass().getResourceAsStream("/lang/" + lang + ".yml")) {
                if (stream == null) {
                    continue;
                }

                Map<String, String> langConfig = parseLanguage(stream);
                if (!langConfig.isEmpty()) {
                    languages.put(lang, langConfig);
                }
            } catch (Exception ignored) {
                // Ignore invalid language files so other languages can still load
            }
        }
    }

    private Map<String, String> parseLanguage(InputStream stream) {
        Map<String, String> entries = new HashMap<>();
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            Object loaded = YAML.load(reader);
            if (loaded instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<?, ?> map = (Map<?, ?>) loaded;
                flattenEntries("", map, entries);
            }
        } catch (Exception ignored) {
            // Malformed YAML should not block the plugin from starting
        }
        return entries;
    }

    private void flattenEntries(String prefix, Map<?, ?> source, Map<String, String> target) {
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();
            String combinedKey = prefix.isEmpty() ? key : prefix + "." + key;

            if (value instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<?, ?> nested = (Map<?, ?>) value;
                flattenEntries(combinedKey, nested, target);
            } else if (value != null) {
                target.put(combinedKey, value.toString());
            }
        }
    }
    
    public void setLanguage(String language) {
        if (languages.containsKey(language)) {
            this.currentLanguage = language;
        } else {
            this.currentLanguage = "en";
        }
    }
    
    public String getMessage(String key) {
        Map<String, String> lang = languages.get(currentLanguage);
        if (lang != null && lang.containsKey(key)) {
            return lang.get(key);
        }
        
        // Fallback to English
        Map<String, String> defaultLang = languages.get("en");
        if (defaultLang != null && defaultLang.containsKey(key)) {
            return defaultLang.get(key);
        }
        
        return key;
    }
    
    public String getMessage(String key, Map<String, String> replacements) {
        String message = getMessage(key);
        
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        return message;
    }
}

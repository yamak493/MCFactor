package net.enabify.mcfactor.language;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    private final Map<String, FileConfiguration> languages;
    private String currentLanguage;
    
    private static final String[] SUPPORTED_LANGUAGES = {
        "en", "hi", "zh", "es", "ar", "fr", "ru", "pt", "id", "de", "ja"
    };
    
    public LanguageManager() {
        this.languages = new HashMap<>();
        loadLanguages();
    }
    
    private void loadLanguages() {
        for (String lang : SUPPORTED_LANGUAGES) {
            InputStream stream = getClass().getResourceAsStream("/lang/" + lang + ".yml");
            if (stream != null) {
                FileConfiguration langConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)
                );
                languages.put(lang, langConfig);
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
        FileConfiguration lang = languages.get(currentLanguage);
        if (lang != null && lang.contains(key)) {
            return lang.getString(key);
        }
        
        // Fallback to English
        FileConfiguration defaultLang = languages.get("en");
        if (defaultLang != null && defaultLang.contains(key)) {
            return defaultLang.getString(key);
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

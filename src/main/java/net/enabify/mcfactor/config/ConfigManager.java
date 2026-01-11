package net.enabify.mcfactor.config;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager implements MCFactorConfig {
    private final File configFile;
    private Map<String, Object> config;
    
    private static final int CONFIG_VERSION = 1;
    
    public ConfigManager(File dataFolder) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.configFile = new File(dataFolder, "config.yml");
        loadConfig();
    }
    
    private void loadConfig() {
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        
        try (FileInputStream fis = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> loadedConfig = yaml.load(fis);
            config = loadedConfig != null ? loadedConfig : new HashMap<>();
            
            // Check config version and update if needed
            int version = getIntValue("config-version", 0);
            if (version != CONFIG_VERSION) {
                updateConfig();
            }
        } catch (IOException e) {
            e.printStackTrace();
            config = new HashMap<>();
        }
    }
    
    private void createDefaultConfig() {
        try {
            // Try to copy from resources first
            InputStream resourceStream = ConfigManager.class.getResourceAsStream("/config.yml");
            if (resourceStream != null) {
                Files.copy(resourceStream, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                resourceStream.close();
            } else {
                // Fallback: create default configuration manually
                createConfigFromDefaults();
            }
        } catch (IOException e) {
            e.printStackTrace();
            createConfigFromDefaults();
        }
    }
    
    private void createConfigFromDefaults() {
        config = new HashMap<>();
        
        // Basic settings
        config.put("enabled", true);
        config.put("language", "en");
        config.put("config-version", CONFIG_VERSION);
        
        // Authentication modes
        config.put("code-authentication", true);
        config.put("app-authentication", false);
        
        // REST API settings (for app authentication)
        Map<String, Object> restApi = new HashMap<>();
        restApi.put("host", "0.0.0.0");
        restApi.put("port", 25566);
        config.put("rest-api", restApi);
        
        // Proxy mode (true if this server is behind a proxy)
        config.put("proxy-mode", false);
        
        // Server credentials (auto-generated on first run)
        config.put("server-id", "");
        config.put("secret-key", "");
        
        // Debugging (prints additional debug information when true)
        config.put("debug", false);
        
        saveConfig();
    }
    
    private void updateConfig() {
        config.put("config-version", CONFIG_VERSION);
        saveConfig();
    }
    
    @Override
    public void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);
            yaml.dump(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> getConfig() {
        return config;
    }
    
    @Override
    public boolean isEnabled() {
        return getBooleanValue("enabled", true);
    }
    
    @Override
    public String getLanguage() {
        return getStringValue("language", "en");
    }
    
    @Override
    public boolean isCodeAuthentication() {
        return getBooleanValue("code-authentication", true);
    }
    
    @Override
    public boolean isAppAuthentication() {
        return getBooleanValue("app-authentication", false);
    }
    
    @Override
    public String getRestApiHost() {
        return getNestedStringValue("rest-api", "host", "0.0.0.0");
    }
    
    @Override
    public int getRestApiPort() {
        return getNestedIntValue("rest-api", "port", 25566);
    }
    
    @Override
    public String getServerId() {
        return getStringValue("server-id", "");
    }
    
    @Override
    public void setServerId(String serverId) {
        config.put("server-id", serverId);
        saveConfig();
    }
    
    @Override
    public String getSecretKey() {
        return getStringValue("secret-key", "");
    }
    
    @Override
    public void setSecretKey(String secretKey) {
        config.put("secret-key", secretKey);
        saveConfig();
    }
    
    @Override
    public boolean isProxyMode() {
        return getBooleanValue("proxy-mode", false);
    }
    
    @Override
    public void reload() {
        loadConfig();
    }

    @Override
    public boolean isDebug() {
        return getBooleanValue("debug", false);
    }
    
    // Helper methods
    private String getStringValue(String key, String defaultValue) {
        Object value = config.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }
    
    private int getIntValue(String key, int defaultValue) {
        Object value = config.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    private boolean getBooleanValue(String key, boolean defaultValue) {
        Object value = config.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }
    
    @SuppressWarnings("unchecked")
    private String getNestedStringValue(String parentKey, String childKey, String defaultValue) {
        Object parent = config.get(parentKey);
        if (parent instanceof Map) {
            Map<String, Object> parentMap = (Map<String, Object>) parent;
            Object value = parentMap.get(childKey);
            return value instanceof String ? (String) value : defaultValue;
        }
        return defaultValue;
    }
    
    @SuppressWarnings("unchecked")
    private int getNestedIntValue(String parentKey, String childKey, int defaultValue) {
        Object parent = config.get(parentKey);
        if (parent instanceof Map) {
            Map<String, Object> parentMap = (Map<String, Object>) parent;
            Object value = parentMap.get(childKey);
            if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        }
        return defaultValue;
    }
}

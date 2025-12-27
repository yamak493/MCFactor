package net.enabify.mcfactor.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
    private final File configFile;
    private FileConfiguration config;
    
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
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Check config version and update if needed
        if (config.getInt("config-version", 0) != CONFIG_VERSION) {
            updateConfig();
        }
    }
    
    private void createDefaultConfig() {
        config = new YamlConfiguration();
        
        // Basic settings
        config.set("enabled", true);
        config.set("language", "en");
        config.set("config-version", CONFIG_VERSION);
        
        // Authentication modes
        config.set("code-authentication", true);
        config.set("app-authentication", false);
        
        // REST API settings (for app authentication)
        config.set("rest-api.host", "0.0.0.0");
        config.set("rest-api.port", 25566);
        
        // Server credentials (auto-generated on first run)
        config.set("server-id", "");
        config.set("secret-key", "");
        
        saveConfig();
    }
    
    private void updateConfig() {
        config.set("config-version", CONFIG_VERSION);
        saveConfig();
    }
    
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    public boolean isEnabled() {
        return config.getBoolean("enabled", true);
    }
    
    public String getLanguage() {
        return config.getString("language", "en");
    }
    
    public boolean isCodeAuthentication() {
        return config.getBoolean("code-authentication", true);
    }
    
    public boolean isAppAuthentication() {
        return config.getBoolean("app-authentication", false);
    }
    
    public String getRestApiHost() {
        return config.getString("rest-api.host", "0.0.0.0");
    }
    
    public int getRestApiPort() {
        return config.getInt("rest-api.port", 25566);
    }
    
    public String getServerId() {
        return config.getString("server-id", "");
    }
    
    public void setServerId(String serverId) {
        config.set("server-id", serverId);
        saveConfig();
    }
    
    public String getSecretKey() {
        return config.getString("secret-key", "");
    }
    
    public void setSecretKey(String secretKey) {
        config.set("secret-key", secretKey);
        saveConfig();
    }
    
    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }
}

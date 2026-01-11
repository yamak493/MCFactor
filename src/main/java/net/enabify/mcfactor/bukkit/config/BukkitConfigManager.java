package net.enabify.mcfactor.bukkit.config;

import net.enabify.mcfactor.config.MCFactorConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class BukkitConfigManager implements MCFactorConfig {
    private final File configFile;
    private final JavaPlugin plugin;
    private FileConfiguration config;
    
    private static final int CONFIG_VERSION = 1;
    
    public BukkitConfigManager(JavaPlugin plugin, File dataFolder) {
        this.plugin = plugin;
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
        try {
            // Try to copy from plugin resources first
            InputStream resourceStream = plugin.getResource("config.yml");
            if (resourceStream != null) {
                plugin.saveResource("config.yml", false);
                // The resource should now be in the plugin's data folder
                File tempFile = new File(plugin.getDataFolder(), "config.yml");
                if (tempFile.exists() && !tempFile.equals(configFile)) {
                    tempFile.renameTo(configFile);
                }
            } else {
                // Fallback: create default configuration manually
                createConfigFromDefaults();
            }
        } catch (Exception e) {
            e.printStackTrace();
            createConfigFromDefaults();
        }
    }
    
    private void createConfigFromDefaults() {
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
        
        // Proxy mode (true if this server is behind a proxy)
        config.set("proxy-mode", false);
        
        // Server credentials (auto-generated on first run)
        config.set("server-id", "");
        config.set("secret-key", "");
        
        // Debugging (prints additional debug information when true)
        config.set("debug", false);
        
        saveConfig();
    }
    
    private void updateConfig() {
        config.set("config-version", CONFIG_VERSION);
        saveConfig();
    }
    
    @Override
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
    
    @Override
    public boolean isEnabled() {
        return config.getBoolean("enabled", true);
    }
    
    @Override
    public String getLanguage() {
        return config.getString("language", "en");
    }
    
    @Override
    public boolean isCodeAuthentication() {
        return config.getBoolean("code-authentication", true);
    }
    
    @Override
    public boolean isAppAuthentication() {
        return config.getBoolean("app-authentication", false);
    }
    
    @Override
    public String getRestApiHost() {
        return config.getString("rest-api.host", "0.0.0.0");
    }
    
    @Override
    public int getRestApiPort() {
        return config.getInt("rest-api.port", 25566);
    }
    
    @Override
    public String getServerId() {
        return config.getString("server-id", "");
    }
    
    @Override
    public void setServerId(String serverId) {
        config.set("server-id", serverId);
        saveConfig();
    }
    
    @Override
    public String getSecretKey() {
        return config.getString("secret-key", "");
    }
    
    @Override
    public void setSecretKey(String secretKey) {
        config.set("secret-key", secretKey);
        saveConfig();
    }
    
    @Override
    public boolean isProxyMode() {
        return config.getBoolean("proxy-mode", false);
    }
    
    @Override
    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    @Override
    public boolean isDebug() {
        return config.getBoolean("debug", false);
    }
}

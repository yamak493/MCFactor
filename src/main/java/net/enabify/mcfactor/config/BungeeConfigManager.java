package net.enabify.mcfactor.config;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class BungeeConfigManager implements MCFactorConfig {
    private final File configFile;
    private Configuration config;
    
    private static final int CONFIG_VERSION = 1;
    
    public BungeeConfigManager(File dataFolder) {
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
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
            
            // Check config version and update if needed
            if (config.getInt("config-version", 0) != CONFIG_VERSION) {
                updateConfig();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void createDefaultConfig() {
        try {
            // Try to copy from resources first
            InputStream resourceStream = BungeeConfigManager.class.getResourceAsStream("/config.yml");
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
        try {
            configFile.createNewFile();
            config = new Configuration();
            
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
            
            // Proxy mode (always false for BungeeCord as it IS the proxy)
            config.set("proxy-mode", false);
            
            // Server credentials (auto-generated on first run)
            config.set("server-id", "");
            config.set("secret-key", "");
            
            // Debugging (prints additional debug information when true)
            config.set("debug", false);
            
            saveConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void updateConfig() {
        config.set("config-version", CONFIG_VERSION);
        saveConfig();
    }
    
    @Override
    public void saveConfig() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public Configuration getConfig() {
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
        // BungeeCord is always the proxy, never in proxy mode
        return false;
    }
    
    @Override
    public void reload() {
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isDebug() {
        return config.getBoolean("debug", false);
    }
}

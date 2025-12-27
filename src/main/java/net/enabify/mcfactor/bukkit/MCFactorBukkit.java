package net.enabify.mcfactor.bukkit;

import net.enabify.mcfactor.MCFactorPlugin;
import net.enabify.mcfactor.api.ApiClient;
import net.enabify.mcfactor.auth.AuthenticationManager;
import net.enabify.mcfactor.auth.RestApiServer;
import net.enabify.mcfactor.bukkit.command.BukkitTwoFactorCommand;
import net.enabify.mcfactor.bukkit.listener.PlayerListener;
import net.enabify.mcfactor.config.ConfigManager;
import net.enabify.mcfactor.language.LanguageManager;
import net.enabify.mcfactor.storage.LinkStorage;
import com.google.gson.JsonObject;
import org.bukkit.plugin.java.JavaPlugin;

public class MCFactorBukkit extends JavaPlugin implements MCFactorPlugin {
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private LinkStorage linkStorage;
    private AuthenticationManager authManager;
    private RestApiServer restApiServer;
    
    @Override
    public void onEnable() {
        // Initialize managers
        configManager = new ConfigManager(getDataFolder());
        languageManager = new LanguageManager();
        languageManager.setLanguage(configManager.getLanguage());
        linkStorage = new LinkStorage(getDataFolder());
        
        // Check if plugin is enabled
        if (!configManager.isEnabled()) {
            getLogger().warning("MCFactor is disabled in config.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Register server if needed
        if (configManager.getServerId().isEmpty() || configManager.getSecretKey().isEmpty()) {
            registerServer();
        }
        
        // Initialize authentication manager
        authManager = new AuthenticationManager(
            linkStorage,
            configManager.getServerId(),
            configManager.getSecretKey()
        );
        
        // Start REST API server if app authentication is enabled
        if (configManager.isAppAuthentication()) {
            startRestApiServer();
            updateServerInfo();
        }
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        // Register commands
        getCommand("2fa").setExecutor(new BukkitTwoFactorCommand(this));
        
        // Start cleanup task (compatible with both Bukkit and Folia)
        startCleanupTask();
        
        getLogger().info("MCFactor has been enabled!");
    }
    
    private void startCleanupTask() {
        // Try Folia API first
        try {
            Class<?> asyncSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
            Object asyncScheduler = getServer().getClass().getMethod("getAsyncScheduler").invoke(getServer());
            java.lang.reflect.Method runNowMethod = asyncSchedulerClass.getMethod("runNow", 
                org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class);
            
            runNowMethod.invoke(asyncScheduler, this, (java.util.function.Consumer<Object>) task -> {
                try {
                    while (!getServer().getPluginManager().isPluginEnabled(this)) {
                        Thread.sleep(1000);
                    }
                    while (getServer().getPluginManager().isPluginEnabled(this)) {
                        Thread.sleep(60000); // Every minute
                        authManager.cleanupOldAuthentications();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        } catch (Exception e) {
            // Fallback to traditional Bukkit scheduler for non-Folia servers
            getServer().getScheduler().runTaskTimer(this, () -> {
                authManager.cleanupOldAuthentications();
            }, 20L * 60, 20L * 60);
        }
    }
    
    @Override
    public void onDisable() {
        // Stop REST API server
        if (restApiServer != null) {
            restApiServer.stop();
        }
        
        getLogger().info("MCFactor has been disabled!");
    }
    
    private void registerServer() {
        getLogger().info("Registering server with backend API...");
        
        try {
            String type = configManager.isAppAuthentication() ? "app" : "code";
            String host = configManager.getRestApiHost();
            int port = configManager.getRestApiPort();
            
            JsonObject response = ApiClient.registerServer(host, port, type);
            
            if (response.has("serverid") && response.has("secretkey")) {
                String serverId = response.get("serverid").getAsString();
                String secretKey = response.get("secretkey").getAsString();
                
                configManager.setServerId(serverId);
                configManager.setSecretKey(secretKey);
                
                getLogger().info("Server registered successfully!");
                getLogger().info("Server ID: " + serverId);
            } else {
                getLogger().severe("Failed to register server: Invalid response");
                getServer().getPluginManager().disablePlugin(this);
            }
        } catch (Exception e) {
            getLogger().severe("Failed to register server: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    private void updateServerInfo() {
        try {
            String type = configManager.isAppAuthentication() ? "app" : "code";
            String host = configManager.getRestApiHost();
            int port = configManager.getRestApiPort();
            
            JsonObject response = ApiClient.updateServer(
                configManager.getServerId(),
                configManager.getSecretKey(),
                host,
                port,
                type
            );
            
            // Check if response indicates success
            // The API may return {"true": true} or wrapped response {"response": true}
            boolean isSuccess = false;
            if (response.has("true") && response.get("true").getAsBoolean()) {
                isSuccess = true;
            } else if (response.has("response") && response.get("response").isJsonPrimitive()) {
                try {
                    isSuccess = response.get("response").getAsBoolean();
                } catch (Exception e) {
                    // If it's not a boolean, treat any response as success
                    isSuccess = true;
                }
            } else if (response.size() > 0) {
                // If response has any content, treat as success
                isSuccess = true;
            }
            
            if (isSuccess) {
                getLogger().info("Server information updated successfully!");
            } else {
                getLogger().severe("Failed to update server information");
                getServer().getPluginManager().disablePlugin(this);
            }
        } catch (Exception e) {
            getLogger().severe("Failed to update server information: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    private void startRestApiServer() {
        try {
            restApiServer = new BukkitRestApiServer(
                configManager.getRestApiPort(),
                authManager,
                linkStorage,
                configManager.getServerId(),
                configManager.getSecretKey(),
                this
            );
            restApiServer.start();
            getLogger().info("REST API server started on port " + configManager.getRestApiPort());
            
            // Test the REST API endpoint
            testRestApiEndpoint();
        } catch (Exception e) {
            getLogger().severe("Failed to start REST API server: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    private void testRestApiEndpoint() {
        // Wait a moment for the server to fully start
        getServer().getScheduler().runTaskLater(this, () -> {
            String host = configManager.getRestApiHost();
            int port = configManager.getRestApiPort();
            
            getLogger().info("Testing REST API endpoint at " + host + ":" + port + "...");
            
            boolean isWorking = ApiClient.testRestApiEndpoint(host, port);
            
            if (isWorking) {
                getLogger().info("REST API endpoint test successful!");
            } else {
                getLogger().severe("REST API endpoint test failed!");
                getLogger().severe("The API server may not be accessible from " + host + ":" + port);
                getLogger().severe("Please check your config.yml settings and firewall rules.");
            }
        }, 20L); // Wait 1 second (20 ticks)
    }
    
    @Override
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    @Override
    public LanguageManager getLanguageManager() {
        return languageManager;
    }
    
    @Override
    public LinkStorage getLinkStorage() {
        return linkStorage;
    }
    
    @Override
    public AuthenticationManager getAuthManager() {
        return authManager;
    }
    
    @Override
    public void log(String message) {
        getLogger().info(message);
    }
    
    @Override
    public void logError(String message, Exception e) {
        getLogger().severe(message);
        if (e != null) {
            e.printStackTrace();
        }
    }
}

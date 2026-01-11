package net.enabify.mcfactor.bukkit;

import net.enabify.mcfactor.MCFactorPlugin;
import net.enabify.mcfactor.api.ApiClient;
import net.enabify.mcfactor.auth.AuthenticationManager;
import net.enabify.mcfactor.auth.RestApiServer;
import net.enabify.mcfactor.bukkit.command.BukkitTwoFactorCommand;
import net.enabify.mcfactor.bukkit.listener.PlayerListener;
import net.enabify.mcfactor.bukkit.freeze.PlayerFreezeManager;
import net.enabify.mcfactor.bukkit.config.BukkitConfigManager;
import net.enabify.mcfactor.config.MCFactorConfig;
import net.enabify.mcfactor.language.LanguageManager;
import net.enabify.mcfactor.storage.LinkStorage;
import net.enabify.mcfactor.messaging.MessagingConstants;
import net.enabify.mcfactor.messaging.AuthStateMessage;
import net.enabify.mcfactor.util.FoliaScheduler;
import com.google.gson.JsonObject;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class MCFactorBukkit extends JavaPlugin implements MCFactorPlugin, PluginMessageListener {
    private MCFactorConfig configManager;
    private LanguageManager languageManager;
    private LinkStorage linkStorage;
    private AuthenticationManager authManager;
    private RestApiServer restApiServer;
    private PlayerFreezeManager freezeManager;
    
    @Override
    public void onEnable() {
        // Initialize managers
        configManager = new BukkitConfigManager(this, getDataFolder());
        languageManager = new LanguageManager();
        languageManager.setLanguage(configManager.getLanguage());
        linkStorage = new LinkStorage(getDataFolder());
        freezeManager = new PlayerFreezeManager();
        
        // Check if plugin is enabled
        if (!configManager.isEnabled()) {
            getLogger().warning("MCFactor is disabled in config.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Determine operation mode based on proxy-mode flag
        boolean isProxyMode = configManager.isProxyMode();
        
        if (isProxyMode) {
            // Mode 3: Downstream server behind proxy - only handle freeze control
            logDebug("Running in Proxy Mode (downstream server) - freeze control only");
            
            // Register Plugin Messaging Channel to receive auth state from proxy
            getServer().getMessenger().registerIncomingPluginChannel(this, MessagingConstants.CHANNEL_NAME, this);
            
            // Register listeners for freeze control
            getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
            
            log("MCFactor has been enabled in Proxy Mode!");
            return;
        }
        
        // Mode 1: Standalone server - full authentication and freeze control
        logDebug("Running in Standalone Mode - full authentication");
        
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
        FoliaScheduler.runGlobalTaskTimer(this, () -> {
            authManager.cleanupOldAuthentications();
        }, 20L * 60, 20L * 60);
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
        logDebug("Registering server with backend API...");
        
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
                
                log("Server registered successfully!");
                logDebug("Server ID: " + serverId);
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
                logDebug("Server information updated successfully!");
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
            logDebug("REST API server started on port " + configManager.getRestApiPort());
            
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
        FoliaScheduler.runGlobalTaskLater(this, () -> {
            String host = configManager.getRestApiHost();
            int port = configManager.getRestApiPort();
            
            logDebug("Testing REST API endpoint at " + host + ":" + port + "...");
            
            boolean isWorking = ApiClient.testRestApiEndpoint(host, port);
            
            if (isWorking) {
                logDebug("REST API endpoint test successful!");
            } else {
                getLogger().severe("REST API endpoint test failed!");
                getLogger().severe("The API server may not be accessible from " + host + ":" + port);
                getLogger().severe("Please check your config.yml settings and firewall rules.");
            }
        }, 20L); // Wait 1 second (20 ticks)
    }

    @Override
    public MCFactorConfig getConfigManager() {
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
    public void logDebug(String message) {
        // Only output when debug flag is enabled
        if (configManager != null && configManager.isDebug()) {
            getLogger().info(message);
        }
    }
    
    @Override
    public void logError(String message, Exception e) {
        getLogger().severe(message);
        if (e != null) {
            e.printStackTrace();
        }
    }
    
    public PlayerFreezeManager getFreezeManager() {
        return freezeManager;
    }
    
    /**
     * Handle Plugin Messages from BungeeCord proxy
     * Receives authentication state updates and applies freeze/unfreeze accordingly
     */
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(MessagingConstants.CHANNEL_NAME)) {
            return;
        }
        
        try {
            AuthStateMessage authState = AuthStateMessage.fromBytes(message);
            
            logDebug("Received auth state for player " + authState.getPlayerUuid() + 
                           ": " + (authState.isAuthenticated() ? "AUTHED" : "UNAUTHED"));
            
            // Find the player on this server
            Player targetPlayer = getServer().getPlayer(authState.getPlayerUuid());
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                getLogger().warning("Received auth state for offline player: " + authState.getPlayerUuid());
                return;
            }
            
            // Apply freeze/unfreeze based on authentication state
            if (authState.isAuthenticated()) {
                // Player is authenticated - unfreeze
                if (freezeManager.isFrozen(targetPlayer.getUniqueId())) {
                    FoliaScheduler.runTask(this, targetPlayer, () -> {
                        freezeManager.unfreezePlayer(targetPlayer);
                        logDebug("Unfroze player " + targetPlayer.getName());
                    });
                }
            } else {
                // Player is not authenticated - freeze
                if (!freezeManager.isFrozen(targetPlayer.getUniqueId())) {
                    FoliaScheduler.runTask(this, targetPlayer, () -> {
                        freezeManager.freezePlayer(targetPlayer);
                        logDebug("Froze player " + targetPlayer.getName());
                    });
                }
            }
        } catch (Exception e) {
            getLogger().severe("Failed to process auth state message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

package net.enabify.mcfactor.bungee;

import net.enabify.mcfactor.MCFactorPlugin;
import net.enabify.mcfactor.api.ApiClient;
import net.enabify.mcfactor.auth.AuthenticationManager;
import net.enabify.mcfactor.auth.RestApiServer;
import net.enabify.mcfactor.bungee.command.BungeeTwoFactorCommand;
import net.enabify.mcfactor.bungee.listener.BungeePlayerListener;
import net.enabify.mcfactor.config.BungeeConfigManager;
import net.enabify.mcfactor.language.LanguageManager;
import net.enabify.mcfactor.storage.LinkStorage;
import net.enabify.mcfactor.messaging.MessagingConstants;
import net.enabify.mcfactor.messaging.AuthStateMessage;
import com.google.gson.JsonObject;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.concurrent.TimeUnit;

public class MCFactorBungee extends Plugin implements MCFactorPlugin {
    private BungeeConfigManager configManager;
    private LanguageManager languageManager;
    private LinkStorage linkStorage;
    private AuthenticationManager authManager;
    private RestApiServer restApiServer;
    
    @Override
    public void onEnable() {
        // Initialize managers
        configManager = new BungeeConfigManager(getDataFolder());
        languageManager = new LanguageManager();
        languageManager.setLanguage(configManager.getLanguage());
        linkStorage = new LinkStorage(getDataFolder());
        
        // Check if plugin is enabled
        if (!configManager.isEnabled()) {
            getLogger().warning("MCFactor is disabled in config.yml");
            return;
        }
        
        // Register Plugin Messaging Channel
        getProxy().registerChannel(MessagingConstants.CHANNEL_NAME);
        
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
        getProxy().getPluginManager().registerListener(this, new BungeePlayerListener(this));
        
        // Register commands
        getProxy().getPluginManager().registerCommand(this, new BungeeTwoFactorCommand(this));
        
        // Start cleanup task
        getProxy().getScheduler().schedule(this, () -> {
            authManager.cleanupOldAuthentications();
        }, 1, 1, TimeUnit.MINUTES);
        
        getLogger().info("MCFactor has been enabled!");
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
            }
        } catch (Exception e) {
            getLogger().severe("Failed to register server: " + e.getMessage());
            e.printStackTrace();
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
            boolean isSuccess = false;
            if (response.has("true") && response.get("true").getAsBoolean()) {
                isSuccess = true;
            } else if (response.has("response") && response.get("response").isJsonPrimitive()) {
                try {
                    isSuccess = response.get("response").getAsBoolean();
                } catch (Exception e) {
                    isSuccess = true;
                }
            } else if (response.size() > 0) {
                isSuccess = true;
            }
            
            if (isSuccess) {
                logDebug("Server information updated successfully!");
            } else {
                getLogger().severe("Failed to update server information");
            }
        } catch (Exception e) {
            getLogger().severe("Failed to update server information: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void startRestApiServer() {
        try {
            restApiServer = new BungeeRestApiServer(
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
        }
    }
    
    private void testRestApiEndpoint() {
        // Only test if debug is enabled
        if (!configManager.isDebug()) {
            return;
        }
        
        // Wait a moment for the server to fully start
        getProxy().getScheduler().schedule(this, () -> {
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
        }, 1, TimeUnit.SECONDS);
    }
    
    @Override
    public BungeeConfigManager getConfigManager() {
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
    
    /**
     * Send authentication state to all downstream servers
     * This notifies servers whether a player is authenticated or not
     */
    public void sendAuthState(ProxiedPlayer player, boolean authenticated) {
        net.md_5.bungee.api.connection.Server server = player.getServer();
        if (server == null) {
            return;
        }
        
        try {
            // Create auth state message
            AuthStateMessage message = new AuthStateMessage(player.getUniqueId(), authenticated);
            
            // Send the plugin message
            server.sendData(MessagingConstants.CHANNEL_NAME, message.toBytes());
            
            logDebug("Sent auth state to server for player " + player.getName() +
                           ": " + (authenticated ? "AUTHED" : "UNAUTHED"));
        } catch (Exception e) {
            getLogger().warning("Failed to send auth state: " + e.getMessage());
        }
    }
}

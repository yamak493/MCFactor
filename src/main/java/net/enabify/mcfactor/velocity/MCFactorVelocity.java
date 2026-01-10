package net.enabify.mcfactor.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.enabify.mcfactor.MCFactorPlugin;
import net.enabify.mcfactor.api.ApiClient;
import net.enabify.mcfactor.auth.AuthenticationManager;
import net.enabify.mcfactor.config.ConfigManager;
import net.enabify.mcfactor.language.LanguageManager;
import net.enabify.mcfactor.storage.LinkStorage;
import net.enabify.mcfactor.velocity.listener.VelocityPlayerListener;
import net.enabify.mcfactor.messaging.MessagingConstants;
import net.enabify.mcfactor.messaging.AuthStateMessage;
import com.google.gson.JsonObject;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.Optional;

@Plugin(
    id = "mcfactor",
    name = "MCFactor",
    version = "1.0-SNAPSHOT",
    description = "Two-factor authentication plugin for Minecraft",
    authors = {"Enabify"}
)
public class MCFactorVelocity implements MCFactorPlugin {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private LinkStorage linkStorage;
    private AuthenticationManager authManager;
    private VelocityRestApiServer restApiServer;
    
    @Inject
    public MCFactorVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }
    
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Initialize managers
        configManager = new ConfigManager(dataDirectory.toFile());
        languageManager = new LanguageManager();
        languageManager.setLanguage(configManager.getLanguage());
        linkStorage = new LinkStorage(dataDirectory.toFile());
        
        // Check if plugin is enabled
        if (!configManager.isEnabled()) {
            logger.warn("MCFactor is disabled in config.yml");
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
        
        // Register Plugin Messaging Channel
        server.getChannelRegistrar().register(
            MinecraftChannelIdentifier.from(MessagingConstants.CHANNEL_NAME)
        );
        
        // Start REST API server if app authentication is enabled
        if (configManager.isAppAuthentication()) {
            startRestApiServer();
            updateServerInfo();
        }
        
        // Register listeners
        server.getEventManager().register(this, new VelocityPlayerListener(this));
        
        // Register commands
        // TODO: Implement Velocity commands
        
        // Start cleanup task
        server.getScheduler()
            .buildTask(this, () -> authManager.cleanupOldAuthentications())
            .repeat(1, TimeUnit.MINUTES)
            .schedule();
        
        logger.info("MCFactor has been enabled!");
    }
    
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        // Stop REST API server
        if (restApiServer != null) {
            restApiServer.stop();
        }
        
        logger.info("MCFactor has been disabled!");
    }
    
    private void registerServer() {
        logger.info("Registering server with backend API...");
        
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
                configManager.saveConfig();
                
                logger.info("Server registered successfully!");
                logger.info("Server ID: " + serverId);
            } else {
                logger.error("Failed to register server: Invalid response");
            }
        } catch (Exception e) {
            logger.error("Failed to register server", e);
        }
    }
    
    private void startRestApiServer() {
        try {
            restApiServer = new VelocityRestApiServer(
                configManager.getRestApiPort(),
                authManager,
                linkStorage,
                configManager.getServerId(),
                configManager.getSecretKey(),
                this
            );
            restApiServer.start();
            logger.info("REST API server started on " + 
                configManager.getRestApiHost() + ":" + configManager.getRestApiPort());
        } catch (Exception e) {
            logger.error("Failed to start REST API server", e);
        }
    }
    
    private void updateServerInfo() {
        try {
            String host = configManager.getRestApiHost();
            int port = configManager.getRestApiPort();
            String type = configManager.isAppAuthentication() ? "app" : "code";
            ApiClient.updateServer(
                configManager.getServerId(),
                configManager.getSecretKey(),
                host,
                port,
                type
            );
            logger.info("Server info updated successfully!");
        } catch (Exception e) {
            logger.error("Failed to update server info", e);
        }
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
    
    public ProxyServer getServer() {
        return server;
    }
    
    public Logger getLogger() {
        return logger;
    }
    
    @Override
    public File getDataFolder() {
        return dataDirectory.toFile();
    }
    
    @Override
    public void log(String message) {
        logger.info(message);
    }
    
    @Override
    public void logError(String message, Exception e) {
        logger.error(message, e);
    }
    
    /**
     * Send authentication state to all downstream servers
     * This notifies servers whether a player is authenticated or not
     */
    public void sendAuthState(Player player, boolean authenticated) {
        Optional<ServerConnection> serverConnection = player.getCurrentServer();
        if (!serverConnection.isPresent()) {
            return;
        }
        
        try {
            // Create auth state message
            AuthStateMessage message = new AuthStateMessage(player.getUniqueId(), authenticated);
            
            // Send the plugin message
            serverConnection.get().sendPluginMessage(
                MinecraftChannelIdentifier.from(MessagingConstants.CHANNEL_NAME),
                message.toBytes()
            );
            
            logger.info("Sent auth state to server for player " + player.getUsername() +
                       ": " + (authenticated ? "AUTHED" : "UNAUTHED"));
        } catch (Exception e) {
            logger.warn("Failed to send auth state: " + e.getMessage());
        }
    }
}

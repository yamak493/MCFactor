package net.enabify.mcfactor.command;

import net.enabify.mcfactor.MCFactorPlugin;
import net.enabify.mcfactor.api.ApiClient;
import net.enabify.mcfactor.auth.AuthenticationManager;
import net.enabify.mcfactor.language.LanguageManager;
import net.enabify.mcfactor.storage.LinkStorage;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class TwoFactorCommand {
    protected final MCFactorPlugin plugin;
    protected final AuthenticationManager authManager;
    protected final LinkStorage linkStorage;
    protected final LanguageManager languageManager;
    
    public TwoFactorCommand(MCFactorPlugin plugin) {
        this.plugin = plugin;
        this.authManager = plugin.getAuthManager();
        this.linkStorage = plugin.getLinkStorage();
        this.languageManager = plugin.getLanguageManager();
    }
    
    /**
     * Handle /2fa <appkey> - Register app key
     */
    protected void handleRegister(UUID playerUuid, String playerName, String appKey) {
        // Validate app key format (6 characters)
        if (appKey.length() != 6) {
            sendMessage(playerUuid, languageManager.getMessage("command.register.invalid-appkey"));
            return;
        }
        
        // Check if already linked
        if (linkStorage.hasLink(playerUuid)) {
            sendMessage(playerUuid, languageManager.getMessage("command.register.already-linked"));
            return;
        }
        
        // Request pass key from API
        try {
            JsonObject response = ApiClient.tempRegisterPlugin(
                plugin.getConfigManager().getServerId(),
                plugin.getConfigManager().getSecretKey(),
                appKey
            );
            
            if (response.has("passkey")) {
                String passKey = response.get("passkey").getAsString();
                authManager.startRegistration(playerUuid, playerName, appKey, passKey);
                
                Map<String, String> replacements = new HashMap<>();
                sendMessage(playerUuid, languageManager.getMessage("command.register.enter-token", replacements));
            } else {
                sendMessage(playerUuid, languageManager.getMessage("command.register.api-error"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(playerUuid, languageManager.getMessage("command.register.api-error"));
        }
    }
    
    /**
     * Handle /2fa - Unlink
     */
    protected void handleUnlink(UUID playerUuid) {
        if (!linkStorage.hasLink(playerUuid)) {
            sendMessage(playerUuid, languageManager.getMessage("command.unlink.not-linked"));
            return;
        }
        
        linkStorage.removeLink(playerUuid);
        sendMessage(playerUuid, languageManager.getMessage("command.unlink.success"));
    }
    
    /**
     * Handle /2fa help
     */
    protected void handleHelp(UUID playerUuid, boolean isOp) {
        sendMessage(playerUuid, languageManager.getMessage("command.help.header"));
        sendMessage(playerUuid, languageManager.getMessage("command.help.register"));
        sendMessage(playerUuid, languageManager.getMessage("command.help.unlink"));
        sendMessage(playerUuid, languageManager.getMessage("command.help.help"));
        
        if (isOp) {
            sendMessage(playerUuid, languageManager.getMessage("command.help.disconnect"));
        }
    }
    
    /**
     * Handle /2fa disconnect <player> - OP only
     */
    protected void handleDisconnect(UUID senderUuid, String targetPlayerName, boolean isOp) {
        if (!isOp) {
            sendMessage(senderUuid, languageManager.getMessage("command.no-permission"));
            return;
        }
        
        linkStorage.removeLinkByName(targetPlayerName);
        
        Map<String, String> replacements = new HashMap<>();
        replacements.put("player", targetPlayerName);
        sendMessage(senderUuid, languageManager.getMessage("command.disconnect.success", replacements));
    }
    
    /**
     * Handle token verification during registration
     */
    protected void handleTokenVerification(UUID playerUuid, String token) {
        if (!authManager.hasPendingRegistration(playerUuid)) {
            sendMessage(playerUuid, languageManager.getMessage("command.register.no-pending"));
            return;
        }
        
        if (authManager.completeRegistration(playerUuid, token)) {
            sendMessage(playerUuid, languageManager.getMessage("command.register.success"));
        } else {
            sendMessage(playerUuid, languageManager.getMessage("command.register.invalid-token"));
            authManager.cancelRegistration(playerUuid);
        }
    }
    
    protected abstract void sendMessage(UUID playerUuid, String message);
}

package net.enabify.mcfactor.bungee.listener;

import net.enabify.mcfactor.bungee.MCFactorBungee;
import net.enabify.mcfactor.storage.LinkStorage;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BungeePlayerListener implements Listener {
    private final MCFactorBungee plugin;
    
    public BungeePlayerListener(MCFactorBungee plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPostLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        
        // Check if player has linked account
        if (!plugin.getLinkStorage().hasLink(player.getUniqueId())) {
            return;
        }
        
        // Code authentication mode
        if (plugin.getConfigManager().isCodeAuthentication()) {
            plugin.getAuthManager().setAwaitingCodeInput(player.getUniqueId(), true);
            
            // Send message
            player.sendMessage(new TextComponent(
                plugin.getLanguageManager().getMessage("auth.enter-code")
            ));
        }
        
        // App authentication mode
        if (plugin.getConfigManager().isAppAuthentication()) {
            if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
                plugin.getProxy().getScheduler().schedule(plugin, () -> {
                    if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
                        player.disconnect(new TextComponent(
                            plugin.getLanguageManager().getMessage("auth.not-authenticated")
                        ));
                    }
                }, 1, TimeUnit.SECONDS);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer)) {
            return;
        }
        
        ProxiedPlayer player = (ProxiedPlayer) event.getSender();
        
        // Check if player is in registration process
        if (plugin.getAuthManager().hasPendingRegistration(player.getUniqueId())) {
            event.setCancelled(true);
            
            String token = event.getMessage().trim();
            plugin.getLogger().info("[MCFactor] Player " + player.getName() + " attempting token registration");
            plugin.getLogger().info("[MCFactor] Input token: " + token);
            
            // Get pending registration details for debugging
            try {
                java.lang.reflect.Field field = plugin.getAuthManager().getClass().getDeclaredField("pendingRegistrations");
                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.Map<java.util.UUID, Object> pendingRegs = (java.util.Map<java.util.UUID, Object>) field.get(plugin.getAuthManager());
                Object pendingReg = pendingRegs.get(player.getUniqueId());
                
                if (pendingReg != null) {
                    java.lang.reflect.Field passKeyField = pendingReg.getClass().getDeclaredField("passKey");
                    passKeyField.setAccessible(true);
                    String passKey = (String) passKeyField.get(pendingReg);
                    
                    String serverId = plugin.getConfigManager().getServerId();
                    plugin.getLogger().info("[MCFactor] Server ID: " + serverId);
                    plugin.getLogger().info("[MCFactor] Pass Key: " + passKey);
                    
                    // Use debug method to see all tokens in all intervals
                    net.enabify.mcfactor.util.HashUtil.verifyAuthTokenDebug(serverId, passKey, token, plugin.getLogger());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[MCFactor] Debug error: " + e.getMessage());
            }
            
            if (plugin.getAuthManager().completeRegistration(player.getUniqueId(), token)) {
                player.sendMessage(new TextComponent(
                    plugin.getLanguageManager().getMessage("command.register.success")
                ));
            } else {
                player.sendMessage(new TextComponent(
                    plugin.getLanguageManager().getMessage("command.register.invalid-token")
                ));
                plugin.getAuthManager().cancelRegistration(player.getUniqueId());
            }
            return;
        }
        
        // Check if player is awaiting code input
        if (plugin.getAuthManager().isAwaitingCodeInput(player.getUniqueId())) {
            event.setCancelled(true);
            
            String code = event.getMessage().trim();
            plugin.getLogger().info("[MCFactor] Player " + player.getName() + " attempting code authentication");
            plugin.getLogger().info("[MCFactor] Input code: " + code);
            
            // Debug: show expected codes in all time intervals
            try {
                String serverId = plugin.getConfigManager().getServerId();
                
                LinkStorage.PlayerLink link = plugin.getLinkStorage().getLink(player.getUniqueId());
                if (link != null) {
                    String passKey = link.getPassKey();
                    
                    plugin.getLogger().info("[MCFactor Code Debug] Server ID: " + serverId);
                    plugin.getLogger().info("[MCFactor Code Debug] Pass Key: " + passKey);
                    
                    long currentTimestamp = net.enabify.mcfactor.util.HashUtil.getTimestamp();
                    plugin.getLogger().info("[MCFactor Code Debug] Current Timestamp: " + currentTimestamp);
                    
                    // Check all 3 intervals
                    for (long offset = -180; offset <= 180; offset += 180) {
                        long testTimestamp = currentTimestamp + offset;
                        String expectedCode = net.enabify.mcfactor.util.HashUtil.generateToken(serverId + testTimestamp + passKey);
                        plugin.getLogger().info("[MCFactor Code Debug] Timestamp " + testTimestamp + " -> code: " + expectedCode);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[MCFactor] Debug error: " + e.getMessage());
            }
            
            if (plugin.getAuthManager().verifyCode(player.getUniqueId(), code)) {
                plugin.getAuthManager().setAwaitingCodeInput(player.getUniqueId(), false);
                player.sendMessage(new TextComponent(
                    plugin.getLanguageManager().getMessage("auth.success")
                ));
            } else {
                player.disconnect(new TextComponent(
                    plugin.getLanguageManager().getMessage("auth.invalid-code")
                ));
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(net.md_5.bungee.api.event.PlayerDisconnectEvent event) {
        plugin.getAuthManager().setAwaitingCodeInput(event.getPlayer().getUniqueId(), false);
    }
}

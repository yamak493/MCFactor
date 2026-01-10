package net.enabify.mcfactor.bukkit.listener;

import net.enabify.mcfactor.bukkit.MCFactorBukkit;
import net.enabify.mcfactor.storage.LinkStorage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PlayerListener implements Listener {
    private final MCFactorBukkit plugin;
    
    public PlayerListener(MCFactorBukkit plugin) {
        this.plugin = plugin;
        startSyncTaskProcessor();
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // If in proxy mode, we don't manage authentication here
        if (plugin.getConfigManager().isProxyMode()) {
            // In proxy mode, freeze state will be set via Plugin Message from proxy
            return;
        }
        
        // Check if player has linked account
        if (!plugin.getLinkStorage().hasLink(player.getUniqueId())) {
            return;
        }
        
        // Code authentication mode
        if (plugin.getConfigManager().isCodeAuthentication()) {
            plugin.getAuthManager().setAwaitingCodeInput(player.getUniqueId(), true);
            
            // Freeze player using FreezeManager
            plugin.getFreezeManager().freezePlayer(player);
            
            // Send message
            player.sendMessage(plugin.getLanguageManager().getMessage("auth.enter-code"));
        }
        
        // App authentication mode
        if (plugin.getConfigManager().isAppAuthentication()) {
            if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
                        player.kickPlayer(plugin.getLanguageManager().getMessage("auth.not-authenticated"));
                    }
                }, 20L); // Wait 1 second
            }
        }
    }
    
    /**
     * Cancel movement for frozen players
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is frozen
        if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
            // Get freeze location
            org.bukkit.Location freezeLoc = plugin.getFreezeManager().getFreezeLocation(player.getUniqueId());
            if (freezeLoc != null) {
                // Cancel movement if player moved from freeze location
                if (!isSameBlock(event.getFrom(), freezeLoc)) {
                    event.setCancelled(true);
                    player.teleport(freezeLoc);
                }
            }
        }
    }
    
    /**
     * Check if two locations are in the same block
     */
    private boolean isSameBlock(org.bukkit.Location loc1, org.bukkit.Location loc2) {
        return loc1.getBlockX() == loc2.getBlockX() &&
               loc1.getBlockY() == loc2.getBlockY() &&
               loc1.getBlockZ() == loc2.getBlockZ();
    }
    
    /**
     * Cancel commands for frozen players (safety measure)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        
        // Allow 2fa command even when frozen
        if (event.getMessage().toLowerCase().startsWith("/2fa") || 
            event.getMessage().toLowerCase().startsWith("/2f")) {
            return;
        }
        
        // Cancel other commands if player is frozen
        if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getLanguageManager().getMessage("auth.enter-code"));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is frozen (this is our UNAUTHED state, used in both standalone and proxy mode)
        if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
            
            // If we are NOT in proxy mode, handle the code input here locally
            if (!plugin.getConfigManager().isProxyMode()) {
                handleLocalChatAuth(event);
            }
            return;
        }
    }
    
    private void handleLocalChatAuth(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is in registration process
        if (plugin.getAuthManager().hasPendingRegistration(player.getUniqueId())) {
            String token = event.getMessage().trim();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
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
                    player.sendMessage(plugin.getLanguageManager().getMessage("command.register.success"));
                } else {
                    player.sendMessage(plugin.getLanguageManager().getMessage("command.register.invalid-token"));
                    plugin.getAuthManager().cancelRegistration(player.getUniqueId());
                }
            });
            return;
        }
        
        // Check if player is awaiting code input
        if (plugin.getAuthManager().isAwaitingCodeInput(player.getUniqueId())) {
            event.setCancelled(true);
            
            String code = event.getMessage().trim();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
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
                    
                    // Unfreeze player using FreezeManager
                    plugin.getFreezeManager().unfreezePlayer(player);
                    
                    player.sendMessage(plugin.getLanguageManager().getMessage("auth.success"));
                } else {
                    player.kickPlayer(plugin.getLanguageManager().getMessage("auth.invalid-code"));
                }
            });
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (plugin.getAuthManager() != null) {
            plugin.getAuthManager().setAwaitingCodeInput(event.getPlayer().getUniqueId(), false);
        }
        
        // Cleanup freeze data
        plugin.getFreezeManager().cleanup(event.getPlayer().getUniqueId());
    }

    private void startSyncTaskProcessor() {
        // No longer needed as we use PlayerQuitEvent for cleanup
    }
}

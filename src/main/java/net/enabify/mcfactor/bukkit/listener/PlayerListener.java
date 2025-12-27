package net.enabify.mcfactor.bukkit.listener;

import net.enabify.mcfactor.bukkit.MCFactorBukkit;
import net.enabify.mcfactor.storage.LinkStorage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PlayerListener implements Listener {
    private final MCFactorBukkit plugin;
    private final Map<String, Boolean> awaitingCodeInput;
    
    public PlayerListener(MCFactorBukkit plugin) {
        this.plugin = plugin;
        this.awaitingCodeInput = new HashMap<>();
        startSyncTaskProcessor();
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Check if player has linked account
        if (!plugin.getLinkStorage().hasLink(player.getUniqueId())) {
            return;
        }
        
        // Code authentication mode
        if (plugin.getConfigManager().isCodeAuthentication()) {
            awaitingCodeInput.put(player.getName(), true);
            
            // Freeze player
            player.setWalkSpeed(0);
            player.setFlySpeed(0);
            
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
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is in registration process
        if (plugin.getAuthManager().hasPendingRegistration(player.getUniqueId())) {
            event.setCancelled(true);
            
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
        if (awaitingCodeInput.containsKey(player.getName())) {
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
                    awaitingCodeInput.remove(player.getName());
                    
                    // Unfreeze player
                    player.setWalkSpeed(0.2f);
                    player.setFlySpeed(0.1f);
                    
                    player.sendMessage(plugin.getLanguageManager().getMessage("auth.success"));
                } else {
                    player.kickPlayer(plugin.getLanguageManager().getMessage("auth.invalid-code"));
                }
            });
        }
    }

    private void startSyncTaskProcessor() {
        Runnable cleanupTask = () -> {
            awaitingCodeInput.entrySet().removeIf(entry -> {
                Player target = plugin.getServer().getPlayer(entry.getKey());
                return target == null || !target.isOnline();
            });
        };

        try {
            Object asyncScheduler = plugin.getServer().getClass().getMethod("getAsyncScheduler").invoke(plugin.getServer());
            Consumer<Object> runner = ignored -> cleanupTask.run();
            asyncScheduler.getClass()
                .getMethod("runAtFixedRate", org.bukkit.plugin.Plugin.class, Consumer.class, Duration.class, Duration.class)
                .invoke(asyncScheduler, plugin, runner, Duration.ofSeconds(30), Duration.ofSeconds(30));
        } catch (Exception ignored) {
            // Fallback to legacy scheduler when async scheduler is unavailable
            plugin.getServer().getScheduler().runTaskTimer(plugin, cleanupTask, 20L * 30, 20L * 30);
        }
    }
}

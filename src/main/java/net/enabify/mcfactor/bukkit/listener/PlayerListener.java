package net.enabify.mcfactor.bukkit.listener;

import net.enabify.mcfactor.bukkit.MCFactorBukkit;
import net.enabify.mcfactor.storage.LinkStorage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;

public class PlayerListener implements Listener {
    private final MCFactorBukkit plugin;
    private final Map<String, Boolean> awaitingCodeInput;
    
    public PlayerListener(MCFactorBukkit plugin) {
        this.plugin = plugin;
        this.awaitingCodeInput = new HashMap<>();
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
}

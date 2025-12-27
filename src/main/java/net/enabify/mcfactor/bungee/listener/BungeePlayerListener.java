package net.enabify.mcfactor.bungee.listener;

import net.enabify.mcfactor.bungee.MCFactorBungee;
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
    private final Map<String, Boolean> awaitingCodeInput;
    
    public BungeePlayerListener(MCFactorBungee plugin) {
        this.plugin = plugin;
        this.awaitingCodeInput = new HashMap<>();
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
            awaitingCodeInput.put(player.getName(), true);
            
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
        if (awaitingCodeInput.containsKey(player.getName())) {
            event.setCancelled(true);
            
            String code = event.getMessage().trim();
            if (plugin.getAuthManager().verifyCode(player.getUniqueId(), code)) {
                awaitingCodeInput.remove(player.getName());
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
}

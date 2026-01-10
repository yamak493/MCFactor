package net.enabify.mcfactor.velocity.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import net.enabify.mcfactor.storage.LinkStorage;
import net.enabify.mcfactor.velocity.MCFactorVelocity;
import net.kyori.adventure.text.Component;

import java.util.concurrent.TimeUnit;

public class VelocityPlayerListener {
    private final MCFactorVelocity plugin;
    
    public VelocityPlayerListener(MCFactorVelocity plugin) {
        this.plugin = plugin;
    }
    
    @Subscribe(order = PostOrder.FIRST)
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        
        // Check if player has linked account
        if (!plugin.getLinkStorage().hasLink(player.getUniqueId())) {
            return;
        }
        
        // Send initial UNAUTHED state
        plugin.getServer().getScheduler()
            .buildTask(plugin, () -> plugin.sendAuthState(player, false))
            .delay(500, TimeUnit.MILLISECONDS)
            .schedule();
        
        // Code authentication mode
        if (plugin.getConfigManager().isCodeAuthentication()) {
            plugin.getAuthManager().setAwaitingCodeInput(player.getUniqueId(), true);
            
            // Send message
            player.sendMessage(plugin.getLanguageManager().getComponent("auth.enter-code"));
        }
        
        // App authentication mode
        if (plugin.getConfigManager().isAppAuthentication()) {
            if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
                plugin.getServer().getScheduler()
                    .buildTask(plugin, () -> {
                        if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
                            player.disconnect(plugin.getLanguageManager().getComponent("auth.not-authenticated"));
                        }
                    })
                    .delay(1, TimeUnit.SECONDS)
                    .schedule();
            }
        }
    }
    
    /**
     * Send authentication state when player connects to a server
     */
    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        
        // Check if player has linked account
        if (!plugin.getLinkStorage().hasLink(player.getUniqueId())) {
            return;
        }
        
        // Send current authentication state to the server
        boolean authenticated = plugin.getAuthManager().isAuthenticated(player.getUniqueId());
        plugin.getServer().getScheduler()
            .buildTask(plugin, () -> plugin.sendAuthState(player, authenticated))
            .delay(500, TimeUnit.MILLISECONDS)
            .schedule();
    }
    
    @Subscribe(order = PostOrder.LAST)
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is in registration process
        if (plugin.getAuthManager().hasPendingRegistration(player.getUniqueId())) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
            String token = event.getMessage().trim();
            plugin.getLogger().info("[MCFactor] Player " + player.getUsername() + " attempting token registration");
            
            if (plugin.getAuthManager().completeRegistration(player.getUniqueId(), token)) {
                player.sendMessage(plugin.getLanguageManager().getComponent("command.register.success"));
            } else {
                player.sendMessage(plugin.getLanguageManager().getComponent("command.register.invalid-token"));
                plugin.getAuthManager().cancelRegistration(player.getUniqueId());
            }
            return;
        }
        
        // Check if player is awaiting code input
        if (plugin.getAuthManager().isAwaitingCodeInput(player.getUniqueId())) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
            String code = event.getMessage().trim();
            plugin.getLogger().info("[MCFactor] Player " + player.getUsername() + " attempting code authentication");
            
            if (plugin.getAuthManager().verifyCode(player.getUniqueId(), code)) {
                plugin.getAuthManager().setAwaitingCodeInput(player.getUniqueId(), false);
                player.sendMessage(plugin.getLanguageManager().getComponent("auth.success"));
                plugin.sendAuthState(player, true);
            } else {
                // Keep player in awaiting state, but disconnect on wrong code as before
                player.disconnect(plugin.getLanguageManager().getComponent("auth.invalid-code"));
            }
        }
    }
    
    @Subscribe
    public void onPlayerQuit(DisconnectEvent event) {
        plugin.getAuthManager().setAwaitingCodeInput(event.getPlayer().getUniqueId(), false);
    }
}

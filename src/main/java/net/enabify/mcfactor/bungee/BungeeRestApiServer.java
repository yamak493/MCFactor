package net.enabify.mcfactor.bungee;

import net.enabify.mcfactor.auth.AuthenticationManager;
import net.enabify.mcfactor.auth.RestApiServer;
import net.enabify.mcfactor.storage.LinkStorage;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;

public class BungeeRestApiServer extends RestApiServer {
    private final MCFactorBungee plugin;
    
    public BungeeRestApiServer(int port, AuthenticationManager authManager, LinkStorage linkStorage,
                               String serverId, String secretKey, MCFactorBungee plugin) {
        super(port, authManager, linkStorage, serverId, secretKey);
        this.plugin = plugin;
    }
    
    @Override
    protected UUID findPlayerUuid(String playerName) {
        ProxiedPlayer player = plugin.getProxy().getPlayer(playerName);
        if (player != null) {
            return player.getUniqueId();
        }
        
        return null;
    }
    
    @Override
    protected void onAuthenticationSuccess(UUID playerUuid) {
        // Send authentication state to downstream servers
        ProxiedPlayer player = plugin.getProxy().getPlayer(playerUuid);
        if (player != null) {
            plugin.sendAuthState(player, true);
        }
    }
}

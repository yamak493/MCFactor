package net.enabify.mcfactor.velocity;

import com.velocitypowered.api.proxy.Player;
import net.enabify.mcfactor.auth.AuthenticationManager;
import net.enabify.mcfactor.auth.RestApiServer;
import net.enabify.mcfactor.storage.LinkStorage;

import java.util.Optional;
import java.util.UUID;

public class VelocityRestApiServer extends RestApiServer {
    private final MCFactorVelocity plugin;
    
    public VelocityRestApiServer(int port, AuthenticationManager authManager, LinkStorage linkStorage,
                                 String serverId, String secretKey, MCFactorVelocity plugin) {
        super(port, authManager, linkStorage, serverId, secretKey);
        this.plugin = plugin;
    }
    
    @Override
    protected UUID findPlayerUuid(String playerName) {
        Optional<Player> player = plugin.getServer().getPlayer(playerName);
        if (player.isPresent()) {
            return player.get().getUniqueId();
        }
        
        return null;
    }
    
    @Override
    protected void onAuthenticationSuccess(UUID playerUuid) {
        // Velocity handles authentication state differently
        Optional<Player> player = plugin.getServer().getPlayer(playerUuid);
        if (player.isPresent()) {
            // Send authentication state to backend servers
            plugin.sendAuthState(player.get(), true);
        }
    }
}

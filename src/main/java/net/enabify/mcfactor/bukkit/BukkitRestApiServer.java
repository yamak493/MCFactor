package net.enabify.mcfactor.bukkit;

import net.enabify.mcfactor.auth.AuthenticationManager;
import net.enabify.mcfactor.auth.RestApiServer;
import net.enabify.mcfactor.storage.LinkStorage;
import net.enabify.mcfactor.util.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BukkitRestApiServer extends RestApiServer {
    private final MCFactorBukkit plugin;
    
    public BukkitRestApiServer(int port, AuthenticationManager authManager, LinkStorage linkStorage, 
                               String serverId, String secretKey, MCFactorBukkit plugin) {
        super(port, authManager, linkStorage, serverId, secretKey);
        this.plugin = plugin;
    }
    
    @Override
    protected UUID findPlayerUuid(String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player != null) {
            return player.getUniqueId();
        }
        
        // Search in link storage
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getName().equalsIgnoreCase(playerName)) {
                return onlinePlayer.getUniqueId();
            }
        }
        
        return null;
    }
    
    @Override
    protected void onAuthenticationSuccess(UUID playerUuid) {
        // In standalone mode, unfreeze the player if they are frozen
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && plugin.getFreezeManager().isFrozen(playerUuid)) {
            FoliaScheduler.runTask(plugin, player, () -> {
                plugin.getFreezeManager().unfreezePlayer(player);
            });
        }
    }
}

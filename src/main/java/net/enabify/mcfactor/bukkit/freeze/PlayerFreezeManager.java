package net.enabify.mcfactor.bukkit.freeze;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages player freeze state for authentication
 * Uses SPECTATOR mode + Move event cancellation to restrict player movement
 */
public class PlayerFreezeManager {
    private final Map<UUID, FreezeData> frozenPlayers = new HashMap<>();
    
    /**
     * Freeze a player (set to SPECTATOR mode and record original state)
     */
    public void freezePlayer(Player player) {
        if (frozenPlayers.containsKey(player.getUniqueId())) {
            return; // Already frozen
        }
        
        // Store original state
        FreezeData data = new FreezeData(
            player.getGameMode(),
            player.getLocation().clone()
        );
        frozenPlayers.put(player.getUniqueId(), data);
        
        // Set to spectator mode
        player.setGameMode(GameMode.SPECTATOR);
    }
    
    /**
     * Unfreeze a player (restore original game mode)
     */
    public void unfreezePlayer(Player player) {
        FreezeData data = frozenPlayers.remove(player.getUniqueId());
        if (data == null) {
            return; // Not frozen
        }
        
        // Restore original game mode
        player.setGameMode(data.getOriginalGameMode());
    }
    
    /**
     * Check if a player is frozen
     */
    public boolean isFrozen(UUID playerUuid) {
        return frozenPlayers.containsKey(playerUuid);
    }
    
    /**
     * Get the freeze location for a player
     */
    public Location getFreezeLocation(UUID playerUuid) {
        FreezeData data = frozenPlayers.get(playerUuid);
        return data != null ? data.getFreezeLocation() : null;
    }
    
    /**
     * Remove freeze data when player disconnects
     */
    public void cleanup(UUID playerUuid) {
        frozenPlayers.remove(playerUuid);
    }
    
    /**
     * Data class to store freeze information
     */
    private static class FreezeData {
        private final GameMode originalGameMode;
        private final Location freezeLocation;
        
        public FreezeData(GameMode originalGameMode, Location freezeLocation) {
            this.originalGameMode = originalGameMode;
            this.freezeLocation = freezeLocation;
        }
        
        public GameMode getOriginalGameMode() {
            return originalGameMode;
        }
        
        public Location getFreezeLocation() {
            return freezeLocation;
        }
    }
}

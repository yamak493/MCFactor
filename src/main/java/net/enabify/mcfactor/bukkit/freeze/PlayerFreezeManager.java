package net.enabify.mcfactor.bukkit.freeze;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages player freeze state for authentication
 * Blocks movement and various player actions
 */
public class PlayerFreezeManager {
    private final Map<UUID, FreezeData> frozenPlayers = new HashMap<>();
    
    /**
     * Freeze a player (record original state and restrict movement)
     */
    public void freezePlayer(Player player) {
        if (frozenPlayers.containsKey(player.getUniqueId())) {
            return; // Already frozen
        }
        
        // Store original state
        FreezeData data = new FreezeData(
            player.getLocation().clone()
        );
        frozenPlayers.put(player.getUniqueId(), data);
    }
    
    /**
     * Unfreeze a player
     */
    public void unfreezePlayer(Player player) {
        frozenPlayers.remove(player.getUniqueId());
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
        private final Location freezeLocation;
        
        public FreezeData(Location freezeLocation) {
            this.freezeLocation = freezeLocation;
        }
        
        public Location getFreezeLocation() {
            return freezeLocation;
        }
    }
}

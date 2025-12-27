package net.enabify.mcfactor.auth;

import net.enabify.mcfactor.storage.LinkStorage;
import net.enabify.mcfactor.util.HashUtil;

import java.util.*;

public class AuthenticationManager {
    private final LinkStorage linkStorage;
    private final String serverId;
    private final String secretKey;
    
    // Track pending registrations
    private final Map<UUID, PendingRegistration> pendingRegistrations;
    
    // Track authenticated players (for app authentication mode)
    private final Map<UUID, Long> authenticatedPlayers;
    
    public AuthenticationManager(LinkStorage linkStorage, String serverId, String secretKey) {
        this.linkStorage = linkStorage;
        this.serverId = serverId;
        this.secretKey = secretKey;
        this.pendingRegistrations = new HashMap<>();
        this.authenticatedPlayers = new HashMap<>();
    }
    
    /**
     * Start registration process for a player
     */
    public void startRegistration(UUID playerUuid, String playerName, String appKey, String passKey) {
        PendingRegistration registration = new PendingRegistration(playerName, appKey, passKey);
        pendingRegistrations.put(playerUuid, registration);
        
        // Auto-cancel after 5 minutes
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                pendingRegistrations.remove(playerUuid);
            }
        }, 5 * 60 * 1000);
    }
    
    /**
     * Complete registration by verifying token
     * Token format: SHA-256(serverId + timestamp + passKey), first 6 chars uppercase
     * Accepts tokens from current interval and ±1 interval (time skew tolerance)
     */
    public boolean completeRegistration(UUID playerUuid, String token) {
        PendingRegistration registration = pendingRegistrations.get(playerUuid);
        if (registration == null) {
            return false;
        }
        
        // Use the new HashUtil method that handles time skew tolerance
        if (HashUtil.verifyAuthToken(serverId, registration.passKey, token)) {
            linkStorage.addLink(
                playerUuid, 
                registration.playerName, 
                registration.appKey, 
                registration.passKey
            );
            pendingRegistrations.remove(playerUuid);
            return true;
        }
        
        return false;
    }
    
    /**
     * Cancel pending registration
     */
    public void cancelRegistration(UUID playerUuid) {
        pendingRegistrations.remove(playerUuid);
    }
    
    /**
     * Check if player has pending registration
     */
    public boolean hasPendingRegistration(UUID playerUuid) {
        return pendingRegistrations.containsKey(playerUuid);
    }
    
    /**
     * Verify code for code authentication mode
     * Code format: SHA-256(serverId + timestamp + passKey), first 6 chars uppercase
     * Uses the player's passKey to generate codes
     */
    public boolean verifyCode(UUID playerUuid, String code) {
        LinkStorage.PlayerLink link = linkStorage.getLink(playerUuid);
        if (link == null) {
            return false;
        }
        
        // Use verifyAuthToken with the player's passKey
        return HashUtil.verifyAuthToken(serverId, link.getPassKey(), code);
    }
    
    /**
     * Mark player as authenticated (for app authentication mode)
     */
    public void markAuthenticated(UUID playerUuid) {
        authenticatedPlayers.put(playerUuid, System.currentTimeMillis());
    }
    
    /**
     * Check if player is authenticated (within last 5 minutes)
     */
    public boolean isAuthenticated(UUID playerUuid) {
        Long authTime = authenticatedPlayers.get(playerUuid);
        if (authTime == null) {
            return false;
        }
        
        long timeDiff = System.currentTimeMillis() - authTime;
        return timeDiff < 5 * 60 * 1000; // 5 minutes
    }
    
    /**
     * Remove authentication
     */
    public void removeAuthentication(UUID playerUuid) {
        authenticatedPlayers.remove(playerUuid);
    }
    
    /**
     * Clean up old authentications
     */
    public void cleanupOldAuthentications() {
        long currentTime = System.currentTimeMillis();
        authenticatedPlayers.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > 5 * 60 * 1000
        );
    }
    
    private static class PendingRegistration {
        final String playerName;
        final String appKey;
        final String passKey;
        
        PendingRegistration(String playerName, String appKey, String passKey) {
            this.playerName = playerName;
            this.appKey = appKey;
            this.passKey = passKey;
        }
    }
}

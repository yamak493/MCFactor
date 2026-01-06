package net.enabify.mcfactor.messaging;

import java.io.*;
import java.util.UUID;

/**
 * Plugin Message for authentication state synchronization between Proxy and downstream servers
 */
public class AuthStateMessage {
    private final UUID playerUuid;
    private final boolean authenticated;
    
    public AuthStateMessage(UUID playerUuid, boolean authenticated) {
        this.playerUuid = playerUuid;
        this.authenticated = authenticated;
    }
    
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    public boolean isAuthenticated() {
        return authenticated;
    }
    
    /**
     * Serialize message to byte array for Plugin Messaging Channel
     */
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOut);
        
        // Write UUID
        out.writeLong(playerUuid.getMostSignificantBits());
        out.writeLong(playerUuid.getLeastSignificantBits());
        
        // Write authentication state
        out.writeBoolean(authenticated);
        
        return byteOut.toByteArray();
    }
    
    /**
     * Deserialize message from byte array received from Plugin Messaging Channel
     */
    public static AuthStateMessage fromBytes(byte[] data) throws IOException {
        ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
        DataInputStream in = new DataInputStream(byteIn);
        
        // Read UUID
        long mostSigBits = in.readLong();
        long leastSigBits = in.readLong();
        UUID playerUuid = new UUID(mostSigBits, leastSigBits);
        
        // Read authentication state
        boolean authenticated = in.readBoolean();
        
        return new AuthStateMessage(playerUuid, authenticated);
    }
}

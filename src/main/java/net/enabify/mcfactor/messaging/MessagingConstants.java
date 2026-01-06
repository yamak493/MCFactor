package net.enabify.mcfactor.messaging;

/**
 * Constants for Plugin Messaging Channels
 */
public class MessagingConstants {
    /**
     * Channel name for authentication state synchronization
     * Used for communication between Proxy and downstream servers
     */
    public static final String CHANNEL_NAME = "mcfactor:authstate";
    
    private MessagingConstants() {
        // Utility class, no instantiation
    }
}

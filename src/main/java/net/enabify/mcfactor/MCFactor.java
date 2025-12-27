package net.enabify.mcfactor;

import net.enabify.mcfactor.bungee.MCFactorBungee;
import net.enabify.mcfactor.bukkit.MCFactorBukkit;

/**
 * Main entry point for MCFactor plugin
 * This class serves as a bridge to detect the platform and load the appropriate implementation
 */
public final class MCFactor {
    
    // Bukkit/Spigot entry point
    public static class Bukkit extends MCFactorBukkit {
        // All logic is in MCFactorBukkit
    }
    
    // BungeeCord entry point
    public static class Bungee extends MCFactorBungee {
        // All logic is in MCFactorBungee
    }
}

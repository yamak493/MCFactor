package net.enabify.mcfactor.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {
    
    /**
     * Generate SHA-256 hash and return the first 6 characters as token
     * @param input The input string to hash
     * @return First 6 characters of the hash in uppercase
     */
    public static String generateToken(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            String hexHash = bytesToHex(hash);
            return hexHash.substring(0, 6).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Get current timestamp rounded to 3-minute intervals
     * @return Timestamp in seconds (rounded to 3-minute intervals)
     */
    public static long getTimestamp() {
        long currentSeconds = System.currentTimeMillis() / 1000;
        return (currentSeconds / 180) * 180; // 180 seconds = 3 minutes
    }
    
    /**
     * Generate token from Server ID, timestamp, and Pass Key
     * Algorithm: SHA-256(serverId + timestamp + passKey), first 6 chars uppercase
     * @param serverId Server ID
     * @param timestamp Timestamp (should be rounded to 3-minute intervals)
     * @param passKey Pass Key
     * @return Generated token
     */
    public static String generateAuthToken(String serverId, long timestamp, String passKey) {
        String input = serverId + timestamp + passKey;
        return generateToken(input);
    }
    
    /**
     * Generate token from Server ID and Pass Key using current timestamp
     * @param serverId Server ID
     * @param passKey Pass Key
     * @return Generated token
     */
    public static String generateAuthToken(String serverId, String passKey) {
        long timestamp = getTimestamp();
        return generateAuthToken(serverId, timestamp, passKey);
    }
    
    /**
     * Verify token against Server ID and Pass Key, allowing for time skew
     * Accepts tokens from current interval and ±1 interval (±180 seconds)
     * @param serverId Server ID
     * @param passKey Pass Key
     * @param token Token to verify
     * @return true if token is valid, false otherwise
     */
    public static boolean verifyAuthToken(String serverId, String passKey, String token) {
        long currentTimestamp = getTimestamp();
        
        // Check current interval and ±1 interval
        for (long offset = -180; offset <= 180; offset += 180) {
            long testTimestamp = currentTimestamp + offset;
            String expectedToken = generateAuthToken(serverId, testTimestamp, passKey);
            if (expectedToken.equals(token)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Generate token for code authentication
     * @param serverId Server ID
     * @param secretKey Secret Key
     * @return Generated token
     */
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Generate random string for keys
     * @param length Length of the string
     * @return Random string
     */
    public static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return sb.toString();
    }
    
    /**
     * Debug method: Generate token with detailed logging
     * @param serverId Server ID
     * @param timestamp Timestamp
     * @param passKey Pass Key
     * @param logger Logger for output (optional, can be null)
     * @return Generated token
     */
    public static String generateAuthTokenDebug(String serverId, long timestamp, String passKey, java.util.logging.Logger logger) {
        String input = serverId + timestamp + passKey;
        String token = generateToken(input);
        
        if (logger != null) {
            logger.info("[MCFactor Token Debug]");
            logger.info("  Server ID: " + serverId);
            logger.info("  Timestamp: " + timestamp);
            logger.info("  Pass Key: " + passKey);
            logger.info("  Combined: " + input);
            logger.info("  Generated Token: " + token);
        }
        
        return token;
    }
    
    /**
     * Debug method: Verify token with detailed logging
     * @param serverId Server ID
     * @param passKey Pass Key
     * @param token Token to verify
     * @param logger Logger for output (optional, can be null)
     * @return true if token is valid
     */
    public static boolean verifyAuthTokenDebug(String serverId, String passKey, String token, java.util.logging.Logger logger) {
        long currentTimestamp = getTimestamp();
        
        if (logger != null) {
            logger.info("[MCFactor Verify Debug] Current Timestamp: " + currentTimestamp);
            logger.info("[MCFactor Verify Debug] Input Token: " + token);
        }
        
        // Check current interval and ±1 interval
        for (long offset = -180; offset <= 180; offset += 180) {
            long testTimestamp = currentTimestamp + offset;
            String expectedToken = generateAuthToken(serverId, testTimestamp, passKey);
            
            if (logger != null) {
                logger.info("[MCFactor Verify Debug] Testing timestamp " + testTimestamp + " -> token: " + expectedToken);
            }
            
            if (expectedToken.equals(token)) {
                if (logger != null) {
                    logger.info("[MCFactor Verify Debug] Token MATCHED with timestamp " + testTimestamp);
                }
                return true;
            }
        }
        
        if (logger != null) {
            logger.info("[MCFactor Verify Debug] Token FAILED - no match in any interval");
        }
        
        return false;
    }
}

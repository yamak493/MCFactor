package net.enabify.mcfactor.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {
    
    /**
     * Generate SHA3-256 hash and return the first 6 characters as token
     * @param input The input string to hash
     * @return First 6 characters of the hash
     */
    public static String generateToken(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA3-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            String hexHash = bytesToHex(hash);
            return hexHash.substring(0, 6).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            // Fallback to SHA-256 if SHA3-256 is not available
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
                String hexHash = bytesToHex(hash);
                return hexHash.substring(0, 6).toUpperCase();
            } catch (NoSuchAlgorithmException ex) {
                throw new RuntimeException("No hash algorithm available", ex);
            }
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
     * Generate token from Server ID, Secret Key, App Key, Pass Key, and timestamp
     * @param serverId Server ID
     * @param secretKey Secret Key
     * @param appKey App Key
     * @param passKey Pass Key
     * @return Generated token
     */
    public static String generateAuthToken(String serverId, String secretKey, String appKey, String passKey) {
        long timestamp = getTimestamp();
        String input = serverId + timestamp + appKey + passKey;
        return generateToken(input);
    }
    
    /**
     * Generate token for code authentication (without app key and pass key)
     * @param serverId Server ID
     * @param secretKey Secret Key
     * @return Generated token
     */
    public static String generateCodeToken(String serverId, String secretKey) {
        long timestamp = getTimestamp();
        String input = serverId + timestamp + secretKey;
        return generateToken(input);
    }
    
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
}

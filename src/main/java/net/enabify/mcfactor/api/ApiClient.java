package net.enabify.mcfactor.api;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ApiClient {
    private static final String API_BASE_URL = "https://api.enabify.net/mcfactor";
    private static final Gson gson = new Gson();
    
    // Rate limiting maps
    private static final Map<String, Long> rateLimitMap = new HashMap<>();
    
    /**
     * Register server and get Server ID and Secret Key
     * @param host REST API host
     * @param port REST API port
     * @param type Authentication type
     * @return JsonObject containing serverid and secretkey
     */
    public static JsonObject registerServer(String host, int port, String type) throws Exception {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("host", host);
        requestBody.addProperty("port", port);
        requestBody.addProperty("type", type);
        
        return postRequest(API_BASE_URL + "/register.php", requestBody);
    }
    
    /**
     * Update server information
     * @param serverId Server ID
     * @param secretKey Secret Key
     * @param host REST API host
     * @param port REST API port
     * @param type Authentication type
     * @return JsonObject containing success status
     */
    public static JsonObject updateServer(String serverId, String secretKey, String host, int port, String type) throws Exception {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("serverid", serverId);
        requestBody.addProperty("secretkey", secretKey);
        requestBody.addProperty("host", host);
        requestBody.addProperty("port", port);
        requestBody.addProperty("type", type);
        
        return postRequest(API_BASE_URL + "/update.php", requestBody);
    }
    
    /**
     * Get server information
     * @param serverId Server ID
     * @return JsonObject containing host, port, and type
     */
    public static JsonObject getServerInfo(String serverId) throws Exception {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("serverid", serverId);
        
        return postRequest(API_BASE_URL + "/info.php", requestBody);
    }
    
    /**
     * Temporary registration - Plugin side (get Pass Key)
     * @param serverId Server ID
     * @param secretKey Secret Key
     * @param appKey App Key
     * @return JsonObject containing passkey
     */
    public static JsonObject tempRegisterPlugin(String serverId, String secretKey, String appKey) throws Exception {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("serverid", serverId);
        requestBody.addProperty("secretkey", secretKey);
        requestBody.addProperty("appkey", appKey);
        
        return postRequest(API_BASE_URL + "/temp.php", requestBody);
    }
    
    /**
     * Temporary registration - App side (get Server ID and Pass Key)
     * @param appKey App Key
     * @return JsonObject containing serverid and passkey
     */
    public static JsonObject tempRegisterApp(String appKey) throws Exception {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("appkey", appKey);
        
        return postRequest(API_BASE_URL + "/temp.php", requestBody);
    }
    
    /**
     * Test REST API endpoint
     * @param host REST API host
     * @param port REST API port
     * @return true if the endpoint is responding, false otherwise
     */
    public static boolean testRestApiEndpoint(String host, int port) {
        try {
            String urlString = "http://" + host + ":" + port + "/authenticate";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000); // 5 second timeout
            conn.setReadTimeout(5000);
            
            // Send a test request with invalid credentials
            JsonObject testBody = new JsonObject();
            testBody.addProperty("appkey", "test-key");
            testBody.addProperty("token", "TEST00");
            
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = gson.toJson(testBody).getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // Read response
            int responseCode = conn.getResponseCode();
            
            // We expect a 403 error for invalid credentials, which means the API is working
            // Any 4xx or 2xx response means the endpoint is responding
            return responseCode >= 200 && responseCode < 500;
            
        } catch (Exception e) {
            // Connection failed, API is not accessible
            return false;
        }
    }
    
    private static JsonObject postRequest(String urlString, JsonObject requestBody) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = gson.toJson(requestBody).getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        // Read response
        int responseCode = conn.getResponseCode();
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        
        // Parse response flexibly - handle both JsonObject and other types
        String responseString = response.toString();
        JsonElement element = gson.fromJson(responseString, JsonElement.class);
        
        // If the response is already a JsonObject, return it
        if (element.isJsonObject()) {
            return element.getAsJsonObject();
        }
        
        // If the response is a primitive (string, number, boolean), wrap it in a JsonObject
        JsonObject result = new JsonObject();
        if (element.isJsonPrimitive()) {
            result.add("response", element);
        } else if (element.isJsonArray()) {
            result.add("response", element);
        } else {
            result.add("response", element);
        }
        
        return result;
    }
}

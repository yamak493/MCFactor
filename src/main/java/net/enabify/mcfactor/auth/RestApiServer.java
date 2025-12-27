package net.enabify.mcfactor.auth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.enabify.mcfactor.storage.LinkStorage;
import net.enabify.mcfactor.util.HashUtil;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

public abstract class RestApiServer {
    private HttpServer server;
    private final int port;
    protected final AuthenticationManager authManager;
    protected final LinkStorage linkStorage;
    protected final String serverId;
    protected final String secretKey;
    protected final Gson gson;
    
    // Rate limiting
    private final Map<String, Long> rateLimitMap;
    
    public RestApiServer(int port, AuthenticationManager authManager, LinkStorage linkStorage, String serverId, String secretKey) {
        this.port = port;
        this.authManager = authManager;
        this.linkStorage = linkStorage;
        this.serverId = serverId;
        this.secretKey = secretKey;
        this.gson = new Gson();
        this.rateLimitMap = new HashMap<>();
    }
    
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/authenticate", new AuthenticateHandler());
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
    }
    
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
    
    private boolean checkRateLimit(String ip) {
        long currentTime = System.currentTimeMillis();
        Long lastRequest = rateLimitMap.get(ip);
        
        if (lastRequest != null && currentTime - lastRequest < 10000) {
            return false;
        }
        
        rateLimitMap.put(ip, currentTime);
        return true;
    }
    
    /**
     * Find player UUID by player name - must be implemented by platform-specific subclasses
     */
    protected abstract UUID findPlayerUuid(String playerName);
    
    private class AuthenticateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
                return;
            }
            
            String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
            
            if (!checkRateLimit(clientIp)) {
                sendResponse(exchange, 429, "{\"error\": \"Rate limit exceeded\"}");
                return;
            }
            
            try {
                // Read request body
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);
                StringBuilder requestBody = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    requestBody.append(line);
                }
                
                JsonObject request = gson.fromJson(requestBody.toString(), JsonObject.class);
                String playerName = request.get("player").getAsString();
                String appKey = request.get("appkey").getAsString();
                String token = request.get("token").getAsString();
                
                System.out.println("[MCFactor REST API] Received authentication request");
                System.out.println("[MCFactor REST API] Player: " + playerName);
                System.out.println("[MCFactor REST API] App Key: " + appKey);
                System.out.println("[MCFactor REST API] Token: " + token);
                
                // Find player by name
                UUID playerUuid = findPlayerUuid(playerName);
                if (playerUuid == null) {
                    System.out.println("[MCFactor REST API] Player not found: " + playerName);
                    sendResponse(exchange, 404, "{\"success\": false, \"error\": \"Player not found\"}");
                    return;
                }
                
                System.out.println("[MCFactor REST API] Player UUID: " + playerUuid);
                
                // Get player link
                LinkStorage.PlayerLink link = linkStorage.getLink(playerUuid);
                if (link == null) {
                    System.out.println("[MCFactor REST API] No player link found for: " + playerName);
                    sendResponse(exchange, 403, "{\"success\": false, \"error\": \"Invalid app key\"}");
                    return;
                }
                
                System.out.println("[MCFactor REST API] Stored App Key: " + link.getAppKey());
                System.out.println("[MCFactor REST API] Stored Pass Key: " + link.getPassKey());
                
                if (!link.getAppKey().equals(appKey)) {
                    System.out.println("[MCFactor REST API] App key mismatch");
                    sendResponse(exchange, 403, "{\"success\": false, \"error\": \"Invalid app key\"}");
                    return;
                }
                
                System.out.println("[MCFactor REST API] Server ID: " + serverId);
                
                // Verify token using the correct algorithm
                // Token format: SHA-256(serverId + timestamp + passKey), first 6 chars uppercase
                // Time skew tolerance: ±180 seconds (±1 interval)
                if (HashUtil.verifyAuthToken(serverId, link.getPassKey(), token)) {
                    System.out.println("[MCFactor REST API] Token verification SUCCESS");
                    authManager.markAuthenticated(playerUuid);
                    sendResponse(exchange, 200, "{\"success\": true}");
                } else {
                    System.out.println("[MCFactor REST API] Token verification FAILED");
                    sendResponse(exchange, 403, "{\"success\": false, \"error\": \"Invalid token\"}");
                }
                
            } catch (Exception e) {
                sendResponse(exchange, 400, "{\"error\": \"Invalid request\"}");
            }
        }
        
        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }
}

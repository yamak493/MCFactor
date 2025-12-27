package net.enabify.mcfactor.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LinkStorage {
    private final File linkFile;
    private Map<String, PlayerLink> links;
    private final Gson gson;
    
    public LinkStorage(File dataFolder) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.linkFile = new File(dataFolder, "link.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.links = new HashMap<>();
        load();
    }
    
    private void load() {
        if (!linkFile.exists()) {
            links = new HashMap<>();
            save();
            return;
        }
        
        try (Reader reader = new FileReader(linkFile)) {
            Type type = new TypeToken<Map<String, PlayerLink>>(){}.getType();
            Map<String, PlayerLink> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                links = loaded;
            }
        } catch (IOException e) {
            e.printStackTrace();
            links = new HashMap<>();
        }
    }
    
    public void save() {
        try (Writer writer = new FileWriter(linkFile)) {
            gson.toJson(links, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void addLink(UUID playerUuid, String playerName, String appKey, String passKey) {
        PlayerLink link = new PlayerLink(playerName, appKey, passKey);
        links.put(playerUuid.toString(), link);
        save();
    }
    
    public void removeLink(UUID playerUuid) {
        links.remove(playerUuid.toString());
        save();
    }
    
    public PlayerLink getLink(UUID playerUuid) {
        return links.get(playerUuid.toString());
    }
    
    public boolean hasLink(UUID playerUuid) {
        return links.containsKey(playerUuid.toString());
    }
    
    public void removeLinkByName(String playerName) {
        links.entrySet().removeIf(entry -> 
            entry.getValue().getPlayerName().equalsIgnoreCase(playerName)
        );
        save();
    }
    
    public static class PlayerLink {
        private final String playerName;
        private final String appKey;
        private final String passKey;
        
        public PlayerLink(String playerName, String appKey, String passKey) {
            this.playerName = playerName;
            this.appKey = appKey;
            this.passKey = passKey;
        }
        
        public String getPlayerName() {
            return playerName;
        }
        
        public String getAppKey() {
            return appKey;
        }
        
        public String getPassKey() {
            return passKey;
        }
    }
}

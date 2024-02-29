package dev.jsinco.textureapi.storage;

import org.bukkit.Bukkit;

import java.util.UUID;

public class CachedTexture {


    private final UUID uuid;
    private String base64;
    private long lastUpdated;
    private boolean keepAlive;


    public CachedTexture(UUID uuid, String base64, boolean keepAlive) {
        this.uuid = uuid;
        this.base64 = base64;
        this.lastUpdated = System.currentTimeMillis();
        this.keepAlive = keepAlive;
    }

    public CachedTexture(UUID uuid, String base64, long lastUpdated, boolean keepAlive) {
        this.uuid = uuid;
        this.base64 = base64;
        this.lastUpdated = lastUpdated;
        this.keepAlive = keepAlive;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setBase64(String base64) {
        this.base64 = base64;
    }

    public String getBase64() {
        return base64;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void updateLastUpdated() {
        this.lastUpdated = System.currentTimeMillis();
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public boolean keepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public void updateWhenPlayerOnline() {
        if (Bukkit.getPlayer(uuid) != null) {
            updateLastUpdated();
        }
    }
}

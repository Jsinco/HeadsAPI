package dev.jsinco.textureapi.storage;

import dev.jsinco.textureapi.TextureAPI;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class Database {

    private final static long CACHE_TIME = 12000;
    protected final static ConcurrentLinkedQueue<CachedTexture> cache = new ConcurrentLinkedQueue<>();
    public static BukkitTask cacheTask;


    public abstract Connection getConnection();
    public abstract void closeConnection();

    protected void initDatabase() {
        try (PreparedStatement statement = getConnection().prepareStatement(
                "CREATE TABLE IF NOT EXISTS textures (uuid VARCHAR(36) PRIMARY KEY, base64 TEXT);")){

            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Async Bukkit Database task
        cacheTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (CachedTexture cachedTexture : cache) {
                    if (cachedTexture.keepAlive()) continue;
                    cachedTexture.updateWhenPlayerOnline();

                    if (System.currentTimeMillis() - cachedTexture.getLastUpdated() > CACHE_TIME) {
                        saveCachedTexture(cachedTexture);
                        cache.remove(cachedTexture);
                    }
                }
            }
        }.runTaskTimerAsynchronously(TextureAPI.getPlugin(), 0L, CACHE_TIME);
    }

    @Nullable
    public CachedTexture pullTextureFromDB(UUID uuid) {
        for (CachedTexture cachedTexture : cache) {
            if (cachedTexture.getUuid().equals(uuid)) {
                cachedTexture.updateLastUpdated();
                return cachedTexture;
            }
        }

        try (PreparedStatement statement = getConnection().prepareStatement("SELECT * FROM textures WHERE uuid=?;")) {

            statement.setString(1, String.valueOf(uuid));
            String base64 = statement.executeQuery().getString("base64");
            if (base64 != null) {
                CachedTexture cachedTexture = new CachedTexture(uuid, base64, false);
                cache.add(cachedTexture);
                return cachedTexture;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return null;
    }


    public void saveCachedTexture(CachedTexture cachedTexture) {
        try (PreparedStatement statement = getConnection().prepareStatement("INSERT OR REPLACE INTO textures (uuid, base64) VALUES (?, ?)")) {

            statement.setString(1, cachedTexture.getUuid().toString());
            statement.setString(2, cachedTexture.getBase64());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void saveTexture(UUID uuid, String base64, boolean cacheIt) {
        if (base64 == null) {
            throw new IllegalArgumentException("Null base64 or uuid!");
        }
        CachedTexture cachedTexture = new CachedTexture(uuid, base64, false);
        if (cacheIt) {
            cache.removeIf(cachedTexture1 -> cachedTexture1.getUuid().equals(uuid));
            cache.add(cachedTexture);
        }
        saveCachedTexture(cachedTexture);
    }

    public ConcurrentLinkedQueue<CachedTexture> getCache() {
        return cache;
    }
}

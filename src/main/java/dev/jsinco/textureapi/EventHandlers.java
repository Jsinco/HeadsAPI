package dev.jsinco.textureapi;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import dev.jsinco.textureapi.storage.CachedTexture;
import dev.jsinco.textureapi.storage.Database;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Base64;
import java.util.UUID;

public class EventHandlers implements Listener {

    private final Database sql;

    public EventHandlers(Database sql) {
        this.sql = sql;
    }

    @EventHandler
    public void whenPlayerJoinsServer(PlayerJoinEvent event) {
        new BukkitRunnable() {
            @Override
            public void run() {
                UUID uuid = event.getPlayer().getUniqueId();
                PlayerProfile profile = event.getPlayer().getPlayerProfile();

                TextureAPI.log("Running async task to pull textures from database.");
                CachedTexture cachedTexture = sql.pullTextureFromDB(uuid);

                try {
                    String base64 = null;
                    for (ProfileProperty property : profile.getProperties()) {
                        base64 = property.getValue();
                    }

                    if (cachedTexture != null) {
                        for (CachedTexture cachedTexture1 : sql.getCache()) {
                            if (cachedTexture1.getUuid().equals(uuid)) {
                                return;
                            }
                        }

                        sql.saveTexture(uuid, base64, true);
                        TextureAPI.log("Texture found in database, but it's not cached, updating!");
                    } else if (base64 != null) {
                        sql.saveTexture(uuid, base64, true);
                        TextureAPI.log("Texture not found in database, saving...");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(TextureAPI.getPlugin());
    }
}

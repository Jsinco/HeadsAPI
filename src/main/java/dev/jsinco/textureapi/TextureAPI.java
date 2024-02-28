package dev.jsinco.textureapi;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.jsinco.textureapi.storage.CachedTexture;
import dev.jsinco.textureapi.storage.SQLite;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.UUID;

public final class TextureAPI extends JavaPlugin {

    private static final Gson gson = new Gson();
    private static boolean verbose = false;
    private static TextureAPI plugin;
    private static SQLite database;

    @Override
    public void onEnable() {
        plugin = this;
        database = new SQLite(this);
    }

    @Override
    public void onDisable() {
        database.closeConnection();
    }

    /**
     * Get the base64 texture of a player's head using the Mojang API
     * @param uuid the UUID of the player
     * @return the base64 texture of the player's head or null if the player has no texture
     * @throws IOException if the Mojang API is down, or you have no internet connection
     */
    @Nullable
    public static String getBase64ThruAPI(@NotNull String uuid) throws IOException {
        if (verbose) {
            Bukkit.getLogger().info("[TextureAPI] Getting MC profile through API...");
        }
        StringBuilder content = getMinecraftProfile(uuid);
        JsonObject jsonObject = gson.fromJson(content.toString(), JsonObject.class);
        JsonElement properties = jsonObject.get("properties");
        for (JsonElement property : properties.getAsJsonArray()) {
            String base64 = property.getAsJsonObject().get("value").getAsString().replace("\"", "").strip();
            database.saveTexture(UUID.fromString(uuid), base64, true);
            return base64;
        }
        return null;
    }


    /**
     * Get the Minecraft profile of a player using the Mojang API
     * @param uuid the UUID of the player
     * @return the Minecraft profile of the player if it's a valid UUID
     * @throws IOException if the Mojang API is down, or you have no internet connection
     */
    public static @NotNull StringBuilder getMinecraftProfile(@NotNull String uuid) throws IOException {
        uuid = uuid.replace("-", "").strip();
        StringBuilder content = new StringBuilder();

        try {
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();
        } catch (ConnectException e) {
            e.printStackTrace(); // maybe just ignore
        }

        if (verbose) {
            Bukkit.getLogger().info("[TextureAPI] Got MC profile through API!\n" + content);
        }

        return content;
    }


    /**
     * Get the base64 texture of a player's head without using the Mojang API
     * @param uuid the UUID of the player
     * @return the base64 texture of the player's head or null if Bukkit cannot find their texture
     */
    @Nullable
    private static String getBase64ThruBukkit(@NotNull String uuid) {
        if (verbose) {
            Bukkit.getLogger().info("[TextureAPI] Looking for MC profile through Bukkit...");
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        if (!offlinePlayer.hasPlayedBefore()) {
            return null;
        }
        PlayerProfile playerProfile = offlinePlayer.getPlayerProfile();
        if (verbose) {
            Bukkit.getLogger().info("[TextureAPI] Getting texture from Bukkit player profile! \n" + playerProfile);
        }

        List<ProfileProperty> profileProperties = playerProfile.getProperties().stream().toList();
        if (profileProperties.isEmpty()) {
            return null;
        }
        return profileProperties.get(0).getValue();
    }

    /**
     * Get the base64 texture of a player's head using the Mojang API if the texture is not found in Bukkit
     * @param uuid the UUID of the player
     * @return the base64 texture of the player's head or null if the player has no texture
     */
    @Nullable
    public static String getBase64(UUID uuid) {
        CachedTexture base64 = database.pullTextureFromDB(uuid);
        if (base64 == null) {
            log("Texture not found in database, getting texture from API...");
            try {
                return getBase64ThruAPI(uuid.toString());
            } catch (IOException e) {
                throw new RuntimeException("Could not get texture from Mojang API", e);
            }
        }
        return base64.getBase64();
    }

    /**
     * Set the verbose mode of the API
     * @param verbose true if you want to see the API's logs
     */
    public static void setVerbose(boolean verbose) {
        TextureAPI.verbose = verbose;
    }


    public static void log(String message) {
        if (verbose) {
            Bukkit.getLogger().info("[TextureAPI] " + message);
        }
    }

    /**
     * Get the verbose mode of the API
     * @return true if the API is in verbose mode
     */
    public static boolean isVerbose() {
        return verbose;
    }

    public static TextureAPI getPlugin() {
        return plugin;
    }
}
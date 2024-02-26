package dev.jsinco.headsapi;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class HeadsAPI {
    /**
     * Get the base64 texture of a player's head using the Mojang API
     * @param uuid the UUID of the player
     * @return the base64 texture of the player's head or null if the player has no texture
     * @throws IOException if the Mojang API is down, or you have no internet connection
     */
    @Nullable
    public static String getBase64ThruAPI(@NotNull String uuid) throws IOException {
        StringBuilder content = getMinecraftProfile(uuid);
        JsonObject jsonObject = new Gson().fromJson(content.toString(), JsonObject.class);
        JsonElement properties = jsonObject.get("properties");
        for (JsonElement property : properties.getAsJsonArray()) {
            return property.getAsJsonObject().get("value").getAsString().replace("\"", "").strip();
        }
        return null;
    }


    /**
     * Get the Minecraft profile of a player using the Mojang API
     * @param uuid the UUID of the player
     * @return the Minecraft profile of the player if it's a valid UUID
     * @throws IOException if the Mojang API is down, or you have no internet connection
     */
    @Nullable
    public static StringBuilder getMinecraftProfile(@NotNull String uuid) throws IOException {
        uuid = uuid.replace("-", "").strip();
        URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        StringBuilder content = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();
        }
        if (content.isEmpty()) {
            return null;
        }
        return content;
    }


    /**
     * Get the base64 texture of a player's head without using the Mojang API
     * @param uuid the UUID of the player
     * @return the base64 texture of the player's head or null if Bukkit cannot find their texture
     */
    @Nullable
    public static String getBase64ThruPaper(@NotNull String uuid) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        URL url = offlinePlayer.getPlayerProfile().getTextures().getSkin();
        if (url != null) {
            return url.toString().replace("http://textures.minecraft.net/texture/", "").strip();
        }
        return null;
    }

    /**
     * Get the base64 texture of a player's head using the Mojang API if the texture is not found in Bukkit
     * @param uuid the UUID of the player
     * @return the base64 texture of the player's head or null if the player has no texture
     */
    @Nullable
    public static String getBase64(@NotNull String uuid) {
        String base64 = getBase64ThruPaper(uuid);
        if (base64 == null) {
            try {
                base64 = getBase64ThruAPI(uuid);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return base64;
    }

    /**
     * Get the base64 texture of a player's head using the Mojang API if the texture is not found in Bukkit
     * @param uuid the UUID of the player
     * @return the base64 texture of the player's head or null if the player has no texture
     */
    @Nullable
    public static String getBase64(@NotNull UUID uuid) {
        return getBase64(uuid.toString());
    }
}
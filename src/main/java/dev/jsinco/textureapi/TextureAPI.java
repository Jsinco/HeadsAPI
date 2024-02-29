package dev.jsinco.textureapi;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.jsinco.textureapi.storage.CachedTexture;
import dev.jsinco.textureapi.storage.Database;
import dev.jsinco.textureapi.storage.SQLite;
import org.bukkit.Bukkit;
import org.bukkit.Warning;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.UUID;
import java.util.logging.Level;

// TODO: Providing plugin

public class TextureAPI {

    public static final String VERSION = "2.0";

    private static final Gson gson = new Gson();
    private static boolean verbose = false;
    private static JavaPlugin plugin;
    private static Database database;

    public TextureAPI(final JavaPlugin provider) {
        this(provider, false);
    }

    public TextureAPI(final JavaPlugin provider, boolean verbose) {
        plugin = provider;
        database = new SQLite(provider);
        TextureAPI.verbose = verbose;

        provider.getServer().getPluginManager().registerEvents(new EventHandlers(database), provider);
        if (provider instanceof TextureAPIJavaPlugin) {
            provider.getCommand("textureapi").setExecutor(new TextureAPIJavaPluginCommand(database));
        }
    }

    public void shutdown() {
        database.closeConnection();
    }

    /**
     * Get the base64 texture of a player's head using the Mojang API
     * @param uuid the UUID of the player
     * @return the base64 texture of the player's head or null if the player has no texture
     * @throws IOException if the Mojang API is down, or you have no internet connection
     */
    public static String getBase64ThruAPI(@NotNull String uuid) throws IOException {
        log("Getting MC profile through Mojang API...");

        // check if running on main thread
        if (Bukkit.isPrimaryThread()) {
            throw new IllegalArgumentException("Don't run this method on the main thread!");
        }

        StringBuilder content = getMinecraftProfile(uuid);
        if (content == null) {
            return null;
        }
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
    @Nullable
    private static StringBuilder getMinecraftProfile(@NotNull String uuid) throws IOException {
        uuid = uuid.replace("-", "").strip();
        StringBuilder content = new StringBuilder();

        try {
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.addRequestProperty("User-Agent", "Mozilla/5.0");
            con.setConnectTimeout(10000);

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();
        } catch (ConnectException | SocketTimeoutException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get texture from Mojang after 10 seconds! Try again maybe?");
            if (verbose) {
                e.printStackTrace();
            }
            return null;
        }

        log("Got MC profile through API!\n" + content);

        return content;
    }

    public static CachedTexture getTexture(UUID uuid) {
        return getTexture(uuid, false);
    }

    /**
     * Get the base64 texture of a player's head using the Mojang API if the texture is not found in Bukkit
     * @param uuid the UUID of the player
     * @return the base64 texture of the player's head or null if the player has no texture
     */
    public static CachedTexture getTexture(UUID uuid, boolean keepAlive) {
        CachedTexture texture = database.pullTextureFromDB(uuid);
        if (texture == null) {
            log("Texture not found in database, getting texture from API...");
            try {
                return new CachedTexture(uuid, getBase64ThruAPI(uuid.toString()), keepAlive);
            } catch (IOException e) {
                throw new RuntimeException("Could not get texture from Mojang API. Is it down?", e);
            }
        }
        return texture;
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

    public static JavaPlugin getPlugin() {
        return plugin;
    }
}
package dev.jsinco.textureapi.storage;

import dev.jsinco.textureapi.FileManager;
import dev.jsinco.textureapi.TextureAPI;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;

public class SQLite extends Database {

    private final TextureAPI plugin;
    private Connection connection;
    private final File sqlFile;


    public SQLite(final TextureAPI plugin) {
        this.plugin = plugin;
        sqlFile = new FileManager("textures.db").generateAndGetFile();
        if (!sqlFile.exists()) {
            try {
                sqlFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        initDatabase();
    }

    @Override
    public Connection getConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                return connection;
            }

            connection = DriverManager.getConnection("jdbc:sqlite:" + sqlFile);
            return connection;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "SQLite exception on initialize", ex);
        }
        return null;
    }

    @Override
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                for (CachedTexture cachedTexture : cache) {
                    saveCachedTexture(cachedTexture);
                }
                connection.close();
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "SQLite exception on close", ex);
        }
    }
}

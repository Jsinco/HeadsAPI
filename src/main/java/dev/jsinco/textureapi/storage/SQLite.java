package dev.jsinco.textureapi.storage;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;

public class SQLite extends Database {

    private final JavaPlugin plugin;
    private Connection connection;
    private final File sqlFile;


    public SQLite(final JavaPlugin plugin) {
        this.plugin = plugin;
        sqlFile = new FileManager("textureAPI.db").generateAndGetFile();
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
            ex.printStackTrace();
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

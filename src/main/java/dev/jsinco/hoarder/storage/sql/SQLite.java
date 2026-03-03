package dev.jsinco.hoarder.storage.sql;

import dev.jsinco.hoarder.Hoarder;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;

public class SQLite extends Database {


    private Connection connection;
    private final Hoarder plugin;
    private final String path;

    public SQLite(Hoarder plugin) {
        this.plugin = plugin;

        File dbFile = new File(plugin.getDataFolder(), plugin.getConfig().getString("storage.database") + ".db");
        if (!dbFile.exists()) {
            try {
                dbFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create SQLite database file!", e);
            }
        }

        path = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/" + plugin.getConfig().getString("storage.database") + ".db";
        initializeDatabase(true).join();
    }

    @Override
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(path);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to SQLite database!", e);
        }

        return connection;
    }
}

package dev.jsinco.hoarder.storage.sql;

import dev.jsinco.hoarder.Hoarder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;

public class MySQL extends Database {

    private Connection connection;
    private final Hoarder plugin;

    private final String address;
    private final String username;
    private final String password;

    public MySQL(Hoarder plugin) {
        this.plugin = plugin;

        this.address = "jdbc:mysql://" + plugin.getConfig().getString("storage.address");
        this.username = plugin.getConfig().getString("storage.username");
        this.password = plugin.getConfig().getString("storage.password");
        initializeDatabase(false).join();
    }

    @Override
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(address, username, password);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to MySQL database! (Did you configure it correctly?)", e);
        }
        return connection;
    }

    public String getAddress() {
        return address;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

}

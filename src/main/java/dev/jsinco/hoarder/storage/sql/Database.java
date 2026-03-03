package dev.jsinco.hoarder.storage.sql;

import dev.jsinco.hoarder.Hoarder;
import dev.jsinco.hoarder.manager.FileManager;
import dev.jsinco.hoarder.objects.HoarderPlayer;
import dev.jsinco.hoarder.objects.TreasureItem;
import dev.jsinco.hoarder.storage.DataManager;
import dev.jsinco.hoarder.utilities.BukkitSerialization;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public abstract class Database implements DataManager {

    private String prefix;
    private boolean usingSQLite = false; // SQLite changes some syntax. TODO: Better method for this? I'm not that good with SQL at the time of writing this
    private static final Hoarder plugin = Hoarder.getInstance();

    public Database() {}


    public abstract Connection getConnection();

    protected CompletableFuture<Void> initializeDatabase(boolean usingSQLite) {
        this.prefix = plugin.getConfig().getString("storage.table_prefix");


        return CompletableFuture.runAsync(() -> {
            List<String> initStatements = new ArrayList<>(List.of(
                    "USE " + plugin.getConfig().getString("storage.database") + ";",
                    "CREATE TABLE IF NOT EXISTS " + prefix + "data (event VARCHAR(500) PRIMARY KEY, endtime LONG, material VARCHAR(500), sellprice DECIMAL(15, 2));",
                    "CREATE TABLE IF NOT EXISTS " + prefix + "treasure_items (identifier VARCHAR(3072) PRIMARY KEY, weight INT, itemstack VARCHAR(3072));",
                    "CREATE TABLE IF NOT EXISTS " + prefix + "players (uuid VARCHAR(36) PRIMARY KEY, points INT NOT NULL DEFAULT 0, claimabletreasures INT NOT NULL DEFAULT 0);",
                    "CREATE TABLE IF NOT EXISTS " + prefix + "cache (uuid VARCHAR(36) PRIMARY KEY, position INT);"
            ));

            if (usingSQLite) {
                initStatements.remove(0);
                this.usingSQLite = true;
            }
            try {
                for (String statement : initStatements) {
                    PreparedStatement preparedStatement = getConnection().prepareStatement(statement);
                    preparedStatement.execute();
                    preparedStatement.close();
                }

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", e);
            }
        });
    }


    // Event data


    @Override
    public CompletableFuture<Void> setEventEndTime(long time) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement statement;
                if (!usingSQLite) {
                    statement = getConnection().prepareStatement("INSERT INTO " + prefix + "data (event, endtime) VALUES (?, ?) ON DUPLICATE KEY UPDATE endtime = VALUES(endtime);");
                    statement.setString(1, "main");
                    statement.setLong(2, time);
                } else {
                    statement = getConnection().prepareStatement("UPDATE " + prefix + "data SET endtime = ?;");
                    statement.setLong(1, time);
                }


                statement.executeUpdate();
                statement.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to set event end time in database", e);
            }
        });
    }


    @Override
    public CompletableFuture<@NotNull Long> getEventEndTime() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PreparedStatement statement = getConnection().prepareStatement("SELECT * FROM " + prefix +"data WHERE event = ?;");
                statement.setString(1, "main");

                ResultSet resultSet = statement.executeQuery();
                if (!resultSet.next()) {
                    statement.close();
                    return -1L;
                }

                long endTime = resultSet.getLong("endtime");
                statement.close();
                return endTime;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get event end time in database", e);
                return -1L;
            }
        });
    }

    @Override
    public CompletableFuture<Void> setEventMaterial(@NotNull Material material) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement statement;
                if (!usingSQLite) {
                    statement = getConnection().prepareStatement("INSERT INTO " + prefix + "data (event, material) VALUES (?, ?) ON DUPLICATE KEY UPDATE material = VALUES(material);");
                } else {
                    statement = getConnection().prepareStatement("INSERT OR REPLACE INTO " + prefix + "data (event, material) VALUES (?, ?);");
                }
                statement.setString(1, "main");
                statement.setString(2, material.name());

                statement.executeUpdate();
                statement.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to set event material in database", e);
            }
        });
    }

    @NotNull
    @Override
    public CompletableFuture<@NotNull Material> getEventMaterial() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PreparedStatement statement = getConnection().prepareStatement("SELECT * FROM " + prefix + "data WHERE event = ?;");
                statement.setString(1, "main");
                ResultSet resultSet = statement.executeQuery();


                if (!resultSet.next()) {
                    statement.close();
                    return Material.AIR;
                }

                Material material = Material.valueOf(resultSet.getString("material"));
                statement.close();
                return material;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get event material in database", e);
                return Material.AIR;
            }
        });
    }

    @Override
    public CompletableFuture<Void> setEventSellPrice(double price) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement statement;
                if (!usingSQLite) {
                    statement = getConnection().prepareStatement("INSERT INTO " + prefix + "data (event, sellprice) VALUES (?, ?) ON DUPLICATE KEY UPDATE sellprice = VALUES(sellprice);");
                    statement.setString(1, "main");
                    statement.setDouble(2, price);
                } else {
                    statement = getConnection().prepareStatement("UPDATE " + prefix + "data SET sellprice = ? WHERE event = ?;");
                    statement.setDouble(1, price);
                    statement.setString(2, "main");
                }

                statement.executeUpdate();
                statement.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to set event sell price in database", e);
            }
        });
    }


    @Override
    public CompletableFuture<Double> getEventSellPrice() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PreparedStatement statement = getConnection().prepareStatement("SELECT sellprice FROM " + prefix + "data WHERE event = ?;");
                statement.setString(1, "main");
                ResultSet resultSet = statement.executeQuery();

                if (!resultSet.next()) {
                    statement.close();
                    return 0D;
                }

                double sellPrice = resultSet.getDouble("sellprice");
                statement.close();
                return sellPrice;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get event sell price in database", e);
                return 0D;
            }
        });
    }


    // Hoarder players


    @Override
    public CompletableFuture<Void> addPoints(@NotNull String uuid, int amount) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement statement;
                if (!usingSQLite) {
                    statement = getConnection().prepareStatement("INSERT INTO " + prefix + "players (uuid, points) VALUES (?, ?) ON DUPLICATE KEY UPDATE points = points + VALUES(points);");
                    statement.setInt(2, amount);
                } else {
                    statement = getConnection().prepareStatement("INSERT OR REPLACE INTO " + prefix + "players (uuid, points, claimabletreasures) VALUES (?, COALESCE((SELECT points FROM " + prefix + "players WHERE uuid = ?), 0) + ?, COALESCE((SELECT claimabletreasures FROM " + prefix + "players WHERE uuid = ?), 0));");
                    statement.setString(2, uuid);
                    statement.setInt(3, amount);
                    statement.setString(4, uuid);
                }
                statement.setString(1, uuid);



                statement.executeUpdate();
                statement.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to add points to database", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> removePoints(@NotNull String uuid, int amount) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement statement;

                if (!usingSQLite) {
                    statement = getConnection().prepareStatement("INSERT INTO " + prefix + "players (uuid, points) VALUES (?, ?) ON DUPLICATE KEY UPDATE points = points - VALUES(points);");
                    statement.setInt(2, amount);
                } else {
                    statement = getConnection().prepareStatement("INSERT OR REPLACE INTO " + prefix + "players (uuid, points, claimabletreasures) VALUES (?, COALESCE((SELECT points FROM " + prefix + "players WHERE uuid = ?), 0) - ?, COALESCE((SELECT claimabletreasures FROM " + prefix + "players WHERE uuid = ?), 0));");
                    statement.setString(2, uuid);
                    statement.setInt(3, amount);
                    statement.setString(4, uuid);
                }
                statement.setString(1, uuid);

                statement.executeUpdate();
                statement.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to remove points from database", e);
            }
        });
    }

    @Override
    public CompletableFuture<Integer> getPoints(@NotNull String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PreparedStatement statement = getConnection().prepareStatement("SELECT points FROM " + prefix +"players WHERE uuid = ?;");
                statement.setString(1, uuid);
                ResultSet resultSet = statement.executeQuery();

                if (!resultSet.next()) {
                    statement.close();
                    return 0;
                }

                int points = resultSet.getInt("points");

                statement.close();
                return points;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get points from database", e);
                return 0;
            }
        });
    }

    @Override
    public CompletableFuture<Void> setPoints(@NotNull String uuid, int amount) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement statement;
                if (usingSQLite) {
                    statement = getConnection().prepareStatement("INSERT OR REPLACE INTO " + prefix + "players (uuid, points, claimabletreasures) VALUES (?, ?, COALESCE((SELECT claimabletreasures FROM " + prefix + "players WHERE uuid = ?), 0));");
                    statement.setString(3, uuid);
                } else {
                    statement = getConnection().prepareStatement("INSERT INTO " + prefix + "players (uuid, points) VALUES (?, ?) ON DUPLICATE KEY UPDATE points = VALUES(points);");
                }
                statement.setString(1, uuid);
                statement.setInt(2, amount);

                statement.executeUpdate();
                statement.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to set points in database", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> addClaimableTreasures(@NotNull String uuid, int amount) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement statement;
                if (!usingSQLite) {
                    statement = getConnection().prepareStatement("INSERT INTO " + prefix + "players (uuid, claimabletreasures) VALUES (?, ?) ON DUPLICATE KEY UPDATE claimabletreasures = claimabletreasures + VALUES(claimabletreasures);");
                    statement.setInt(2, amount);
                } else {
                    statement = getConnection().prepareStatement("INSERT OR REPLACE INTO " + prefix + "players (uuid, points, claimabletreasures) VALUES (?, COALESCE((SELECT points FROM " + prefix + "players WHERE uuid = ?), 0), COALESCE((SELECT claimabletreasures FROM " + prefix + "players WHERE uuid = ?), 0) + ?);");
                    statement.setString(2, uuid);
                    statement.setString(3, uuid);
                    statement.setInt(4, amount);
                }
                statement.setString(1, uuid);

                statement.executeUpdate();
                statement.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to add claimable treasures to database", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> removeClaimableTreasures(@NotNull String uuid, int amount) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement statement;
                if (!usingSQLite) {
                    statement = getConnection().prepareStatement("INSERT INTO " + prefix + "players (uuid, claimabletreasures) VALUES (?, ?) ON DUPLICATE KEY UPDATE claimabletreasures = claimabletreasures - VALUES(claimabletreasures);");
                    statement.setInt(2, amount);
                } else {
                    statement = getConnection().prepareStatement("INSERT OR REPLACE INTO " + prefix + "players (uuid, points, claimabletreasures) VALUES (?, COALESCE((SELECT points FROM " + prefix + "players WHERE uuid = ?), 0), COALESCE((SELECT claimabletreasures FROM " + prefix + "players WHERE uuid = ?), 0) - ?);");
                    statement.setString(2, uuid);
                    statement.setString(3, uuid);
                    statement.setInt(4, amount);
                }
                statement.setString(1, uuid);

                statement.executeUpdate();
                statement.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to remove claimable treasures from database", e);
            }
        });
    }

    @Override
    public CompletableFuture<Integer> getClaimableTreasures(@NotNull String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PreparedStatement statement = getConnection().prepareStatement("SELECT claimabletreasures FROM " + prefix + "players WHERE uuid = ?;");
                statement.setString(1, uuid);
                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    int ct = resultSet.getInt("claimabletreasures");
                    statement.close();
                    return ct;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get claimable treasures from database", e);
            }
            return 0;
        });
    }

    @Override
    public CompletableFuture<Void> setClaimableTreasures(@NotNull String uuid, int amount) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement statement;
                if (usingSQLite) {
                    statement = getConnection().prepareStatement("INSERT OR REPLACE INTO " + prefix + "players (uuid, points, claimabletreasures) VALUES (?, COALESCE((SELECT points FROM " + prefix + "players WHERE uuid = ?), 0), ?);");
                    statement.setString(2, uuid);
                } else {
                    statement = getConnection().prepareStatement("INSERT INTO " + prefix + "players (uuid, claimabletreasures) VALUES (?, ?) ON DUPLICATE KEY UPDATE claimabletreasures = VALUES(claimabletreasures);");
                }
                statement.setString(1, uuid);
                statement.setInt(2, amount);

                statement.executeUpdate();
                statement.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to set claimable treasures in database", e);
            }
        });
    }

    // Event necessities
    @Override
    public CompletableFuture<Void> resetAllPoints() {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement statement = getConnection().prepareStatement("SELECT uuid FROM "+prefix+"players;");
                ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    PreparedStatement statement2 = getConnection().prepareStatement("UPDATE "+prefix+"players SET points = 0 WHERE uuid = ?;");
                    statement2.setString(1, resultSet.getString("uuid"));
                    statement2.executeUpdate();
                    statement2.close();
                }
                statement.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to reset all points in database", e);
            }
        });
    }

    @Override
    public CompletableFuture<@NotNull Map<String, Integer>> getEventPlayers() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PreparedStatement statement = getConnection().prepareStatement("SELECT uuid, points FROM "+prefix+"players;");
                ResultSet resultSet = statement.executeQuery();

                Map<String, Integer> eventPlayers = new HashMap<>();

                while (resultSet.next()) {
                    int points = resultSet.getInt("points");
                    if (points != 0) {
                        eventPlayers.put(resultSet.getString("uuid"), points);
                    }
                }
                statement.close();
                return eventPlayers;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get event players from database", e);
            }
            return Collections.emptyMap();
        });
    }

    @Override
    public CompletableFuture<@NotNull List<String>> getAllHoarderPlayersUUIDS() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PreparedStatement statement = getConnection().prepareStatement("SELECT uuid FROM " + prefix + "players;");
                ResultSet resultSet = statement.executeQuery();

                List<String> hoarderPlayers = new ArrayList<>();

                while (resultSet.next()) {
                    hoarderPlayers.add(resultSet.getString("uuid"));
                }
                statement.close();
                return hoarderPlayers;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get all hoarder players from database", e);
            }
            return Collections.emptyList();
        });
    }

    @NotNull
    @Override
    public CompletableFuture<List<HoarderPlayer>> getAllHoarderPlayers() {
        return CompletableFuture.supplyAsync(() -> {
            List<HoarderPlayer> hoarderPlayers = new ArrayList<>();
            for (String uuid : getAllHoarderPlayersUUIDS().join()) {
                hoarderPlayers.add(new HoarderPlayer(uuid));
            }
            return hoarderPlayers;
        });
    }

    // Treasure


    @Override
    public CompletableFuture<Void> addTreasureItem(@NotNull TreasureItem treasureItem) {
        return CompletableFuture.runAsync(() -> addTreasureItem(treasureItem.getIdentifier(), treasureItem.getWeight(), treasureItem.getItemStack()).join());
    }

    @Override
    public CompletableFuture<Void> addTreasureItem(@NotNull String identifier, int weight, ItemStack itemStack) {
        return CompletableFuture.runAsync(() -> {
            String serialized = BukkitSerialization.itemStackToBase64(itemStack);

            try {
                PreparedStatement statement;
                if (usingSQLite) {
                    statement = getConnection().prepareStatement("INSERT OR IGNORE INTO " + prefix +"treasure_items(identifier, weight, itemstack) VALUES (?, ?, ?);");
                } else {
                    statement = getConnection().prepareStatement("INSERT IGNORE INTO " + prefix +"treasure_items(identifier, weight, itemstack) VALUES (?, ?, ?);");
                }
                statement.setString(1, identifier);
                statement.setInt(2, weight);
                statement.setString(3, serialized);

                statement.executeUpdate();
                statement.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to add treasure item to database", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> modifyTreasureItem(String identifier, int newWeight, String newidentifier) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement statement = getConnection().prepareStatement("UPDATE " + prefix + "treasure_items SET identifier = ?, weight = ? WHERE identifier = ?;");
                statement.setString(1, newidentifier);
                statement.setInt(2, newWeight);
                statement.setString(3, identifier);
                statement.executeUpdate();
                statement.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to modify treasure item in database", e);
            }
        });
    }


    @Override
    public CompletableFuture<Void> removeTreasureItem(@NotNull String identifier) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement statement = getConnection().prepareStatement("DELETE FROM " + prefix + "treasure_items WHERE identifier = ?");
                statement.setString(1, identifier);
                statement.executeUpdate();
                statement.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to remove treasure item from database", e);
            }
        });
    }

    @Override
    public CompletableFuture<TreasureItem> getTreasureItem(@NotNull String identifier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PreparedStatement statement = getConnection().prepareStatement("SELECT * FROM " + prefix + "treasure_items WHERE identifier = ?;");
                statement.setString(1, identifier);
                ResultSet resultSet = statement.executeQuery();

                if (!resultSet.next()) {
                    statement.close();
                    return null;
                }

                String id = resultSet.getString("identifier");
                int weight = resultSet.getInt("weight");
                ItemStack itemStack = BukkitSerialization.itemStackFromBase64(resultSet.getString("itemstack"));

                statement.close();
                return new TreasureItem(id, weight, itemStack);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get treasure item from database", e);
            }
            return null;
        });
    }

    @NotNull
    @Override
    public CompletableFuture<List<TreasureItem>> getAllTreasureItems() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PreparedStatement statement = getConnection().prepareStatement("SELECT * FROM "+prefix+"treasure_items;");
                ResultSet resultSet = statement.executeQuery();

                List<TreasureItem> treasureItems = new ArrayList<>();
                while (resultSet.next()) {
                    String identifier = resultSet.getString("identifier");
                    int weight = resultSet.getInt("weight");
                    ItemStack itemStack = BukkitSerialization.itemStackFromBase64(resultSet.getString("itemstack"));
                    treasureItems.add(new TreasureItem(identifier, weight, itemStack));
                }
                statement.close();
                return treasureItems;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get all treasure items from database", e);
                return Collections.emptyList();
            }
        });
    }

    @Override
    public CompletableFuture<Void> addMsgQueuedPlayer(@NotNull String uuid, int position) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement statement = getConnection().prepareStatement("INSERT INTO " + prefix + "cache (uuid, position) VALUES (?, ?);");
                statement.setString(1, uuid);
                statement.setInt(2, position);
                statement.executeUpdate();
                statement.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to add player to msg queue cache", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> removeMsgQueuedPlayer(@NotNull String uuid) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement statement = getConnection().prepareStatement("DELETE FROM " + prefix + "cache WHERE uuid = ?;");
                statement.setString(1, uuid);
                statement.executeUpdate();
                statement.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to remove player from msg queue cache", e);
            }
        });
    }


    @Override
    public CompletableFuture<Boolean> isMsgQueuedPlayer(@NotNull String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PreparedStatement statement = getConnection().prepareStatement("SELECT 1 FROM " + prefix + "cache WHERE uuid = ?;");
                statement.setString(1, uuid);
                ResultSet resultSet = statement.executeQuery();
                boolean isMsgQueued = resultSet.next();
                statement.close();
                return isMsgQueued;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to check if player is in msg queue cache", e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Integer> getMsgQueuedPlayerPosition(@NotNull String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PreparedStatement statement = getConnection().prepareStatement("SELECT position FROM " + prefix + "cache WHERE uuid = ?;");
                statement.setString(1, uuid);
                ResultSet resultSet = statement.executeQuery();
                int position = resultSet.next() ? resultSet.getInt("position") : -1;
                statement.close();
                return position;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get player position in msg queue cache", e);
            }
            return -1;
        });
    }

    // SQL/File remain synchronous as they expose resources directly

    @NotNull
    @Override
    public Connection getSQLConnection() {
        return getConnection();
    }

    @Override
    public void closeConnection() {
        try {
            getConnection().close();
            plugin.getLogger().log(Level.INFO, "Successfully closed connection to database");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to close connection to database", e);
        }
    }

    @NotNull
    @Override
    public FileManager getFile() {
        throw new UnsupportedOperationException("SQL does not support this method! It is meant for flatfile usage!");
    }

    @Override
    public void saveFile() {
        throw new UnsupportedOperationException("SQL does not support this method! It is meant for flatfile usage!");
    }


}

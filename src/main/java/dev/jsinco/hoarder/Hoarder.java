package dev.jsinco.hoarder;

import dev.jsinco.hoarder.commands.CommandManager;
import dev.jsinco.hoarder.events.Listeners;
import dev.jsinco.hoarder.manager.FileManager;
import dev.jsinco.hoarder.manager.Settings;
import dev.jsinco.hoarder.objects.LangMsg;
import dev.jsinco.hoarder.papi.PAPIManager;
import dev.jsinco.hoarder.utilities.Executors;
import dev.jsinco.hoarder.utilities.UpdateChecker;
import dev.jsinco.hoarder.utilities.configupdater.ConfigUpdater;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Level;

public final class Hoarder extends JavaPlugin {

    private static Hoarder plugin;
    private static PAPIManager papiManager;
    private static boolean usePapi;
    private static String latestVersion;

    /*
    TODO: What's left to do 12/6/2023
    - Debug and test everything
    - expand api
    - add placeholderapi to gui items and lang messages
    - remove config updater
    */

    @Override
    public void onEnable() {
        plugin = this;
        FileManager.generateDefaultFiles(); // Generate default files, and Hoarder data folder
        usePapi = getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;


        Settings.INSTANCE.reloadDataManager();
        // Register commands and events
        getCommand("hoarder").setExecutor(new CommandManager(this));
        getServer().getPluginManager().registerEvents(new Listeners(this), this);

        // Reload Hoarder event/Start one if not running, register command aliases, register PlaceholderAPI
        HoarderEvent.INSTANCE.reloadHoarderEvent();
        registerCommandAliases();

        if (usePapi) {
            papiManager = new PAPIManager(this);
            papiManager.register();
        }


        // Update config if needed
        FileManager configFile = new FileManager("config.yml");
        YamlConfiguration config = configFile.getFileYaml();
        String version = getDescription().getVersion();
        latestVersion = version;

        if (config.get("config-version") == null || !config.getString("config-version").equals(version)) {
            try {
                config.set("config-version", version);
                configFile.saveFileYaml();

                ConfigUpdater.update(this, "config.yml", configFile.getFile(), List.of("event.winners", "materials"));
                getLogger().info("Successfully updated config.yml to v" + version + "!");
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Failed to update config.yml!", e);
            }
            // Update info.md
            FileManager infoFile = new FileManager("info.md");
            infoFile.overrideFile();
        }

        if (Settings.checkForUpdates()) {
            new UpdateChecker(this, 114065).getVersion(latestVersion -> {
                if (!version.equals(latestVersion)) {
                    Hoarder.latestVersion = latestVersion;
                }
            });

            Executors.globalDelayed(200, t -> {
                if (!version.equals(latestVersion)) {
                    plugin.getLogger().info(String.format(ChatColor.stripColor(new LangMsg("notifications.update-available").getMsgSendSound(null)), latestVersion));
                }
            });
        }
    }

    @Override
    public void onDisable() {
        // Close connection to database if using MySQL or SQLite
        Settings.getDataManger().closeConnection();
        // Unregister PlaceholderAPI
        if (usePapi) papiManager.unregister();
    }

    public static Hoarder getInstance() {
        return plugin;
    }

    public static boolean getPlaceHolderAPIStatus() {
        return usePapi;
    }

    public static String getLatestVersion() {
        return latestVersion;
    }


    private void registerCommandAliases() {
        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);

            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
            PluginCommand pluginCommand = getCommand("hoarder");

            short total = 0;
            for (String alias : Settings.commandAliases()) {
                commandMap.register(alias.toLowerCase(), "hoarder", pluginCommand);
                total++;
            }
            if (total > 0) getLogger().info("Successfully registered " + total + " command aliases");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            getLogger().log(Level.SEVERE, "Failed to register command aliases!", e);
        }
    }
}

package dev.jsinco.hoarder.manager

import dev.jsinco.hoarder.Hoarder
import dev.jsinco.hoarder.economy.ProviderType
import dev.jsinco.hoarder.storage.DataManager
import dev.jsinco.hoarder.storage.StorageType
import dev.jsinco.hoarder.storage.sql.Database
import dev.jsinco.hoarder.storage.sql.MySQL
import dev.jsinco.hoarder.storage.sql.SQLite
import org.bukkit.Bukkit
import org.bukkit.Material


object Settings {

    val plugin: Hoarder = Hoarder.getInstance()
    private var dataManager: DataManager? = null



    // EVENT WINNERS
    // position, num of prizes
    fun getWinners(): Map<Int, Int> {
        val winners: MutableMap<Int, Int> = mutableMapOf()
        for (position in plugin.config.getConfigurationSection("event.winners")?.getKeys(false)!!) {
            winners[position.toInt()] = plugin.config.getInt("event.winners.$position")
        }
        return winners
    }


    // EVENT MATERIALS

    fun getMaterialPrice(material: Material): Double {
        return plugin.config.getDouble("materials.${material.name.lowercase()}")
    }

    fun getAllMaterials(): List<Material> {
        val materials = mutableListOf<Material>()
        plugin.config.getConfigurationSection("materials")?.getKeys(false)?.forEach {
            materials.add(Material.valueOf(it.uppercase()))
        }
        return materials
    }

    fun useWhiteListAsBlackList(): Boolean {
        return plugin.config.getBoolean("materials.use-material-list-as-blacklist")
    }


    // ECONOMY

    fun usingEconomy(): Boolean {
        return plugin.config.getBoolean("economy.enabled")
    }

    fun useRandomPricing(): Boolean {
        return plugin.config.getBoolean("economy.random-pricing.enabled") || useWhiteListAsBlackList()
    }

    fun randomPricingBounds(): Pair<Double, Double> {
        val min = plugin.config.getDouble("economy.random-pricing.min")
        val max = plugin.config.getDouble("economy.random-pricing.max")
        return Pair(min, max)
    }

    fun getEconomyProvider(): ProviderType {
        return ProviderType.valueOf(plugin.config.getString("economy.provider")?.uppercase() ?: "VAULT")
    }

    // EVENT TIMING


    fun getEventTimerLength(): Long {
        return plugin.config.getLong("event.timer-length")
    }

    fun getEndTimeInterval(): Long {
        if (plugin.config.get("event.timer-check-interval") == null) {
            plugin.config.set("event.timer-check-interval", 600)
            plugin.saveConfig()
        }
        return plugin.config.getLong("event.timer-check-interval")
    }

    // TREASURE

    fun autoIdentify(): Boolean {
        return plugin.config.getBoolean("treasure-items.auto-identifier")
    }

    fun treasureBoundInt(): Int {
        return plugin.config.getInt("treasure-items.bound-int")
    }

    // MISC

    @JvmStatic fun commandAliases(): List<String> {
        return plugin.config.getStringList("commands.command-aliases")
    }

    fun defaultCommandAction(): String {
        return plugin.config.getString("commands.default-command-action") ?: "NONE"
    }

    fun hideIfPageNotAvailable(): Boolean {
        return plugin.config.getBoolean("gui-options.hide-if-page-not-available")
    }

    fun langFileName(): String {
        return "lang/${plugin.config.getString("lang")}.yml"
    }

    @JvmStatic fun checkForUpdates(): Boolean {
        return plugin.config.getBoolean("update-checking.enabled")
    }

    fun notifyOnAvailableUpdate(): Boolean {
        return plugin.config.getBoolean("update-checking.notify-on-join")
    }

    // STORAGE

    @JvmStatic
    fun getStorageType(): StorageType {
        return StorageType.valueOf(plugin.config.getString("storage.type")?.uppercase() ?: "SQLITE")
    }

    @JvmStatic
    fun getDataManger(): DataManager {
        if (dataManager == null) {
            reloadDataManager()
        }
        return dataManager!!
    }

    fun reloadDataManager() {
        if (dataManager != null && dataManager is Database) {
            dataManager?.closeConnection()
        }

        dataManager = when (getStorageType()) {
            StorageType.MYSQL -> MySQL(plugin)
            StorageType.SQLITE -> SQLite(plugin)
            StorageType.FLATFILE -> {
                plugin.logger.warning("Flatfile storage is no longer supported. Using SQLite instead.")
                SQLite(plugin)
            }
        }
        plugin.logger.info("Loading ${getStorageType()} as storage type...")
    }
}
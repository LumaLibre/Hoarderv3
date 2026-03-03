package dev.jsinco.hoarder.storage

import dev.jsinco.hoarder.Hoarder
import dev.jsinco.hoarder.manager.Settings

class DataMigrater (
    private val newStorageType: StorageType,
){
    companion object {
        val plugin: Hoarder = Hoarder.getInstance()
    }
    // TODO: hoarder cache
    private var dataManager: DataManager = Settings.getDataManger()

    private val hoarderPlayersFuture = dataManager.getAllHoarderPlayers()
    private val treasureItemsFuture = dataManager.getAllTreasureItems()
    private val eventEndTimeFuture = dataManager.getEventEndTime()
    private val activeMaterialFuture = dataManager.getEventMaterial()
    private val activeSellPriceFuture = dataManager.getEventSellPrice()

    init {
        if (newStorageType == StorageType.MYSQL) {
            if (plugin.config.getString("storage.address") == "null" ||
                plugin.config.getString("storage.username") == "null" ||
                plugin.config.getString("storage.password") == "null") {
                throw Exception("MySQL credentials not set in config")
            }
        }
    }

    fun migrate() {
        // Quick method to set new storage type in config
        plugin.config.set("storage.type", newStorageType.name)
        plugin.saveConfig()
        plugin.reloadConfig()

        Settings.reloadDataManager()
        dataManager = Settings.getDataManger()

        // Migrate event data
        eventEndTimeFuture.thenAccept { eventEndTime ->
            dataManager.setEventEndTime(eventEndTime)
        }
        activeMaterialFuture.thenAccept { activeMaterial ->
            dataManager.setEventMaterial(activeMaterial)
        }
        if (Settings.usingEconomy()) {
            activeSellPriceFuture.thenAccept { activeSellPrice ->
                dataManager.setEventSellPrice(activeSellPrice)
            }
        }

        // Migrate player data
        // TODO: /shrug
        hoarderPlayersFuture.thenAccept { hoarderPlayers ->
            for (hoarderPlayer in hoarderPlayers) {
                hoarderPlayer.queryPoints().thenAccept { points ->
                    dataManager.addPoints(hoarderPlayer.uuid, points)
                }
                dataManager.getClaimableTreasures(hoarderPlayer.uuid).thenAccept { claimableTreasures ->
                    dataManager.addClaimableTreasures(hoarderPlayer.uuid, claimableTreasures)
                }
            }
        }

        treasureItemsFuture.thenAccept { treasureItems ->
            if (treasureItems != null) {
                // Migrate treasure data
                for (treasureItem in treasureItems) {
                    dataManager.addTreasureItem(treasureItem)
                }
            }
        }
    }
}
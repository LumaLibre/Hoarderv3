package dev.jsinco.hoarder.storage

import dev.jsinco.hoarder.manager.FileManager
import dev.jsinco.hoarder.objects.HoarderPlayer
import dev.jsinco.hoarder.objects.TreasureItem
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.sql.Connection
import java.util.concurrent.CompletableFuture

interface DataManager {

    // Event data

    /**
     * @param time The end time of the event in milliseconds
     */
    fun setEventEndTime(time: Long): CompletableFuture<Void>

    /**
     * @return The end time of the event in milliseconds
     */
    fun getEventEndTime(): CompletableFuture<Long>

    /**
     * @param material Sets the current event material
     */
    fun setEventMaterial(material: Material): CompletableFuture<Void>


    /**
     * @return The current event material
     */
    fun getEventMaterial(): CompletableFuture<Material>


    /**
     * @param price sets the current event sell price
     */
    fun setEventSellPrice(price: Double): CompletableFuture<Void>


    /**
     * @return The current event sell price
     */
    fun getEventSellPrice(): CompletableFuture<Double>

    // Hoarder Players
    // TODO: Label these functions

    fun addPoints(uuid: String, amount: Int): CompletableFuture<Void>

    fun removePoints(uuid: String, amount: Int): CompletableFuture<Void>

    fun getPoints(uuid: String): CompletableFuture<Int>

    fun setPoints(uuid: String, amount: Int): CompletableFuture<Void>

    fun addClaimableTreasures(uuid: String, amount: Int): CompletableFuture<Void>

    fun removeClaimableTreasures(uuid: String, amount: Int): CompletableFuture<Void>

    fun getClaimableTreasures(uuid: String): CompletableFuture<Int>

    fun setClaimableTreasures(uuid: String, amount: Int): CompletableFuture<Void>

    fun resetAllPoints(): CompletableFuture<Void>

    fun getEventPlayers(): CompletableFuture<Map<String, Int>>

    /**
     * @return A list of all HoarderPlayer objects
     */
    fun getAllHoarderPlayersUUIDS(): CompletableFuture<List<String>>

    fun getAllHoarderPlayers(): CompletableFuture<List<HoarderPlayer>>


    // Treasure items

    /**
     * @param treasureItem The TreasureItem object to add or update to the database
     */
    fun addTreasureItem(treasureItem: TreasureItem): CompletableFuture<Void>

    /**
     * @param identifier The identifier of the TreasureItem to remove from the database
     * @param weight The weight of the TreasureItem to remove from the database
     * @param itemStack The itemStack of the TreasureItem to remove from the database
     */
    fun addTreasureItem(identifier: String, weight: Int, itemStack: ItemStack): CompletableFuture<Void>

    fun modifyTreasureItem(identifier: String, newWeight: Int, newIdentifier: String): CompletableFuture<Void>

    /**
     * @param identifier The identifier of the TreasureItem to remove from the database
     */
    fun removeTreasureItem(identifier: String): CompletableFuture<Void>

    /**
     * @param identifier The identifier of the TreasureItem to get
     */
    fun getTreasureItem(identifier: String): CompletableFuture<TreasureItem?>

    /**
     * @return A list of all TreasureItem objects
     */
    fun getAllTreasureItems(): CompletableFuture<List<TreasureItem>>


    // Message Queued Players
    fun addMsgQueuedPlayer(uuid: String, position: Int): CompletableFuture<Void>

    fun removeMsgQueuedPlayer(uuid: String): CompletableFuture<Void>

    fun isMsgQueuedPlayer(uuid: String): CompletableFuture<Boolean>

    fun getMsgQueuedPlayerPosition(uuid: String): CompletableFuture<Int>

    // SQL / File

    fun getSQLConnection(): Connection

    fun getFile(): FileManager

    fun closeConnection()

    fun saveFile()
}
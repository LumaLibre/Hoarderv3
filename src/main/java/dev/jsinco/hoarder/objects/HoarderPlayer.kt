package dev.jsinco.hoarder.objects

import dev.jsinco.hoarder.utilities.Util
import dev.jsinco.hoarder.storage.DataManager
import dev.jsinco.hoarder.manager.Settings
import dev.jsinco.hoarder.utilities.sync
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.random.Random

/**
 * Hoarder representation of a player that can interact with the database
 * @param uuid The UUID of the player
 */
class HoarderPlayer (val uuid: String) {

    companion object {
        val dataManager: DataManager = Settings.getDataManger()
    }

    private var points: Int = 0

    fun addPoints(amount: Int) {
        dataManager.addPoints(uuid, amount)
        points += amount
    }

    fun removePoints(amount: Int) {
        dataManager.removePoints(uuid, amount)
        points -= amount
    }

    fun getPoints(): Int {
        return points
    }

    fun queryPoints(): CompletableFuture<Int> {
        return dataManager.getPoints(uuid)
            .whenComplete { points, _ ->
                this.points = points
            }
    }


    fun addClaimableTreasures(amount: Int) {
        dataManager.addClaimableTreasures(uuid, amount)
    }

    fun removeClaimableTreasures(amount: Int) {
        dataManager.removeClaimableTreasures(uuid, amount)
    }

    // Etc

    fun getPlayer(): Player? {
        val offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid))
        return if (offlinePlayer.isOnline) offlinePlayer.player else null
    }

    fun getOfflinePlayer(): OfflinePlayer {
        return Bukkit.getOfflinePlayer(UUID.fromString(uuid))
    }

    fun getName(): String {
        return Bukkit.getOfflinePlayer(UUID.fromString(uuid)).name ?: "Unknown"
    }

    fun claimTreasure(amount: Int) {
        val player = getOfflinePlayer().player ?: return
        val treasuresFuture = dataManager.getAllTreasureItems()
        treasuresFuture.thenAccept { treasures ->
            if (treasures.isEmpty()) {
                LangMsg("actions.treasure-claim-none").sendMessage(player)
                return@thenAccept
            }

            dataManager.getClaimableTreasures(uuid).thenAccept { claimable ->
                dataManager.removeClaimableTreasures(uuid, amount).thenAccept { _ ->
                    for (i in 0 until amount) {
                        if (claimable <= 0) return@thenAccept

                        var item: ItemStack? = null
                        while (item == null) {
                            val treasureItem = treasures.random()

                            val bound = Settings.treasureBoundInt()

                            if (treasureItem.weight >= bound || treasureItem.weight <= Random.nextInt(bound + 1)) {
                                item = treasureItem.itemStack.clone()
                            }
                        }
                        player.sync {
                            Util.giveItem(player, item)
                        }
                    }

                    if (amount == 1) {
                        LangMsg("actions.treasure-claim").sendMessage(player)
                    } else {
                        player.sendMessage(LangMsg("actions.treasure-claim-multiple").getMsgSendSound(player).format(amount.toString()))
                    }
                }
            }
        }.exceptionally {
            it.printStackTrace()
            null
        }
    }
}
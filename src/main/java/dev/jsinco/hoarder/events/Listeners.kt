package dev.jsinco.hoarder.events

import dev.jsinco.hoarder.Hoarder
import dev.jsinco.hoarder.gui.GUICreator
import dev.jsinco.hoarder.gui.enums.Action
import dev.jsinco.hoarder.manager.Settings
import dev.jsinco.hoarder.objects.LangMsg
import dev.jsinco.hoarder.utilities.syncDelayed
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.persistence.PersistentDataType

class Listeners(private val plugin: Hoarder) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.inventory.holder !is GUICreator) return
        event.isCancelled = true

        val clickedItem = event.currentItem ?: return
        val actionString = clickedItem.itemMeta?.persistentDataContainer?.get(NamespacedKey(plugin, "action"), PersistentDataType.STRING) ?: return

        if (actionString == "NONE") return
        val parsedAction = Action.parseStringAction(actionString)

        parsedAction.first.executeAction(parsedAction.second, event.whoClicked as Player, event)
    }


    @EventHandler(priority = EventPriority.MONITOR)
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.inventory.holder !is GUICreator) return
        val player = event.player as Player

        // TODO: precheck doesnt work
        var preCheck = false
        player.syncDelayed(1) {
             if (player.openInventory.topInventory.holder is GUICreator) preCheck = true
        }

        if (preCheck) return
        val guiCreator = event.inventory.holder as GUICreator

        if (guiCreator.guiRunnable != null) {
            guiCreator.guiRunnable!!.cancel()
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (!player.hasPermission("hoarder.notify")) return

        val dataManager = Settings.getDataManger()
        dataManager.isMsgQueuedPlayer(player.uniqueId.toString()).thenAccept {bool ->
            if (bool) {
                player.syncDelayed(25) {
                    if (!player.isOnline) return@syncDelayed
                    dataManager.getMsgQueuedPlayerPosition(player.uniqueId.toString()).thenAccept { position ->
                        player.sendMessage(LangMsg("notifications.hoarder-event-won").getMsgSendSound(player).format(position.toString()))
                        dataManager.removeMsgQueuedPlayer(player.uniqueId.toString())
                    }
                }
            }
        }

        dataManager.getClaimableTreasures(player.uniqueId.toString()).thenAccept { claimableTreasures ->
            if (claimableTreasures != null && claimableTreasures > 0) {
                player.syncDelayed(25) {
                    if (!player.isOnline) return@syncDelayed
                    player.sendMessage(LangMsg("notifications.claimable-treasures").getMsgSendSound(player).format(claimableTreasures.toString()))
                }
            }
        }

        if (Hoarder.getLatestVersion() != plugin.description.version && Settings.notifyOnAvailableUpdate()) {
            player.syncDelayed(25) {
                if (!player.isOnline || !player.hasPermission("hoarder.update")) return@syncDelayed
                player.sendMessage(LangMsg("notifications.update-available").getMsgSendSound(player).format(Hoarder.getLatestVersion()))
            }
        }
    }
}
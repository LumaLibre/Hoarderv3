package dev.jsinco.hoarder.gui

import dev.jsinco.hoarder.Hoarder
import dev.jsinco.hoarder.manager.Settings
import dev.jsinco.hoarder.utilities.Executors
import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory
import java.util.concurrent.TimeUnit

/**
 * Class intended to update gui items that do not update constantly off a runnable
 */
class GUIUpdater (guiCreator: GUICreator) {


    val file = guiCreator.file
    val gui: Inventory = guiCreator.gui

    init {
        val itemsList: MutableList<GUIItem> = mutableListOf()

        val itemKeyPaths = file.getConfigurationSection("items")!!.getKeys(false)
        for (itemKey in itemKeyPaths) {
            itemsList.add(GUIItem(file, itemKey))
        }

        val guis = mutableListOf<Inventory>()

        guiCreator.paginatedGUI.thenAccept { result ->
            Executors.global {
                if (result != null) {
                    guis.addAll(result.pages)
                } else {
                    guis.add(gui)
                }
                for ((index, inv) in guis.withIndex()) {
                    for (guiItem in itemsList) {
                        if (Settings.hideIfPageNotAvailable() && result != null && (index == 0 || index == guis.size - 1)) {
                            if (guiItem.getAction() == "[BACK_PAGE]" && index == 0) continue
                            if (guiItem.getAction() == "[NEXT_PAGE]" && index == guis.size - 1) continue
                        }

                        if (guiItem.multiSlotted) {
                            for (slot in guiItem.getSlots()) {
                                guiItem.getItemStack().thenAccept { inv.setItem(slot, it) }
                            }
                        } else {
                            guiItem.getItemStack().thenAccept { inv.setItem(guiItem.getSlot(), it) }
                        }
                    }
                }
            }
        }.exceptionally {
            it.printStackTrace()
            null
        }
    }

    /*
    companion object {
        fun hideItemsIfPageNotAvailable(guiCreator: GUICreator) {
            val paginatedGUI = guiCreator.paginatedGUI ?: return


            paginatedGUI.getPage(0)
        }
    }
     */
}
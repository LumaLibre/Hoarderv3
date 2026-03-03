package dev.jsinco.hoarder.utilities

import com.iridium.iridiumcolorapi.IridiumColorAPI
import dev.jsinco.hoarder.HoarderEvent
import dev.jsinco.hoarder.manager.Settings
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.*
import java.util.concurrent.CompletableFuture

object Util {

    private const val WITH_DELIMITER = "((?<=%1\$s)|(?=%1\$s))"


    // Text coloring


    /**
     * @param text The string of text to apply color/effects to
     * @return Returns a string of text with color/effects applied
     */
    @JvmStatic
    fun colorcode(text: String): String {
        val texts = text.split(String.format(WITH_DELIMITER, "&").toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        val finalText = StringBuilder()
        var i = 0
        while (i < texts.size) {
            if (texts[i].equals("&", ignoreCase = true)) {
                //get the next string
                i++
                if (texts[i][0] == '#') {
                    finalText.append(net.md_5.bungee.api.ChatColor.of(texts[i].substring(0, 7)).toString() + texts[i].substring(7))
                } else {
                    finalText.append(ChatColor.translateAlternateColorCodes('&', "&" + texts[i]))
                }
            } else {
                finalText.append(texts[i])
            }
            i++
        }
        return finalText.toString()
    }
    @JvmStatic
    fun colorcode(list: List<String>): List<String> {
        val coloredList: MutableList<String> = ArrayList()
        for (string in list) {
            coloredList.add(colorcode(string))
        }
        return coloredList
    }
    @JvmStatic
    fun fullColor(string: String): String {
        return IridiumColorAPI.process(colorcode(string))
    }
    @JvmStatic
    fun fullColor(list: List<String>): List<String> {
        return colorcode(list).map { IridiumColorAPI.process(it) }
    }


    fun giveItem(player: Player, item: ItemStack) {
        for (i in 0..35) {
            if (player.inventory.getItem(i) == null || player.inventory.getItem(i)!!.isSimilar(item)) {
                player.inventory.addItem(item)
                break
            } else if (i == 35) {
                player.world.dropItem(player.location, item)
            }
        }
    }

    fun listFilesForFolder(folder: File): List<File> {
        if (!folder.exists() || !folder.isDirectory) return listOf()
        val files: MutableList<File> = mutableListOf()

        for (fileEntry in folder.listFiles()!!) {
            if (fileEntry.isDirectory()) {
                files.addAll(listFilesForFolder(fileEntry))
            } else {
                files.add(fileEntry)
            }
        }
        return files
    }

    fun formatMaterialName(material: Material): String {
        var name = material.toString().lowercase(Locale.getDefault()).replace("_", " ")
        name = name.substring(0, 1).uppercase(Locale.getDefault()) + name.substring(1)
        for (i in name.indices) {
            if (name[i] == ' ') {
                name =
                    name.substring(0, i) + " " + name[i + 1].toString().uppercase(Locale.getDefault()) + name.substring(
                        i + 2
                    ) // Capitalize first letter of each word
            }
        }
        return name
    }

    fun generateItemIdentifier(itemStack: ItemStack): String {
        val name: String =
        if (itemStack.hasItemMeta() && itemStack.itemMeta!!.hasDisplayName()) {
            ChatColor.stripColor(itemStack.itemMeta!!.displayName)!!.lowercase().replace(" ", "_")
        } else {
            itemStack.type.name.lowercase()
        }

        return "$name-${itemStack.amount}"
    }


    // General event stuff


    fun replaceTopPlayerPlaceholders(string: String?, eventPlayers: Map<String, Int>): String? {
        if (string == null) return null
        val num = string.substring(string.indexOf("%top_") + 5, string.indexOf("%top_") + 6).toIntOrNull() ?: return string
        if (eventPlayers.size < num) return null
        val uuid = eventPlayers.keys.toList()[num - 1]

        return string.replace("%top_${num}_name%", Bukkit.getOfflinePlayer(UUID.fromString(uuid)).name ?: "Unknown")
            .replace("%top_${num}_points%", eventPlayers.values.toList()[num - 1].toString())
            .replace("%top_${num}_uuid%", uuid)
    }

    fun getEventPlayersByTop(): CompletableFuture<Map<String, Int>> {
        return Settings.getDataManger()
            .getEventPlayers()
            .thenApply { eventPlayers ->
                eventPlayers
                    .toList()
                    .sortedByDescending { (_, value) -> value }
                    .toMap()
            }
    }

    fun formatEconAmt(amount: Double): String {
        if (amount % 1 == 0.0) return String.format("%,d", amount.toInt())
        return String.format("%,.2f", amount)
    }
}
package dev.jsinco.hoarder.papi.placeholders

import dev.jsinco.hoarder.Hoarder
import dev.jsinco.hoarder.manager.Settings
import dev.jsinco.hoarder.objects.LangMsg
import dev.jsinco.hoarder.papi.Placeholder
import dev.jsinco.hoarder.utilities.Util
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.util.*

class TopPlaceholder : Placeholder {

    override fun onReceivedRequest(plugin: Hoarder, player: OfflinePlayer?, args: List<String>): String? {
        if (args.size < 2) return null

        if (true) return "Unsupported"

        val topPlayers: Map<String, Int> = emptyMap() //Util.getEventPlayersByTop()
        val num = args[1].toIntOrNull() ?: return null

        val uuids = topPlayers.keys.toList()
        val points = topPlayers.values.toList()
        val emptyPos = LangMsg("actions.empty-position").getRawMsg()

        if (args.size == 2 && uuids.size >= num) {
            return ((Bukkit.getOfflinePlayer(UUID.fromString(uuids[num - 1])).name ?: "Unknown") + " - " + points[num - 1].toString())
        } else if (args.size < 3) return emptyPos

        return when(args[2].lowercase()) {
            "name" -> {
                if (uuids.size < num) return emptyPos
                Bukkit.getOfflinePlayer(UUID.fromString(uuids[num - 1])).name ?: "Unknown"
            }

            "points" -> {
                if (points.size < num) return emptyPos
                points[num - 1].toString()
            }

            "uuid" -> {
                if (uuids.size < num) return emptyPos
                uuids[num - 1]
            }

            "claimable" -> {
                if (uuids.size < num) return emptyPos
                Settings.getDataManger().getClaimableTreasures(uuids[num - 1]).toString()
            }

            else -> {
                if (uuids.size < num) return emptyPos
                ((Bukkit.getOfflinePlayer(UUID.fromString(uuids[num - 1])).name ?: "Unknown") + " - " + points[num - 1].toString())
            }
        }
    }
}
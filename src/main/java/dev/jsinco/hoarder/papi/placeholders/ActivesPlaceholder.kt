package dev.jsinco.hoarder.papi.placeholders

import dev.jsinco.hoarder.Hoarder
import dev.jsinco.hoarder.manager.Settings
import dev.jsinco.hoarder.objects.Time
import dev.jsinco.hoarder.papi.Placeholder
import dev.jsinco.hoarder.utilities.Util
import org.bukkit.OfflinePlayer

class ActivesPlaceholder : Placeholder {
    override fun onReceivedRequest(plugin: Hoarder, player: OfflinePlayer?, args: List<String>): String? {
        if (args.size < 2) return null

        return when(args[1].lowercase()) {
            "material" -> {
                return "Unsupported"
                //return Settings.getDataManger().getEventMaterial().name
            }

            "materialformatted" -> {
                return "Unsupported"
                //return Util.formatMaterialName(Settings.getDataManger().getEventMaterial())
            }

            "sellprice" -> {
                return "Unsupported"
                //return Util.formatEconAmt(Settings.getDataManger().getEventSellPrice())
            }

            "time" -> {
                val time = Time()
                return "${time.hours}h ${time.mins}m ${time.secs}s"
            }

            else -> {
                null
            }
        }
    }
}
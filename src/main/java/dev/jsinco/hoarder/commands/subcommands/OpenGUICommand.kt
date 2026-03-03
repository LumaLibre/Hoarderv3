package dev.jsinco.hoarder.commands.subcommands

import dev.jsinco.hoarder.Hoarder
import dev.jsinco.hoarder.utilities.Util
import dev.jsinco.hoarder.commands.SubCommand
import dev.jsinco.hoarder.gui.DynamicItems
import dev.jsinco.hoarder.gui.GUICreator
import dev.jsinco.hoarder.gui.GUIUpdater
import dev.jsinco.hoarder.utilities.sync
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.File

class OpenGUICommand : SubCommand {
    override fun execute(plugin: Hoarder, sender: CommandSender, args: Array<out String>) {
        sender as Player
        if (args.size < 2) return
        val path = "guis/${args[1]}.yml"
        val guiCreator = GUICreator(path)

        DynamicItems(guiCreator).setGuiSpecifics(sender)
        guiCreator.paginatedGUI.thenAccept { paginated ->
            sender.sync {
                if (paginated != null) {
                    GUIUpdater(guiCreator)
                    sender.openInventory(paginated.getPage(0))
                } else {
                    sender.openInventory(guiCreator.inventory)
                }
            }
        }
    }

    override fun tabComplete(plugin: Hoarder, sender: CommandSender, args: Array<out String>): MutableList<String>? {
        val list =  Util.listFilesForFolder(File(plugin.dataFolder, "guis")).map { it.name
            .replace(".yml", "")
        }.toMutableList()
        list.remove("dynamicitems")
        return list
    }

    override fun permission(): String {
        return "hoarder.command.gui"
    }

    override fun playerOnly(): Boolean {
        return true
    }
}
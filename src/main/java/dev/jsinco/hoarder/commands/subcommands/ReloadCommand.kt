package dev.jsinco.hoarder.commands.subcommands

import dev.jsinco.hoarder.Hoarder
import dev.jsinco.hoarder.HoarderEvent
import dev.jsinco.hoarder.commands.SubCommand
import dev.jsinco.hoarder.manager.FileManager
import dev.jsinco.hoarder.manager.Settings
import dev.jsinco.hoarder.objects.LangMsg
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ReloadCommand : SubCommand {
    override fun execute(plugin: Hoarder, sender: CommandSender, args: Array<out String>) {
        plugin.logger.info("Reloading...")
        val startTime = System.currentTimeMillis()

        plugin.reloadConfig()
        Bukkit.getAsyncScheduler().runNow(plugin, {
            Settings.reloadDataManager()
        })
        FileManager.generateDefaultFiles()
        HoarderEvent.reloadHoarderEvent()
        LangMsg.reloadLangFile()
        // TODO: reload etc

        plugin.logger.info("Reload complete")
        sender.sendMessage(LangMsg("commands.reload").getMsgSendSound(sender as? Player).format((System.currentTimeMillis() - startTime).toString()))
    }

    override fun tabComplete(plugin: Hoarder, sender: CommandSender, args: Array<out String>): MutableList<String>? {
        return null
    }

    override fun permission(): String {
        return "hoarder.command.reload"
    }

    override fun playerOnly(): Boolean {
        return false
    }
}
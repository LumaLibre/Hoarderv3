package dev.jsinco.hoarder.objects

import dev.jsinco.hoarder.manager.FileManager
import dev.jsinco.hoarder.manager.Settings
import dev.jsinco.hoarder.utilities.Util
import org.bukkit.Sound
import org.bukkit.entity.Player

class LangMsg(val path: String) {

    companion object {
        var file = FileManager(Settings.langFileName()).generateYamlFile()
        var prefix = Util.fullColor(file.getString("prefix") ?: "")

        fun reloadLangFile() {
            file = FileManager(Settings.langFileName()).generateYamlFile()
            prefix = Util.fullColor(file.getString("prefix") ?: "")
        }
    }

    var message = if (file.getString("$path.message") == null) file.getString(path) else file.getString("$path.message")

    val sound = if (file.getString("$path.sound") != null) file.getString("$path.sound") else null
    val volume: Float = if (file.get("$path.volume") != null) file.getDouble("$path.volume").toFloat() else 1.0f
    val pitch: Float = if (file.get("$path.pitch") != null) file.getDouble("$path.pitch").toFloat() else 0.0f

    fun sendMessage(player: Player) {
        if (sound != null) {
            player.playSound(player.location, Sound.valueOf(sound), volume, pitch)
        }

        if (file.getStringList(path).isNotEmpty()) {
            file.getStringList(path).forEach {
                player.sendMessage(Util.fullColor(prefix + it))
            }
            return
        }

        if (message?.contains("%top_") == true) {
            Util.getEventPlayersByTop().thenAccept { result ->
                message = Util.replaceTopPlayerPlaceholders(message, result ?: return@thenAccept)
                player.sendMessage(Util.fullColor(prefix + message))
            }
        } else {
            player.sendMessage(Util.fullColor(prefix + message))
        }
    }

    fun getMsgSendSound(player: Player?): String {
        if (sound != null && player != null) {
            player.playSound(player.location, Sound.valueOf(sound), volume, pitch)
        }

//        if (message?.contains("%top_") == true) {
//            message = Util.replaceTopPlayerPlaceholders(message, Util.getEventPlayersByTop().join()) // TODO: I just cant be bothered rn
//        }

        return Util.fullColor(prefix + message)
    }

    fun getRawMsg(): String {
        return message ?: ""
    }

    // FIXME
    fun getMsgListSendSound(player: Player?): List<String> {
        if (sound != null && player != null) {
            player.playSound(player.location, Sound.valueOf(sound), volume, pitch)
        }

        return file.getStringList(path).map { Util.fullColor(prefix + it) }
    }

    // FIXME
    fun getMsgListSendSound(players: List<Player>): List<String> {
        if (sound != null) {
            for (player in players) {
                player.playSound(player.location, Sound.valueOf(sound), volume, pitch)
            }
        }
        if (file.getStringList(path).isNotEmpty()) {
            return file.getStringList(path).map { Util.fullColor(prefix + it) }
        }
        return file.getStringList("$path.message").map { Util.fullColor(prefix + it) }
    }
}
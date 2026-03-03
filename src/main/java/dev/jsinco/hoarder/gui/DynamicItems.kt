package dev.jsinco.hoarder.gui

import dev.jsinco.hoarder.Hoarder
import dev.jsinco.hoarder.HoarderEvent
import dev.jsinco.hoarder.utilities.Util
import dev.jsinco.hoarder.gui.enums.GUIType
import dev.jsinco.hoarder.manager.FileManager
import dev.jsinco.hoarder.manager.Settings
import dev.jsinco.hoarder.objects.HoarderPlayer
import dev.jsinco.hoarder.objects.Time
import dev.jsinco.hoarder.utilities.Executors
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class DynamicItems(private val guiCreator: GUICreator) {

    companion object {
        private val plugin: Hoarder = Hoarder.getInstance()
    }

    private val material: Material = HoarderEvent.activeMaterial
    private val sellPrice: Double = HoarderEvent.activeSellPrice
    private val gui = guiCreator.gui
    private val dItemsFile = FileManager("guis/dynamicitems.yml").generateYamlFile()

    // TODO: probably redo/edit this
    fun setGuiSpecifics(player: Player) {

        when (guiCreator.guiType) {
            GUIType.MAIN -> { // FIXME: items need to set placeholders
                // We can use our GuiItem class for these dynamic items
                // TODO: Add runnable for clock


                val activeItem = createItem(
                    Material.valueOf(setMainGUIStrings(dItemsFile.getString("items.active_item.material")!!)),
                    setMainGUIStrings(dItemsFile.getString("items.active_item.name")!!),
                    dItemsFile.getStringList("items.active_item.lore").map { setMainGUIStrings(it) } ,
                    dItemsFile.getBoolean("items.active_item.enchanted"),
                    dItemsFile.getString("items.active_item.action"))

                gui.setItem(dItemsFile.getInt("items.active_item.slot"), activeItem)
                startClockRunnable() // FIXME
                guiCreator.paginatedGUI.complete(null)
            }

            GUIType.TREASURE -> {
                Settings.getDataManger().getAllTreasureItems().thenAccept { treasureItems ->
                    if (treasureItems == null) return@thenAccept

                    val items: MutableList<ItemStack> = mutableListOf()

                    for (treasureItem in treasureItems) {
                        val item = treasureItem.itemStack.clone() // Clone it
                        val meta = item.itemMeta!!

                        val lore = meta.lore ?: emptyList<String?>().toMutableList()
                        for (string in dItemsFile.getStringList("items.treasure.lore")) {
                            lore.add(Util.fullColor(string.replace("%weight%", treasureItem.weight.toString())))
                        }
                        meta.lore = lore
                        item.itemMeta = meta
                        items.add(item)
                    }

                    guiCreator.paginatedGUI.complete(PaginatedGUI(guiCreator.title, gui, items))
                }
            }

            GUIType.STATS -> {
                Util.getEventPlayersByTop().thenAccept { hoarderPlayers ->
                    val hoarderPlayerUUIDS: List<String> = hoarderPlayers.keys.toList()

                    val playerHeads: MutableList<ItemStack> = mutableListOf()

                    for (uuid in hoarderPlayerUUIDS) {
                        val hoarderPlayer = HoarderPlayer(uuid)

                        hoarderPlayer.queryPoints().thenAccept {

                            val statItem = createItem(Material.valueOf(dItemsFile.getString("items.stats.material")!!.uppercase()),
                                setStatsGUIStrings(dItemsFile.getString("items.stats.name")!!, hoarderPlayer, hoarderPlayerUUIDS),
                                dItemsFile.getStringList("items.stats.lore").map { setStatsGUIStrings(it, hoarderPlayer, hoarderPlayerUUIDS) },
                                dItemsFile.getBoolean("items.stats.enchanted"),
                                dItemsFile.getString("items.stats.action") ?: "NONE")

                            playerHeads.add(GUIItem.setPlayerHead(statItem, uuid))

                            guiCreator.paginatedGUI.complete(PaginatedGUI(guiCreator.title, gui, playerHeads))
                        }
                    }
                }

            }
            GUIType.TREASURE_CLAIM -> {
                val materialMatchString = dItemsFile.getString("items.treasure.string-matched-materials") ?: "CONCRETE_POWDER"
                val materials: MutableList<Material> = mutableListOf()

                for (material in Material.entries) {
                    if (material.name.contains(materialMatchString)) materials.add(material)
                }

                Settings.getDataManger().getClaimableTreasures(player.uniqueId.toString()).thenAccept { amt ->
                    val items: MutableList<ItemStack> = mutableListOf()

                    for (i in 0 until amt) {
                        val item = createItem(materials.random(), dItemsFile.getString("items.treasure_claim.name") ?: "", dItemsFile.getStringList("items.treasure_claim.lore"), dItemsFile.getBoolean("items.treasure_claim.enchanted"), dItemsFile.getString("items.treasure_claim.action"))
                        items.add(item)
                    }
                    guiCreator.paginatedGUI.complete(PaginatedGUI(guiCreator.title, gui, items))
                }
            }
            GUIType.OTHER -> {
                guiCreator.paginatedGUI.complete(null)
            }
        }
    }

    fun createItem(material: Material?, name: String?, lore: List<String>, enchanted: Boolean, action:String?): ItemStack {
        val item = ItemStack(material ?: Material.BARRIER)
        val meta = item.itemMeta!!

        meta.setDisplayName(Util.fullColor(name ?: ""))
        meta.lore = Util.fullColor(lore)
        if (enchanted) meta.addEnchant(Enchantment.DURABILITY, 1, true)

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES)
        meta.persistentDataContainer.set(NamespacedKey(plugin, "action"), PersistentDataType.STRING, action ?: "NONE")

        item.itemMeta = meta
        return item
    }


    /**
     * Start the clock runnable for main GUI
     * Expire time 5m
     */
    private fun startClockRunnable() {
        val item = createItem(Material.valueOf(dItemsFile.getString("items.clock.material") ?: "CLOCK"),
            setMainGUIStrings(setClockStrings(dItemsFile.getString("items.clock.name") ?: "")),
            dItemsFile.getStringList("items.clock.lore").map { setMainGUIStrings(setClockStrings(it)) },
            dItemsFile.getBoolean("items.clock.enchanted"),
            dItemsFile.getString("items.clock.action"))
        val meta = item.itemMeta!!

        val expireTime = System.currentTimeMillis() + 300000
        guiCreator.guiRunnable = Executors.globalTimer(1, 20) { self ->
            if (System.currentTimeMillis() >= expireTime) {
                self.cancel()
            }

            meta.setDisplayName(setMainGUIStrings(setClockStrings(dItemsFile.getString("items.clock.name") ?: "")))

            meta.lore = dItemsFile.getStringList("items.clock.lore").map { setMainGUIStrings(setClockStrings(it)) }
            item.itemMeta = meta
            gui.setItem(dItemsFile.getInt("items.clock.slot"), item)
        }
    }

    private fun setClockStrings(string: String): String {
        val time = Time()
        return Util.fullColor(
            string.replace("%hours%", time.hours.toString())
                .replace("%minutes%", time.mins.toString())
                .replace("%seconds%", time.secs.toString())
        )
    }


    private fun setMainGUIStrings(string: String): String {
        return Util.fullColor(
            string.replace("%material%", material.toString())
                .replace("%material_formatted%", Util.formatMaterialName(material))
                .replace("%sell_price%", Util.formatEconAmt(sellPrice))
        )
    }

    private fun setStatsGUIStrings(string: String, hoarderPlayer: HoarderPlayer, hoarderPlayerUUIDS: List<String>): String {
        return Util.fullColor(string
            .replace("%name%", hoarderPlayer.getName())
            .replace("%points%", hoarderPlayer.getPoints().toString())
            .replace("%position%", (hoarderPlayerUUIDS.indexOf(hoarderPlayer.uuid) + 1).toString()))
    }
}
package dev.jsinco.hoarder.gui.enums;

import dev.jsinco.hoarder.gui.DynamicItems;
import dev.jsinco.hoarder.gui.GUICreator;
import dev.jsinco.hoarder.gui.GUIUpdater;
import dev.jsinco.hoarder.gui.PaginatedGUI;
import dev.jsinco.hoarder.manager.SellingManager;
import dev.jsinco.hoarder.objects.HoarderPlayer;
import dev.jsinco.hoarder.utilities.Executors;
import dev.jsinco.hoarder.utilities.SynchronizedExecutors;
import dev.jsinco.hoarder.utilities.Util;
import kotlin.Pair;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

public enum Action {

    OPEN,
    COMMAND,
    CLOSE,
    MESSAGE,
    BACK_PAGE,
    NEXT_PAGE,
    SELL,
    CLAIM;

    public boolean executeAction(String string, Player player, @Nullable InventoryClickEvent inventoryClickEvent) {
        switch (this) {
            case OPEN -> {
                GUICreator guiCreator = new GUICreator(string);
                new DynamicItems(guiCreator).setGuiSpecifics(player);

                guiCreator.getPaginatedGUI().thenAccept(result -> {
                    SynchronizedExecutors.sync(player, () -> {
                        if (result != null) {
                            new GUIUpdater(guiCreator); // Pagination arrows
                            player.openInventory(result.getPage(0));
                        } else {
                            player.openInventory(guiCreator.getInventory());
                        }
                    });
                }).exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });

            }

            case COMMAND -> {
                final String finalString = string;
                Executors.global(() -> {
                    CommandSender sender = Bukkit.getConsoleSender();
                    String stringCopy = finalString;
                    if (stringCopy.contains("-p")) {
                        stringCopy = stringCopy.replace("-p", "").trim();
                        sender = player;
                    }
                    Bukkit.dispatchCommand(sender, stringCopy.replace("%player%", player.getName()));
                });
            }

            case CLOSE -> player.closeInventory();

            case MESSAGE -> player.sendMessage(Util.fullColor(string));

            case BACK_PAGE -> {
                Inventory inv = player.getOpenInventory().getTopInventory();
                GUICreator guiCreator = (GUICreator) inv.getHolder();
                assert guiCreator != null;
                guiCreator.getPaginatedGUI().thenAccept(paginatedGUI -> {
                    if (paginatedGUI == null || paginatedGUI.indexOf(inv) == 0) return;

                    SynchronizedExecutors.sync(player, () -> player.openInventory(paginatedGUI.getPage(paginatedGUI.indexOf(inv) - 1)));
                });
                return true;
            }

            case NEXT_PAGE -> {
                Inventory inv = player.getOpenInventory().getTopInventory();
                GUICreator guiCreator = (GUICreator) inv.getHolder();
                assert guiCreator != null;
                guiCreator.getPaginatedGUI().thenAccept(paginatedGUI -> {
                    if (paginatedGUI == null || paginatedGUI.indexOf(inv) == paginatedGUI.getSize() -1) return;

                    SynchronizedExecutors.sync(player, () -> player.openInventory(paginatedGUI.getPage(paginatedGUI.indexOf(inv) + 1)));
                });
                return true;
            }

            case SELL -> {
                SellingManager sellingManager = new SellingManager(player, player.getInventory());
                sellingManager.sellActiveItem();
                if (player.getOpenInventory().getTopInventory().getHolder() instanceof GUICreator) {
                    new GUIUpdater((GUICreator) player.getOpenInventory().getTopInventory().getHolder());
                }
            }

            case CLAIM -> {
                HoarderPlayer hoarderPlayer = new HoarderPlayer(player.getUniqueId().toString());
                hoarderPlayer.claimTreasure(1);
                if (inventoryClickEvent != null) {
                    inventoryClickEvent.getInventory().setItem(inventoryClickEvent.getSlot(), null);
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    public static Pair<Action, String> parseStringAction(String string) {
        if (!string.contains("[") || !string.contains("]")) return null;

        Action action;
        try {
            action = Action.valueOf(string.substring(string.indexOf('[') + 1, string.indexOf(']')).trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
        String updatedString = string.replace("[" + action.name() + "]", "").trim();
        return new Pair<>(action, updatedString);
    }
}

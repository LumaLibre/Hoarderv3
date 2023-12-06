package dev.jsinco.hoarder.gui.enums;

import dev.jsinco.hoarder.objects.HoarderPlayer;
import dev.jsinco.hoarder.utilities.Messages;
import dev.jsinco.hoarder.utilities.Util;
import dev.jsinco.hoarder.gui.DynamicItems;
import dev.jsinco.hoarder.gui.GUICreator;
import dev.jsinco.hoarder.gui.GUIUpdater;
import dev.jsinco.hoarder.gui.PaginatedGUI;
import dev.jsinco.hoarder.manager.SellingManager;
import kotlin.Pair;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public enum Action {

    OPEN,
    COMMAND,
    CLOSE,
    MESSAGE,
    BACK_PAGE,
    NEXT_PAGE,
    SELL,
    CLAIM;

    public boolean executeAction(String string, Player player) {
        switch (this) {
            case OPEN -> {
                GUICreator guiCreator = new GUICreator(string);
                new DynamicItems(guiCreator).setGuiSpecifics();

                if (guiCreator.getPaginatedGUI() != null) {
                    player.openInventory(guiCreator.getPaginatedGUI().getPage(0));
                } else {
                    player.openInventory(guiCreator.getInventory());
                }
            }

            case COMMAND -> {
                CommandSender sender = Bukkit.getConsoleSender();
                if (string.contains("-p")) {
                    string = string.replace("-p", "").trim();
                    sender = player;
                }
                Bukkit.dispatchCommand(sender, string);
            }

            case CLOSE -> player.closeInventory();

            case MESSAGE -> player.sendMessage(Util.fullColor(string));

            case BACK_PAGE -> {
                GUICreator guiCreator = (GUICreator) player.getOpenInventory().getTopInventory().getHolder();
                PaginatedGUI paginatedGUI = guiCreator.getPaginatedGUI();

                if (paginatedGUI == null) return false;

                player.openInventory(paginatedGUI.getPage(paginatedGUI.indexOf(player.getOpenInventory().getTopInventory()) - 1));
            }

            case NEXT_PAGE -> {
                GUICreator guiCreator = (GUICreator) player.getOpenInventory().getTopInventory().getHolder();
                PaginatedGUI paginatedGUI = guiCreator.getPaginatedGUI();

                if (paginatedGUI == null) return false;

                player.openInventory(paginatedGUI.getPage(paginatedGUI.indexOf(player.getOpenInventory().getTopInventory()) + 1));
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

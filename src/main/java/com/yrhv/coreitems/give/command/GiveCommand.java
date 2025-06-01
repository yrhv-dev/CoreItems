package com.yrhv.coreitems.give.command;

import com.yrhv.coreitems.CoreItems;
import com.yrhv.coreitems.namespace.model.ItemNamespace;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to give custom items to players
 */
public class GiveCommand {
    private final CoreItems plugin;

    public GiveCommand(CoreItems plugin) {
        this.plugin = plugin;
    }

    /**
     * Execute the give command
     * @param sender Command sender
     * @param args Command arguments
     * @return true if command was successful
     */
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /coreitems give <player> <namespace> <item>");
            return true;
        }
        
        if (!sender.hasPermission("coreitems.give")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }
        
        String playerName = args[1];
        String namespaceName = args[2];
        String itemId = args[3];
        
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + playerName);
            return true;
        }
        
        if (plugin.getNamespaceManager().giveItemToPlayer(target, namespaceName, itemId)) {
            // Update player inventory data when an item is given
            plugin.getPlayerDataManager().onItemGive(target);
            
            sender.sendMessage(ChatColor.GREEN + "Given custom item " + namespaceName + ":" + itemId + " to " + target.getName());
            target.sendMessage(ChatColor.GREEN + "You received a custom item: " + namespaceName + ":" + itemId);
        } else {
            sender.sendMessage(ChatColor.RED + "Custom item not found: " + namespaceName + ":" + itemId);
        }
        
        return true;
    }

    /**
     * Tab complete for the give command
     * @param sender Command sender
     * @param args Command arguments
     * @return List of tab completions
     */
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("coreitems.give")) {
            return completions;
        }
        
        if (args.length == 2) {
            // Complete player names
            String partial = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 3) {
            // Complete namespace names
            String partial = args[2].toLowerCase();
            for (String namespace : plugin.getNamespaceManager().getNamespaces().keySet()) {
                if (namespace.toLowerCase().startsWith(partial)) {
                    completions.add(namespace);
                }
            }
        } else if (args.length == 4) {
            // Complete item names within the selected namespace
            String namespaceName = args[2];
            String partial = args[3].toLowerCase();
            
            ItemNamespace namespace = plugin.getNamespaceManager().getNamespace(namespaceName);
            if (namespace != null) {
                for (String item : namespace.getItems().keySet()) {
                    if (item.toLowerCase().startsWith(partial)) {
                        completions.add(item);
                    }
                }
            }
        }
        
        return completions;
    }
}

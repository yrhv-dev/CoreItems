package com.yrhv.coreitems.give.command;

import com.yrhv.coreitems.CoreItems;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to reload the plugin configuration
 */
public class ReloadCommand {
    private final CoreItems plugin;

    public ReloadCommand(CoreItems plugin) {
        this.plugin = plugin;
    }

    /**
     * Execute the reload command
     * @param sender Command sender
     * @param args Command arguments
     * @return true if command was successful
     */
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("coreitems.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }
        
        // Use the new reload method which includes player data reload
        plugin.reload();
        sender.sendMessage(ChatColor.GREEN + "CoreItems configuration and player data reloaded!");
        return true;
    }

    /**
     * Tab complete for the reload command
     * @param sender Command sender
     * @param args Command arguments
     * @return List of tab completions
     */
    public List<String> tabComplete(CommandSender sender, String[] args) {
        // No tab completion needed for reload command
        return new ArrayList<>();
    }
}

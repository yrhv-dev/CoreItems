package com.yrhv.coreitems.give.command;

import com.yrhv.coreitems.CoreItems;
import com.yrhv.coreitems.gui.menu.MainMenu;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Main command handler for the CoreItems plugin
 */
public class CoreItemsCommand implements CommandExecutor, TabCompleter {
    private final CoreItems plugin;
    private final GiveCommand giveCommand;
    private final ReloadCommand reloadCommand;
    private final ListCommand listCommand;

    public CoreItemsCommand(CoreItems plugin) {
        this.plugin = plugin;
        this.giveCommand = new GiveCommand(plugin);
        this.reloadCommand = new ReloadCommand(plugin);
        this.listCommand = new ListCommand(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            // Check permissions
            if (!sender.hasPermission("coreitems.use")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                return true;
            }

            // If no arguments, open menu
            if (args.length == 0) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (sender.hasPermission("coreitems.menu")) {
                        new MainMenu(plugin, player).open();
                    } else {
                        sender.sendMessage(ChatColor.RED + "You don't have permission to use this menu!");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Only players can open the menu!");
                }
                return true;
            }

            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("help")) {
                sendHelpMessage(sender);
                return true;
            } else if (subCommand.equals("menu") || subCommand.equals("gui")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players");
                    return true;
                }

                if (!sender.hasPermission("coreitems.menu")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                    return true;
                }

                Player player = (Player) sender;
                new MainMenu(plugin, player).open();
                return true;
            } else if (subCommand.equals("give")) {
                return giveCommand.execute(sender, args);
            } else if (subCommand.equals("list")) {
                return listCommand.execute(sender, args);
            } else if (subCommand.equals("reload")) {
                return reloadCommand.execute(sender, args);
            }

            sendHelpMessage(sender);
            return true;
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "An error occurred while executing the command: " + e.getMessage());
            e.printStackTrace();
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("coreitems.use")) {
            return new ArrayList<>();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Complete main subcommands
            String partial = args[0].toLowerCase();
            if ("give".startsWith(partial) && sender.hasPermission("coreitems.give")) {
                completions.add("give");
            }
            if ("reload".startsWith(partial) && sender.hasPermission("coreitems.reload")) {
                completions.add("reload");
            }
            if ("list".startsWith(partial)) {
                completions.add("list");
            }
            if ("menu".startsWith(partial) && sender.hasPermission("coreitems.menu")) {
                completions.add("menu");
            }
            if ("help".startsWith(partial)) {
                completions.add("help");
            }
            return completions;
        }

        // Delegate to subcommand handlers
        String subCommand = args[0].toLowerCase();
        if (subCommand.equals("give") && sender.hasPermission("coreitems.give")) {
            return giveCommand.tabComplete(sender, args);
        } else if (subCommand.equals("list")) {
            return listCommand.tabComplete(sender, args);
        }

        return completions;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "===== CoreItems Help =====");
        sender.sendMessage(ChatColor.YELLOW + "/coreitems" + ChatColor.WHITE + " - Open the item menu");

        if (sender.hasPermission("coreitems.give")) {
            sender.sendMessage(ChatColor.YELLOW + "/coreitems give <player> <namespace> <item>" +
                    ChatColor.WHITE + " - Give a custom item");
        }

        sender.sendMessage(ChatColor.YELLOW + "/coreitems list <namespace> [page]" +
                ChatColor.WHITE + " - List all items in a namespace");

        if (sender.hasPermission("coreitems.reload")) {
            sender.sendMessage(ChatColor.YELLOW + "/coreitems reload" +
                    ChatColor.WHITE + " - Reload the plugin");
        }

        if (sender.hasPermission("coreitems.menu")) {
            sender.sendMessage(ChatColor.YELLOW + "/coreitems menu" +
                    ChatColor.WHITE + " - Open the item menu");
        }
    }
}

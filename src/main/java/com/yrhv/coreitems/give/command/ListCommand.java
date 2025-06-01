package com.yrhv.coreitems.give.command;

import com.yrhv.coreitems.CoreItems;
import com.yrhv.coreitems.give.model.CustomItem;
import com.yrhv.coreitems.namespace.model.ItemNamespace;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Command to list all items in a namespace
 */
public class ListCommand {
    private final CoreItems plugin;
    private final int ITEMS_PER_PAGE = 15;

    public ListCommand(CoreItems plugin) {
        this.plugin = plugin;
    }

    /**
     * Execute the list command
     * @param sender Command sender
     * @param args Command arguments
     * @return true if command was successful
     */
    public boolean execute(CommandSender sender, String[] args) {
        // Check if namespace was provided
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Please specify a namespace: /coreitems list <namespace> [page]");
            return true;
        }

        String namespaceName = args[1];

        // Check if the namespace exists
        ItemNamespace namespace = plugin.getNamespaceManager().getNamespace(namespaceName);
        if (namespace == null) {
            sender.sendMessage(ChatColor.RED + "Namespace '" + namespaceName + "' not found!");
            return true;
        }

        // Get all items in the namespace
        Map<String, CustomItem> items = namespace.getItems();
        
        if (items.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No items found in namespace '" + namespaceName + "'!");
            return true;
        }

        // Convert to list for pagination
        List<Map.Entry<String, CustomItem>> itemList = new ArrayList<>(items.entrySet());
        
        // Determine page number
        int page = 1;
        if (args.length >= 3) {
            try {
                page = Integer.parseInt(args[2]);
                if (page < 1) {
                    page = 1;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid page number. Using page 1.");
                page = 1;
            }
        }

        // Calculate total pages
        int totalPages = (int) Math.ceil((double) itemList.size() / ITEMS_PER_PAGE);
        if (page > totalPages) {
            page = totalPages;
        }

        // Calculate start and end index for current page
        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, itemList.size());

        // Display header
        sender.sendMessage(ChatColor.GREEN + "======= " + ChatColor.YELLOW + 
                "Items in namespace '" + namespaceName + "' " + 
                ChatColor.GREEN + "(" + ChatColor.YELLOW + "Page " + page + "/" + totalPages + 
                ChatColor.GREEN + ") =======");

        // Display items for the current page
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<String, CustomItem> entry = itemList.get(i);
            String itemId = entry.getKey();
            CustomItem item = entry.getValue();
            
            // Get the item's display name or use the ID if no name
            String displayName = item.getDisplayName();
            if (displayName == null || displayName.isEmpty()) {
                displayName = itemId;
            }
            
            // Format material name
            String material = item.getMaterial().toString().toLowerCase().replace('_', ' ');
            material = material.substring(0, 1).toUpperCase() + material.substring(1);
            
            sender.sendMessage(ChatColor.YELLOW + itemId + ChatColor.GRAY + " - " + 
                    ChatColor.WHITE + displayName + ChatColor.GRAY + " (" + material + ")");
        }

        // Show navigation help if there are multiple pages
        if (totalPages > 1) {
            if (page < totalPages) {
                sender.sendMessage(ChatColor.GREEN + "Use " + ChatColor.YELLOW + 
                        "/coreitems list " + namespaceName + " " + (page + 1) + 
                        ChatColor.GREEN + " to see the next page.");
            }
            if (page > 1) {
                sender.sendMessage(ChatColor.GREEN + "Use " + ChatColor.YELLOW + 
                        "/coreitems list " + namespaceName + " " + (page - 1) + 
                        ChatColor.GREEN + " to see the previous page.");
            }
        }

        return true;
    }

    /**
     * Tab complete for the list command
     * @param sender Command sender
     * @param args Command arguments
     * @return List of tab completions
     */
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 2) {
            // Complete namespace names
            String partial = args[1].toLowerCase();
            for (String namespace : plugin.getNamespaceManager().getNamespaces().keySet()) {
                if (namespace.toLowerCase().startsWith(partial)) {
                    completions.add(namespace);
                }
            }
        } else if (args.length == 3) {
            // Get namespace to check item count
            String namespaceName = args[1];
            ItemNamespace namespace = plugin.getNamespaceManager().getNamespace(namespaceName);
            
            if (namespace != null) {
                int itemCount = namespace.getItems().size();
                int totalPages = (int) Math.ceil((double) itemCount / ITEMS_PER_PAGE);
                
                // Only suggest page numbers if there are multiple pages
                if (totalPages > 1) {
                    String partial = args[2].toLowerCase();
                    for (int i = 1; i <= totalPages; i++) {
                        String pageNum = String.valueOf(i);
                        if (pageNum.startsWith(partial)) {
                            completions.add(pageNum);
                        }
                    }
                }
            }
        }
        
        return completions;
    }
}

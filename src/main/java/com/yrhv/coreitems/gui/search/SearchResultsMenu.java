package com.yrhv.coreitems.gui.search;

import com.yrhv.coreitems.CoreItems;
import com.yrhv.coreitems.gui.menu.MainMenu;
import com.yrhv.coreitems.gui.menu.Menu;
import com.yrhv.coreitems.gui.menu.NamespaceItemsMenu;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Menu for displaying namespace search results
 */
public class SearchResultsMenu extends Menu {
    
    private final String searchTerm;
    private final List<String> results;
    
    /**
     * Constructor for the search results menu
     * 
     * @param plugin The CoreItems plugin instance
     * @param player The player viewing the menu
     * @param searchTerm The search term entered by the player
     */
    public SearchResultsMenu(CoreItems plugin, Player player, String searchTerm) {
        super(plugin, player);
        this.searchTerm = searchTerm;
        
        // Get namespaces matching the search term
        this.results = plugin.getNamespaceManager().getNamespaceNames().stream()
                .filter(name -> name.toLowerCase().contains(searchTerm.toLowerCase()))
                .collect(Collectors.toList());
    }
    
    @Override
    public String getMenuName() {
        return ChatColor.DARK_PURPLE + "Search Results: " + searchTerm;
    }
    
    @Override
    public int getSlots() {
        return 36; // 4 rows
    }
    
    @Override
    public void handleMenu(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        
        if (event.getCurrentItem() == null) {
            return;
        }
        
        // Back button (center bottom)
        if (slot == 31) {
            new MainMenu(plugin, player).open();
        } 
        // Result item (could be a namespace or "no results" barrier)
        else if (slot == 13 && results.size() == 1) {
            // If there's exactly one result, open that namespace's items
            String namespaceName = results.get(0);
            new NamespaceItemsMenu(plugin, player, namespaceName).open();
        }
        
        event.setCancelled(true);
    }
    
    @Override
    public void setMenuItems() {
        // Fill the entire inventory with the border pattern
        fillBorder();
        
        // Add back button in the bottom center
        inventory.setItem(31, createBackButton());
        
        // Show search results (or lack thereof)
        if (results.isEmpty()) {
            // No matching namespaces found
            ItemStack noResults = createGuiItem(
                Material.BARRIER,
                ChatColor.RED + "No Results Found",
                ChatColor.GRAY + "No namespaces match your search: " + searchTerm,
                ChatColor.YELLOW + "Click the back button to return"
            );
            
            inventory.setItem(13, noResults);
        } else if (results.size() == 1) {
            // Single result - display it in the center
            String namespaceName = results.get(0);
            int itemCount = plugin.getNamespaceManager().getNamespace(namespaceName).getItems().size();
            
            ItemStack result = createGuiItem(
                Material.CHEST,
                ChatColor.GOLD + namespaceName,
                ChatColor.GRAY + "Contains " + itemCount + " custom items",
                ChatColor.YELLOW + "Click to view items"
            );
            
            inventory.setItem(13, result);
        } else {
            // Multiple results - display the number found and instructions
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Found " + results.size() + " matching namespaces:");
            
            // List up to 5 results in the lore
            for (int i = 0; i < Math.min(5, results.size()); i++) {
                lore.add(ChatColor.YELLOW + "• " + results.get(i));
            }
            
            if (results.size() > 5) {
                lore.add(ChatColor.YELLOW + "• ...and " + (results.size() - 5) + " more");
            }
            
            lore.add("");
            lore.add(ChatColor.AQUA + "Return to main menu to browse all namespaces");
            
            ItemStack multipleResults = createGuiItem(
                Material.BOOK,
                ChatColor.GREEN + "Multiple Results Found",
                lore
            );
            
            inventory.setItem(13, multipleResults);
        }
    }
}

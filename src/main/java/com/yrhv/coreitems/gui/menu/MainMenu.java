package com.yrhv.coreitems.gui.menu;

import com.yrhv.coreitems.CoreItems;
import com.yrhv.coreitems.gui.search.NamespaceSearchHandler;
import com.yrhv.coreitems.gui.util.PaginatedMenu;
import com.yrhv.coreitems.namespace.manager.NamespaceManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Main menu GUI for browsing namespaces
 */
public class MainMenu extends PaginatedMenu {
    
    private final NamespaceManager namespaceManager;
    private final List<String> namespaces;
    
    /**
     * Constructor for the main menu
     * 
     * @param plugin The CoreItems plugin instance
     * @param player The player viewing the menu
     */
    public MainMenu(CoreItems plugin, Player player) {
        super(plugin, player);
        this.namespaceManager = plugin.getNamespaceManager();
        this.namespaces = new ArrayList<>(namespaceManager.getNamespaceNames());
    }
    
    @Override
    public String getMenuName() {
        return ChatColor.DARK_PURPLE + "CoreItems: Namespaces";
    }
    
    @Override
    public int getSlots() {
        return 36; // 4 rows
    }
    
    @Override
    public void handleMenu(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        
        // Check if search button was clicked (top middle slot)
        if (slot == 4 && event.getCurrentItem() != null) {
            player.closeInventory();
            // Start search process
            new NamespaceSearchHandler(plugin).promptSearch(player);
            return;
        }
        
        // Check if the clicked slot is a namespace button
        if (event.getCurrentItem() != null && isContentSlot(slot)) {
            int index = slot - getFirstContentSlot() + (page * getMaxItemsPerPage());
            
            if (index >= 0 && index < namespaces.size()) {
                String namespaceName = namespaces.get(index);
                // Open the namespace items menu
                new NamespaceItemsMenu(plugin, player, namespaceName).open();
            }
        } else if (slot == getCloseButtonSlot() && event.getCurrentItem() != null) {
            // Close button
            player.closeInventory();
        } else if (slot == getNextPageButtonSlot() && event.getCurrentItem() != null && hasNextPage()) {
            // Next page button
            page++;
            refreshMenu();
        } else if (slot == getPreviousPageButtonSlot() && event.getCurrentItem() != null && hasPreviousPage()) {
            // Previous page button
            page--;
            refreshMenu();
        }
        
        event.setCancelled(true);
    }
    
    @Override
    public void setMenuItems() {
        fillBorder();
        
        // Add the close button
        inventory.setItem(getCloseButtonSlot(), createCloseButton());
        
        // Add previous page button if needed
        if (hasPreviousPage()) {
            inventory.setItem(getPreviousPageButtonSlot(), createPreviousPageButton());
        }
        
        // Add next page button if needed
        if (hasNextPage()) {
            inventory.setItem(getNextPageButtonSlot(), createNextPageButton());
        }
        
        // Add search button in top middle (slot 4)
        ItemStack searchButton = createGuiItem(
            Material.OAK_SIGN,
            ChatColor.GREEN + "Search Namespaces",
            ChatColor.GRAY + "Click to search for a namespace",
            ChatColor.YELLOW + "Type the name you want to find"
        );
        inventory.setItem(4, searchButton);
        
        // No page indicator in main menu as requested
        
        if (namespaces.isEmpty()) {
            // No namespaces available
            ItemStack noItems = createGuiItem(Material.BARRIER, 
                ChatColor.RED + "No namespaces available", 
                ChatColor.GRAY + "Create a namespace in the customs folder");
            inventory.setItem(getFirstContentSlot(), noItems);
            return;
        }
        
        // Calculate pagination
        int startIndex = page * getMaxItemsPerPage();
        int endIndex = Math.min(startIndex + getMaxItemsPerPage(), namespaces.size());
        
        // Add namespace items
        int slot = getFirstContentSlot();
        for (int i = startIndex; i < endIndex; i++) {
            String namespaceName = namespaces.get(i);
            
            // Get the number of items in this namespace
            int itemCount = 0;
            if (namespaceManager.getNamespace(namespaceName) != null) {
                itemCount = namespaceManager.getNamespace(namespaceName).getItems().size();
            }
            
            // Create the namespace button
            ItemStack namespaceItem = createGuiItem(
                Material.CHEST,
                ChatColor.GOLD + namespaceName,
                ChatColor.GRAY + "Contains " + itemCount + " custom items",
                ChatColor.YELLOW + "Click to view items"
            );
            
            // Calculate the correct slot to keep items within borders
            // Skip to next row when reaching edge of content area
            if (slot % 9 == 8) {
                slot += 2; // Skip the border column and move to next row
            }
            
            inventory.setItem(slot++, namespaceItem);
        }
    }
    
    @Override
    protected int getMaxItemsPerPage() {
        return 14; // 2 rows of items (minus borders)
    }
    
    @Override
    protected int getFirstContentSlot() {
        return 10; // First slot in the second row, after the border (row 1, col 1)
    }
    
    @Override
    protected boolean isContentSlot(int slot) {
        // Check if the slot is in the content area (rows 2-3, excluding borders)
        int row = slot / 9;
        int col = slot % 9;
        
        return (row == 1 || row == 2) && col > 0 && col < 8;
    }
    
    @Override
    protected boolean hasNextPage() {
        return (page + 1) * getMaxItemsPerPage() < namespaces.size();
    }
    
    @Override
    protected boolean hasPreviousPage() {
        return page > 0;
    }
    
    @Override
    protected int getNextPageButtonSlot() {
        return getSlots() - 1; // Bottom right corner
    }
    
    @Override
    protected int getPreviousPageButtonSlot() {
        return getSlots() - 9; // Bottom left corner
    }
    
    @Override
    protected int getCloseButtonSlot() {
        return getSlots() - 5; // Bottom middle
    }
    
    /**
     * Refresh the menu items (for pagination)
     */
    private void refreshMenu() {
        inventory.clear();
        setMenuItems();
    }
}

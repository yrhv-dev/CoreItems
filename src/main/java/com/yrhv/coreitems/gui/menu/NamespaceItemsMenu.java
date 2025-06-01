package com.yrhv.coreitems.gui.menu;

import com.yrhv.coreitems.CoreItems;
import com.yrhv.coreitems.give.model.CustomItem;
import com.yrhv.coreitems.gui.util.PaginatedMenu;
import com.yrhv.coreitems.namespace.model.ItemNamespace;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Menu for displaying and selecting items within a namespace
 */
public class NamespaceItemsMenu extends PaginatedMenu {
    
    private final String namespaceName;
    private final List<Map.Entry<String, CustomItem>> items;
    
    /**
     * Constructor for the namespace items menu
     * 
     * @param plugin The CoreItems plugin instance
     * @param player The player viewing the menu
     * @param namespaceName The name of the namespace to display items from
     */
    public NamespaceItemsMenu(CoreItems plugin, Player player, String namespaceName) {
        super(plugin, player);
        this.namespaceName = namespaceName;
        
        // Load items from the namespace
        ItemNamespace namespace = plugin.getNamespaceManager().getNamespace(namespaceName);
        if (namespace != null) {
            this.items = new ArrayList<>(namespace.getItems().entrySet());
        } else {
            this.items = new ArrayList<>();
        }
    }
    
    @Override
    public String getMenuName() {
        return ChatColor.DARK_PURPLE + "CoreItems: " + namespaceName;
    }
    
    @Override
    public int getSlots() {
        return 45; // 5 rows
    }
    
    @Override
    public void handleMenu(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        
        // Check if the clicked slot is an item button
        if (event.getCurrentItem() != null && isContentSlot(slot)) {
            int index = slot - getFirstContentSlot() + (page * getMaxItemsPerPage());
            
            if (index >= 0 && index < items.size()) {
                Map.Entry<String, CustomItem> entry = items.get(index);
                String itemId = entry.getKey();
                CustomItem customItem = entry.getValue();
                
                // Check for right-click vs left-click
                boolean isRightClick = event.isRightClick();
                
                if (isRightClick) {
                    // Right-click: Give a stack of the item (64)
                    ItemStack stack = customItem.toItemStack().clone();
                    stack.setAmount(64); // Set to a full stack
                    player.getInventory().addItem(stack);
                    
                    player.sendMessage(ChatColor.GREEN + "You received a stack of " + namespaceName + ":" + itemId);
                } else {
                    // Left-click: Give a single item (default behavior)
                    if (plugin.getNamespaceManager().giveItemToPlayer(player, namespaceName, itemId)) {
                        player.sendMessage(ChatColor.GREEN + "You received the item: " + namespaceName + ":" + itemId);
                    } else {
                        player.sendMessage(ChatColor.RED + "Failed to give you the item: " + namespaceName + ":" + itemId);
                    }
                }
                // Don't close inventory or refresh - leave the menu open
            }
        } else if (slot == getBackButtonSlot() && event.getCurrentItem() != null) {
            // Back button - return to main menu
            new MainMenu(plugin, player).open();
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
        
        // Add the back button
        inventory.setItem(getBackButtonSlot(), createBackButton());
        
        // Add next page button if needed
        if (hasNextPage()) {
            inventory.setItem(getNextPageButtonSlot(), createNextPageButton());
        }
        
        // Add previous page button if needed
        if (hasPreviousPage()) {
            inventory.setItem(getPreviousPageButtonSlot(), createPreviousPageButton());
        }
        
        if (items.isEmpty()) {
            // No items available in this namespace
            ItemStack noItems = createGuiItem(Material.BARRIER, 
                ChatColor.RED + "No items available", 
                ChatColor.GRAY + "This namespace has no custom items");
            inventory.setItem(getFirstContentSlot(), noItems);
            return;
        }
        
        // Calculate pagination
        int startIndex = page * getMaxItemsPerPage();
        int endIndex = Math.min(startIndex + getMaxItemsPerPage(), items.size());
        
        // Add item buttons
        int slot = getFirstContentSlot();
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<String, CustomItem> entry = items.get(i);
            // We don't need to use itemId here as we're just showing the item
            CustomItem customItem = entry.getValue();
            
            // Create an actual item stack representation
            ItemStack itemStack = customItem.toItemStack();
            ItemMeta meta = itemStack.getItemMeta();
            
            if (meta != null) {
                // Add additional lore for the GUI
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                if (lore == null) lore = new ArrayList<>();
                
                lore.add("");
                lore.add(ChatColor.YELLOW + "Left-click: Get 1 item");
                lore.add(ChatColor.YELLOW + "Right-click: Get a stack (64)");
                
                meta.setLore(lore);
                itemStack.setItemMeta(meta);
            }
            
            inventory.setItem(slot++, itemStack);
        }
    }
    
    @Override
    protected int getMaxItemsPerPage() {
        return 21; // 3 rows of items (minus borders)
    }
    
    @Override
    protected int getFirstContentSlot() {
        return 10; // First slot in the second row, after the border
    }
    
    @Override
    protected boolean isContentSlot(int slot) {
        // Check if the slot is in the content area (rows 2-4, excluding borders)
        int row = slot / 9;
        int col = slot % 9;
        
        return (row >= 1 && row <= 3) && col > 0 && col < 8;
    }
    
    @Override
    protected boolean hasNextPage() {
        return (page + 1) * getMaxItemsPerPage() < items.size();
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
        return -1; // No close button, we use back button instead
    }
    
    /**
     * Get the slot for the back button
     * 
     * @return The slot index
     */
    protected int getBackButtonSlot() {
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

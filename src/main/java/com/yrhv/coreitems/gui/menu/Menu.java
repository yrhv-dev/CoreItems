package com.yrhv.coreitems.gui.menu;

import com.yrhv.coreitems.CoreItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * Abstract class for creating GUI menus
 */
public abstract class Menu implements InventoryHolder {
    
    // Protected fields
    protected final CoreItems plugin;
    protected final Player player;
    protected Inventory inventory;
    protected int page = 0;
    
    /**
     * Constructor for creating a menu
     * 
     * @param plugin The CoreItems plugin instance
     * @param player The player viewing the menu
     */
    public Menu(CoreItems plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }
    
    /**
     * Open the menu for the player
     */
    public void open() {
        inventory = Bukkit.createInventory(this, getSlots(), getMenuName());
        this.setMenuItems();
        player.openInventory(inventory);
    }
    
    /**
     * Handle a click on the menu
     * 
     * @param event The InventoryClickEvent
     */
    public abstract void handleMenu(InventoryClickEvent event);
    
    /**
     * Set the items in the menu
     */
    public abstract void setMenuItems();
    
    /**
     * Get the menu name
     * 
     * @return The menu name
     */
    public abstract String getMenuName();
    
    /**
     * Get the number of slots in the menu
     * 
     * @return The number of slots
     */
    public abstract int getSlots();
    
    /**
     * Fill the border of the menu with the configured item
     */
    protected void fillBorder() {
        // Get border item from config, default to BLACK_STAINED_GLASS_PANE
        String borderItemStr = plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE");
        
        // Check if border is disabled
        if (borderItemStr.equalsIgnoreCase("NONE")) {
            return;
        }
        
        // Convert string to Material
        Material borderMaterial;
        try {
            borderMaterial = Material.valueOf(borderItemStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid border item in config: " + borderItemStr + ". Using BLACK_STAINED_GLASS_PANE instead.");
            borderMaterial = Material.BLACK_STAINED_GLASS_PANE;
        }
        
        ItemStack filler = createGuiItem(borderMaterial, " ");
        
        int slots = inventory.getSize();
        int rows = slots / 9;
        
        // Fill the top and bottom rows
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, filler); // Top row
            inventory.setItem(slots - 9 + i, filler); // Bottom row
        }
        
        // Fill the first and last columns (excluding corners which are already filled)
        for (int i = 1; i < rows - 1; i++) {
            inventory.setItem(i * 9, filler); // First column
            inventory.setItem(i * 9 + 8, filler); // Last column
        }
    }
    
    /**
     * Create a GUI item with a name
     * 
     * @param material The material of the item
     * @param name The name of the item
     * @return The created ItemStack
     */
    protected ItemStack createGuiItem(Material material, String name) {
        return createGuiItem(material, name, (String[]) null);
    }
    
    /**
     * Create a GUI item with a name and lore
     * 
     * @param material The material of the item
     * @param name The name of the item
     * @param lore The lore of the item
     * @return The created ItemStack
     */
    protected ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            
            if (lore != null) {
                meta.setLore(Arrays.asList(lore));
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Create a GUI item with a name and lore
     * 
     * @param material The material of the item
     * @param name The name of the item
     * @param lore The lore of the item as a List
     * @return The created ItemStack
     */
    protected ItemStack createGuiItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            
            if (lore != null) {
                meta.setLore(lore);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Create a close button
     * 
     * @return The close button ItemStack
     */
    protected ItemStack createCloseButton() {
        return createGuiItem(Material.BARRIER, "§c§lClose", "§7Click to close the menu");
    }
    
    /**
     * Create a back button
     * 
     * @return The back button ItemStack
     */
    protected ItemStack createBackButton() {
        return createGuiItem(Material.BARRIER, "§c§lBack", "§7Click to go back");
    }
    
    /**
     * Create a next page button
     * 
     * @return The next page button ItemStack
     */
    protected ItemStack createNextPageButton() {
        return createGuiItem(Material.ARROW, "§a§lNext Page", "§7Click to go to the next page");
    }
    
    /**
     * Create a previous page button
     * 
     * @return The previous page button ItemStack
     */
    protected ItemStack createPreviousPageButton() {
        return createGuiItem(Material.ARROW, "§a§lPrevious Page", "§7Click to go to the previous page");
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

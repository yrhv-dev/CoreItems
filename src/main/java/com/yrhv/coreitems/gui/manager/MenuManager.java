package com.yrhv.coreitems.gui.manager;

import com.yrhv.coreitems.CoreItems;
import com.yrhv.coreitems.gui.menu.MainMenu;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manager class for handling GUI menus
 */
public class MenuManager {
    
    private final CoreItems plugin;
    private final Map<UUID, InventoryHolder> activeMenus;
    
    /**
     * Constructor for the MenuManager
     * 
     * @param plugin The CoreItems plugin instance
     */
    public MenuManager(CoreItems plugin) {
        this.plugin = plugin;
        this.activeMenus = new HashMap<>();
    }
    
    /**
     * Opens the main items GUI for a player
     * 
     * @param player The player to open the GUI for
     */
    public void openMainMenu(Player player) {
        MainMenu menu = new MainMenu(plugin, player);
        activeMenus.put(player.getUniqueId(), menu);
        menu.open();
    }
    
    /**
     * Registers an active menu for a player
     * 
     * @param player The player viewing the menu
     * @param menu The menu being viewed
     */
    public void registerMenu(Player player, InventoryHolder menu) {
        activeMenus.put(player.getUniqueId(), menu);
    }
    
    /**
     * Unregisters an active menu for a player
     * 
     * @param player The player whose menu to unregister
     */
    public void unregisterMenu(Player player) {
        activeMenus.remove(player.getUniqueId());
    }
    
    /**
     * Gets the active menu for a player
     * 
     * @param player The player to get the menu for
     * @return The active InventoryHolder or null if none exists
     */
    public InventoryHolder getActiveMenu(Player player) {
        return activeMenus.get(player.getUniqueId());
    }
}

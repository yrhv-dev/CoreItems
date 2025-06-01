package com.yrhv.coreitems.gui.util;

import com.yrhv.coreitems.CoreItems;
import com.yrhv.coreitems.gui.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Listener for menu-related events
 */
public class MenuListener implements Listener {
    
    private final CoreItems plugin;
    
    /**
     * Constructor for the menu listener
     * 
     * @param plugin The CoreItems plugin instance
     */
    public MenuListener(CoreItems plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        
        // Check if the inventory holder is one of our custom menus
        if (holder instanceof Menu) {
            Menu menu = (Menu) holder;
            menu.handleMenu(event);
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            
            // Unregister the menu when it's closed
            if (event.getInventory().getHolder() instanceof Menu) {
                plugin.getMenuManager().unregisterMenu(player);
                
                // Scan and save player inventory when they close a menu
                // This replaces per-click scanning for better performance
                plugin.getPlayerDataManager().onGuiItemTake(player);
            }
        }
    }
}

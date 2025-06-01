package com.yrhv.coreitems.gui.util;

import com.yrhv.coreitems.CoreItems;
import com.yrhv.coreitems.gui.menu.Menu;
import org.bukkit.entity.Player;

/**
 * Abstract class for creating paginated menus
 */
public abstract class PaginatedMenu extends Menu {
    
    /**
     * Constructor for creating a paginated menu
     * 
     * @param plugin The CoreItems plugin instance
     * @param player The player viewing the menu
     */
    public PaginatedMenu(CoreItems plugin, Player player) {
        super(plugin, player);
    }
    
    /**
     * Get the maximum number of items per page
     * 
     * @return The maximum number of items per page
     */
    protected abstract int getMaxItemsPerPage();
    
    /**
     * Get the slot index for the first content item
     * 
     * @return The slot index
     */
    protected abstract int getFirstContentSlot();
    
    /**
     * Check if a slot is in the content area of the menu
     * 
     * @param slot The slot to check
     * @return True if the slot is in the content area
     */
    protected abstract boolean isContentSlot(int slot);
    
    /**
     * Check if there is a next page
     * 
     * @return True if there is a next page
     */
    protected abstract boolean hasNextPage();
    
    /**
     * Check if there is a previous page
     * 
     * @return True if there is a previous page
     */
    protected abstract boolean hasPreviousPage();
    
    /**
     * Get the slot for the next page button
     * 
     * @return The slot index
     */
    protected abstract int getNextPageButtonSlot();
    
    /**
     * Get the slot for the previous page button
     * 
     * @return The slot index
     */
    protected abstract int getPreviousPageButtonSlot();
    
    /**
     * Get the slot for the close button
     * 
     * @return The slot index
     */
    protected abstract int getCloseButtonSlot();
}

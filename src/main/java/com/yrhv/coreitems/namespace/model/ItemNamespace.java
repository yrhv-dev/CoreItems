package com.yrhv.coreitems.namespace.model;

import com.yrhv.coreitems.give.model.CustomItem;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a namespace for custom items
 */
public class ItemNamespace {
    
    private final String name;
    private final File configFile;
    private final Map<String, CustomItem> items;
    
    /**
     * Creates a new item namespace
     * 
     * @param name The name of the namespace
     * @param configFile The configuration file for this namespace
     */
    public ItemNamespace(String name, File configFile) {
        this.name = name;
        this.configFile = configFile;
        this.items = new HashMap<>();
    }
    
    /**
     * Gets the name of this namespace
     * 
     * @return The namespace name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the configuration file for this namespace
     * 
     * @return The configuration file
     */
    public File getConfigFile() {
        return configFile;
    }
    
    /**
     * Gets all items in this namespace
     * 
     * @return Map of item ID to CustomItem
     */
    public Map<String, CustomItem> getItems() {
        return Collections.unmodifiableMap(items);
    }
    
    /**
     * Gets an item from this namespace by its ID
     * 
     * @param itemId The item ID
     * @return The CustomItem or null if not found
     */
    public CustomItem getItem(String itemId) {
        return items.get(itemId.toLowerCase());
    }
    
    /**
     * Adds an item to this namespace
     * 
     * @param itemId The item ID
     * @param item The CustomItem
     */
    public void addItem(String itemId, CustomItem item) {
        items.put(itemId.toLowerCase(), item);
    }
    
    /**
     * Clears all items from this namespace
     */
    public void clearItems() {
        items.clear();
    }
    
    /**
     * Returns the full item identifier including namespace
     * 
     * @param itemId The item ID
     * @return The full identifier (namespace:itemId)
     */
    public String getFullItemId(String itemId) {
        return name + ":" + itemId;
    }
    
    /**
     * Checks if this namespace contains an item
     * 
     * @param itemId The item ID
     * @return True if the item exists in this namespace
     */
    public boolean hasItem(String itemId) {
        return items.containsKey(itemId.toLowerCase());
    }
}

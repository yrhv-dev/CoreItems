package com.yrhv.coreitems.namespace.manager;

import com.yrhv.coreitems.give.model.CustomItem;
import com.yrhv.coreitems.namespace.model.ItemNamespace;
import com.yrhv.coreitems.namespace.util.NamespaceLoader;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Manages all item namespaces
 */
public class NamespaceManager {
    
    private final JavaPlugin plugin;
    private final File namespacesDir;
    private final Map<String, ItemNamespace> namespaces;
    private final NamespaceLoader namespaceLoader;
    
    /**
     * Creates a new namespace manager
     * 
     * @param plugin The JavaPlugin instance
     */
    public NamespaceManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.namespacesDir = new File(plugin.getDataFolder(), "customs");
        this.namespaces = new HashMap<>();
        this.namespaceLoader = new NamespaceLoader(plugin);
        
        // Ensure directory exists
        if (!namespacesDir.exists()) {
            namespacesDir.mkdirs();
        }
    }
    
    /**
     * Loads all available namespaces
     */
    public void loadNamespaces() {
        // Clear any existing namespaces
        namespaces.clear();
        
        // Load all namespace directories
        File[] dirs = namespacesDir.listFiles(File::isDirectory);
        if (dirs == null || dirs.length == 0) {
            // Create default namespace if none exist
            createDefaultNamespace();
            return;
        }
        
        // Load each namespace
        for (File dir : dirs) {
            String namespaceName = dir.getName();
            File configFile = new File(dir, "customs.yml");
            
            if (!configFile.exists()) {
                plugin.getLogger().warning("Namespace directory '" + namespaceName + "' does not contain a customs.yml file");
                continue;
            }
            
            try {
                ItemNamespace namespace = new ItemNamespace(namespaceName, configFile);
                namespaceLoader.loadNamespace(namespace);
                namespaces.put(namespaceName.toLowerCase(), namespace);
                plugin.getLogger().info("Loaded namespace: " + namespaceName + " with " + namespace.getItems().size() + " items");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load namespace: " + namespaceName, e);
            }
        }
        
        plugin.getLogger().info("Loaded " + namespaces.size() + " item namespaces");
    }
    
    /**
     * Creates a default namespace with example items if no namespaces exist
     */
    private void createDefaultNamespace() {
        File defaultDir = new File(namespacesDir, "default");
        defaultDir.mkdirs();
        
        try {
            namespaceLoader.createDefaultNamespace(defaultDir);
            loadNamespaces(); // Reload to pick up the default namespace
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create default namespace", e);
        }
    }
    
    /**
     * Gets all available namespaces
     * 
     * @return Set of namespace names
     */
    public Set<String> getNamespaceNames() {
        return Collections.unmodifiableSet(namespaces.keySet());
    }
    
    /**
     * Gets a namespace by name
     * 
     * @param namespaceName The namespace name
     * @return The namespace, or null if it doesn't exist
     */
    public ItemNamespace getNamespace(String namespaceName) {
        return namespaces.get(namespaceName.toLowerCase());
    }
    
    /**
     * Gets all namespaces
     * 
     * @return Map of namespace names to namespaces
     */
    public Map<String, ItemNamespace> getNamespaces() {
        return Collections.unmodifiableMap(namespaces);
    }
    
    /**
     * Gets an item by its namespace and ID
     * 
     * @param namespaceName The namespace name
     * @param itemId The item ID within that namespace
     * @return The CustomItem or null if not found
     */
    public CustomItem getItem(String namespaceName, String itemId) {
        ItemNamespace namespace = getNamespace(namespaceName);
        if (namespace == null) {
            return null;
        }
        
        return namespace.getItem(itemId);
    }
    
    /**
     * Gives a custom item to a player
     * 
     * @param player The player to receive the item
     * @param namespaceName The namespace name
     * @param itemId The item ID
     * @return true if successful, false if item not found
     */
    public boolean giveItemToPlayer(Player player, String namespaceName, String itemId) {
        CustomItem customItem = getItem(namespaceName, itemId);
        if (customItem == null) {
            return false;
        }
        
        ItemStack itemStack = customItem.toItemStack();
        player.getInventory().addItem(itemStack);
        return true;
    }
}

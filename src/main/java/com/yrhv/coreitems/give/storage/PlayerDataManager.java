package com.yrhv.coreitems.give.storage;

import com.yrhv.coreitems.CoreItems;
import com.yrhv.coreitems.give.model.CustomItem;
import com.yrhv.coreitems.namespace.manager.NamespaceManager;
import com.yrhv.coreitems.namespace.model.ItemNamespace;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages the storage and retrieval of player inventory custom item data
 */
public class PlayerDataManager {
    private final CoreItems plugin;
    private final NamespaceManager namespaceManager;
    private final File playerDataFile;
    private FileConfiguration playerDataConfig;
    private BukkitTask autoScanTask;
    
    // Runtime player item inventory data: UUID -> ItemId -> Count
    private final Map<UUID, Map<String, Integer>> playerItemInventory = new ConcurrentHashMap<>();
    
    // Config values
    private boolean enabled;
    private boolean autoScanEnabled;
    private long autoScanInterval;
    
    public PlayerDataManager(CoreItems plugin, NamespaceManager namespaceManager) {
        this.plugin = plugin;
        this.namespaceManager = namespaceManager;
        this.playerDataFile = new File(plugin.getDataFolder(), "player_items.yml");
        loadConfig();
        
        if (enabled) {
            loadPlayerData();
            
            if (autoScanEnabled) {
                startAutoScanTask();
            }
        }
    }
    
    /**
     * Load configuration values from config.yml
     */
    public void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        enabled = config.getBoolean("player-data.enabled", true);
        autoScanEnabled = config.getBoolean("player-data.auto_scan.enabled", true);
        autoScanInterval = config.getLong("player-data.auto_scan.interval", 300000); // Default 5 minutes
    }
    
    /**
     * Loads player item inventory data from the player_items.yml file
     */
    public void loadPlayerData() {
        if (!playerDataFile.exists()) {
            // Create default file if it doesn't exist
            plugin.saveResource("player_items.yml", false);
        }
        
        playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);
        
        // Clear existing data
        playerItemInventory.clear();
        
        // Load all player data
        for (String uuidString : playerDataConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                ConfigurationSection playerSection = playerDataConfig.getConfigurationSection(uuidString);
                
                if (playerSection != null) {
                    Map<String, Integer> itemCounts = new ConcurrentHashMap<>();
                    
                    for (String itemId : playerSection.getKeys(false)) {
                        int amount = playerSection.getInt(itemId);
                        if (amount > 0) {
                            itemCounts.put(itemId, amount);
                        }
                    }
                    
                    if (!itemCounts.isEmpty()) {
                        playerItemInventory.put(uuid, itemCounts);
                    }
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in player_items.yml: " + uuidString);
            }
        }
        
        plugin.getLogger().info("Loaded item inventory data for " + playerItemInventory.size() + " players");
    }
    
    /**
     * Saves current player inventory data to the player_items.yml file
     */
    public void savePlayerData() {
        if (!enabled) return;
        
        playerDataConfig = new YamlConfiguration();
        
        // Save all player data
        for (Map.Entry<UUID, Map<String, Integer>> entry : playerItemInventory.entrySet()) {
            UUID uuid = entry.getKey();
            Map<String, Integer> itemCounts = entry.getValue();
            
            for (Map.Entry<String, Integer> itemEntry : itemCounts.entrySet()) {
                String itemId = itemEntry.getKey();
                int amount = itemEntry.getValue();
                
                if (amount > 0) {
                    String path = uuid.toString() + "." + itemId;
                    playerDataConfig.set(path, amount);
                }
            }
        }
        
        try {
            playerDataConfig.save(playerDataFile);
            plugin.getLogger().info("Saved item inventory data for " + playerItemInventory.size() + " players");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player data to " + playerDataFile, e);
        }
    }
    
    /**
     * Start the automatic scan task to update player inventory data
     */
    private void startAutoScanTask() {
        if (autoScanTask != null) {
            autoScanTask.cancel();
        }
        
        autoScanTask = new BukkitRunnable() {
            @Override
            public void run() {
                scanAllOnlinePlayers();
                savePlayerData();
            }
        }.runTaskTimer(plugin, autoScanInterval / 50, autoScanInterval / 50); // Convert ms to ticks
        
        plugin.getLogger().info("Started auto-scan task for player inventory data (interval: " + autoScanInterval + "ms)");
    }
    
    /**
     * Stop the automatic scan task
     */
    public void stopAutoScanTask() {
        if (autoScanTask != null) {
            autoScanTask.cancel();
            autoScanTask = null;
        }
    }
    
    /**
     * Scan a player's inventory for custom items and update the item counts
     * @param player The player to scan
     */
    public void scanPlayerInventory(Player player) {
        if (!enabled || player == null) return;
        
        UUID playerId = player.getUniqueId();
        Map<String, Integer> itemCounts = new ConcurrentHashMap<>();
        
        // Scan all items in player's inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            
            CustomItem customItem = findMatchingCustomItem(item);
            if (customItem != null) {
                String itemId = customItem.getId();
                int currentCount = itemCounts.getOrDefault(itemId, 0);
                itemCounts.put(itemId, currentCount + item.getAmount());
            }
        }
        
        // Update player's item inventory data
        if (itemCounts.isEmpty()) {
            playerItemInventory.remove(playerId);
        } else {
            playerItemInventory.put(playerId, itemCounts);
        }
    }
    
    /**
     * Scan all online players' inventories
     */
    public void scanAllOnlinePlayers() {
        if (!enabled) return;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            scanPlayerInventory(player);
        }
    }
    
    /**
     * Get the item count for a player and item
     * @param playerId The player UUID
     * @param itemId The item ID
     * @return The item count or 0 if none
     */
    public int getItemCount(UUID playerId, String itemId) {
        Map<String, Integer> itemCounts = playerItemInventory.get(playerId);
        if (itemCounts == null) return 0;
        
        return itemCounts.getOrDefault(itemId, 0);
    }
    
    /**
     * Get all item counts for a player
     * @param playerId The player UUID
     * @return Map of item IDs to counts, or empty map if none
     */
    public Map<String, Integer> getAllItemCounts(UUID playerId) {
        return playerItemInventory.getOrDefault(playerId, new ConcurrentHashMap<>());
    }
    
    /**
     * Find a matching custom item from all namespaces
     * @param item The item stack to match
     * @return The matching custom item or null if none found
     */
    private CustomItem findMatchingCustomItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        
        for (String namespaceName : namespaceManager.getNamespaceNames()) {
            ItemNamespace namespace = namespaceManager.getNamespace(namespaceName);
            if (namespace == null) continue;
            
            for (CustomItem customItem : namespace.getItems().values()) {
                if (isSimilarItem(item, customItem)) {
                    return customItem;
                }
            }
        }
        return null;
    }
    
    /**
     * Checks if an item is similar to a custom item
     * This matches material, display name, and custom model data if present
     */
    private boolean isSimilarItem(ItemStack item, CustomItem customItem) {
        if (item.getType() != customItem.getMaterial()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        // Check display name if the custom item has one
        if (customItem.getDisplayName() != null) {
            if (!meta.hasDisplayName() || !meta.getDisplayName().equals(customItem.getDisplayName())) {
                return false;
            }
        }
        
        // Check custom model data if the custom item has it
        if (customItem.getCustomModelData() != -1) {
            if (!meta.hasCustomModelData() || meta.getCustomModelData() != customItem.getCustomModelData()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Called when the CoreItems reload command is executed
     * This rescans all online players and updates the player_items.yml file
     */
    public void onReload() {
        loadConfig(); // Reload configuration values
        scanAllOnlinePlayers();
        savePlayerData();
        
        // Restart auto-scan task if needed
        if (enabled) {
            if (autoScanEnabled) {
                stopAutoScanTask();
                startAutoScanTask();
            }
        } else {
            stopAutoScanTask();
        }
    }
    
    /**
     * Called when a player receives an item via the give command
     * @param player The player who received an item
     */
    public void onItemGive(Player player) {
        if (!enabled || player == null) return;
        scanPlayerInventory(player);
        savePlayerData();
    }
    
    /**
     * Called when a player takes an item from the GUI
     * @param player The player who took an item
     */
    public void onGuiItemTake(Player player) {
        if (!enabled || player == null) return;
        scanPlayerInventory(player);
        savePlayerData();
    }
    
    /**
     * Shutdown method to save data and cleanup
     */
    public void shutdown() {
        stopAutoScanTask();
        if (enabled) {
            scanAllOnlinePlayers();
            savePlayerData();
        }
    }
}

package com.yrhv.coreitems.give.listener;

import com.yrhv.coreitems.CoreItems;
import com.yrhv.coreitems.give.model.CommandProperties;
import com.yrhv.coreitems.give.model.CustomItem;
import com.yrhv.coreitems.give.storage.PlayerDataManager;
import com.yrhv.coreitems.namespace.manager.NamespaceManager;
import com.yrhv.coreitems.namespace.model.ItemNamespace;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Listener for custom item interactions
 */
public class CustomItemListener implements Listener {
    private final CoreItems plugin;
    private final NamespaceManager namespaceManager;
    private final PlayerDataManager playerDataManager;
    
    // Constants for command identifiers
    private static final String RIGHT_CLICK_ID = "right";
    private static final String LEFT_CLICK_ID = "left";
    private static final String ITEM_COOLDOWN_ID = "item";  // For item-wide cooldown
    
    // In-memory cooldown data (not persisted)
    // UUID -> ItemId -> CommandId -> Expiration Time
    private final Map<UUID, Map<String, Map<String, Long>>> cooldownData = new ConcurrentHashMap<>();
    
    // In-memory message time data (not persisted)
    // UUID -> ItemId -> CommandId -> Last Message Time
    private final Map<UUID, Map<String, Map<String, Long>>> lastMessageData = new ConcurrentHashMap<>();
    
    public CustomItemListener(CoreItems plugin, NamespaceManager namespaceManager, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.namespaceManager = namespaceManager;
        this.playerDataManager = playerDataManager;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // Check if player is holding an item
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        
        // Get global cooldown settings from config
        long globalCooldown = plugin.getConfig().getLong("item-interactions.global-cooldown", 500);
        boolean globalMessagesEnabled = plugin.getConfig().getBoolean("item-interactions.cooldown-message-enabled", false);
        String globalCooldownMessage = plugin.getConfig().getString("item-interactions.cooldown-message", "&cThis item is on cooldown!");
        int globalMessageInterval = plugin.getConfig().getInt("item-interactions.cooldown-message-interval", 4);
        globalCooldownMessage = ChatColor.translateAlternateColorCodes('&', globalCooldownMessage);
        
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Find the matching custom item
        CustomItem customItem = findMatchingCustomItem(item);
        if (customItem == null) {
            return; // Not a custom item
        }
        
        // Get the action for command ID determination
        Action action = event.getAction();
        String commandId = determineCommandId(action);
        
        // Check if player is on cooldown for this action
        String itemId = customItem.getId();
        Long cooldownExpireTime = getCooldownExpirationTime(playerUUID, itemId, commandId);
        
        // Also check global item cooldown as a fallback
        if (cooldownExpireTime == null) {
            cooldownExpireTime = getCooldownExpirationTime(playerUUID, itemId, ITEM_COOLDOWN_ID);
        }
        
        if (cooldownExpireTime != null && cooldownExpireTime > currentTime) {
            // Calculate remaining cooldown time (for future use if needed)
            @SuppressWarnings("unused")
            long remainingCooldown = cooldownExpireTime - currentTime;
            
            // We're on cooldown, check if we should send a message
            CommandProperties cmdProps = null;
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                cmdProps = customItem.getRightClickProperties();
            } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                cmdProps = customItem.getLeftClickProperties();
            }
            
            // Get cooldown message from command properties, item, or global setting
            String cooldownMessage = null;
            
            if (cmdProps != null) {
                cooldownMessage = cmdProps.getCooldownMessage();
                // Store interval for later use in effective interval calculation
            }
            
            // Fall back to item-level settings if command doesn't have specific settings
            if (cooldownMessage == null) {
                cooldownMessage = customItem.getCooldownMessage();
            }
            
            // Determine if we should show a message
            boolean shouldShowMessage = globalMessagesEnabled || cooldownMessage != null;
            
            if (shouldShowMessage) {
                // Calculate effective cooldown and interval
                long effectiveCooldown;
                int effectiveInterval;
                
                if (cmdProps != null && cmdProps.getCooldown() > 0) {
                    effectiveCooldown = cmdProps.getEffectiveCooldown(globalCooldown);
                    effectiveInterval = cmdProps.getEffectiveCooldownMessageInterval(globalMessageInterval);
                } else {
                    effectiveCooldown = customItem.getEffectiveCooldown(globalCooldown);
                    effectiveInterval = customItem.getEffectiveCooldownMessageInterval(globalMessageInterval);
                }
                
                long messageInterval = Math.max(effectiveCooldown / Math.max(effectiveInterval, 1), 1);
                
                // Check if it's time to send another message
                Long lastMessageTime = getLastMessageTime(playerUUID, itemId, commandId);
                if (lastMessageTime == null) {
                    lastMessageTime = getLastMessageTime(playerUUID, itemId, ITEM_COOLDOWN_ID);
                }
                lastMessageTime = lastMessageTime != null ? lastMessageTime : 0L;
                
                if (currentTime - lastMessageTime >= messageInterval) {
                    // Fall back to global message if no specific message is defined
                    if (cooldownMessage == null) {
                        cooldownMessage = globalCooldownMessage;
                    }
                    
                    // Replace %remaining% placeholder with actual time
                    if (cooldownMessage.contains("%remaining%")) {
                        long remainingSecs = (cooldownExpireTime - currentTime) / 1000;
                        cooldownMessage = cooldownMessage.replace("%remaining%", String.valueOf(remainingSecs));
                    }
                    
                    player.sendMessage(cooldownMessage);
                    
                    // Update last message time
                    setLastMessageTime(playerUUID, itemId, commandId, currentTime);
                }
            }
            
            event.setCancelled(true);
            return; // Still on cooldown
        }
        
        // Action was already determined earlier
        boolean commandExecuted = false;
        
        // Handle right click
        if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) 
                && customItem.getRightClickCommand() != null) {
            CommandProperties rightClickProps = customItem.getRightClickProperties();
            
            // Execute right click command
            if (rightClickProps != null) {
                customItem.executeCommandProperties(rightClickProps, player);
            } else {
                customItem.executeCommand(customItem.getRightClickCommand(), player);
            }
            commandExecuted = true;
            
            // Set command-specific cooldown if available
            if (rightClickProps != null && rightClickProps.getCooldown() > 0) {
                long cmdCooldown = rightClickProps.getEffectiveCooldown(globalCooldown);
                
                // Store cooldown
                setCooldownExpirationTime(playerUUID, itemId, RIGHT_CLICK_ID, currentTime + cmdCooldown);
                
                commandExecuted = false; // Skip the global cooldown application
            }
            
            // Prevent block placement when right-clicking if configured
            if (action == Action.RIGHT_CLICK_BLOCK && customItem.shouldCancelRightClick()) {
                event.setCancelled(true);
            }
        }
        
        // Handle left click
        if ((action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) 
                && customItem.getLeftClickCommand() != null) {
            CommandProperties leftClickProps = customItem.getLeftClickProperties();
            
            // Execute left click command
            if (leftClickProps != null) {
                customItem.executeCommandProperties(leftClickProps, player);
            } else {
                customItem.executeCommand(customItem.getLeftClickCommand(), player);
            }
            commandExecuted = true;
            
            // Set command-specific cooldown if available
            if (leftClickProps != null && leftClickProps.getCooldown() > 0) {
                long cmdCooldown = leftClickProps.getEffectiveCooldown(globalCooldown);
                
                // Store cooldown
                setCooldownExpirationTime(playerUUID, itemId, LEFT_CLICK_ID, currentTime + cmdCooldown);
                
                commandExecuted = false; // Skip the global cooldown application
            }
            
            // Prevent block breaking when left-clicking if configured
            if (action == Action.LEFT_CLICK_BLOCK && customItem.shouldCancelLeftClick()) {
                event.setCancelled(true);
            }
        }
        
        // Apply cooldown if a command was executed
        if (commandExecuted) {
            long itemCooldown = customItem.getEffectiveCooldown(globalCooldown);
            
            // Store cooldown
            setCooldownExpirationTime(playerUUID, itemId, ITEM_COOLDOWN_ID, currentTime + itemCooldown);
            
            // Notify PlayerDataManager to update inventory tracking after command execution
            playerDataManager.onItemGive(player);
        }
    }
    
    /**
     * Determines the command ID based on the interaction action
     * @param action The player's interaction action
     * @return The command ID (right, left, or item for unknown)
     */
    private String determineCommandId(Action action) {
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            return RIGHT_CLICK_ID;
        } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            return LEFT_CLICK_ID;
        }
        return ITEM_COOLDOWN_ID; // Default fallback
    }
    
    /**
     * Find a matching custom item from all namespaces
     */
    private CustomItem findMatchingCustomItem(ItemStack item) {
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
     * Track player inventory changes when switching items
     */
    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        // Update inventory tracking when players switch items
        playerDataManager.scanPlayerInventory(event.getPlayer());
    }
    
    /**
     * Handle player quit event to clean up memory
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clear our in-memory cooldown data
        UUID playerUUID = event.getPlayer().getUniqueId();
        cooldownData.remove(playerUUID);
        lastMessageData.remove(playerUUID);
    }
    
    /**
     * Get the cooldown expiration time for a player, item, and command
     * @param playerUUID The player UUID
     * @param itemId The item ID
     * @param commandId The command ID (right, left, or item)
     * @return The expiration time or null if no cooldown exists
     */
    private Long getCooldownExpirationTime(UUID playerUUID, String itemId, String commandId) {
        Map<String, Map<String, Long>> itemCooldowns = cooldownData.get(playerUUID);
        if (itemCooldowns == null) return null;
        
        Map<String, Long> commandCooldowns = itemCooldowns.get(itemId);
        if (commandCooldowns == null) return null;
        
        return commandCooldowns.get(commandId);
    }
    
    /**
     * Set the cooldown expiration time for a player, item, and command
     * @param playerUUID The player UUID
     * @param itemId The item ID
     * @param commandId The command ID (right, left, or item)
     * @param expirationTime The cooldown expiration time
     */
    private void setCooldownExpirationTime(UUID playerUUID, String itemId, String commandId, long expirationTime) {
        cooldownData.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(itemId, k -> new ConcurrentHashMap<>())
                .put(commandId, expirationTime);
    }
    
    /**
     * Get the last message time for a player, item, and command
     * @param playerUUID The player UUID
     * @param itemId The item ID
     * @param commandId The command ID (right, left, or item)
     * @return The last message time or null if no message has been sent
     */
    private Long getLastMessageTime(UUID playerUUID, String itemId, String commandId) {
        Map<String, Map<String, Long>> itemMessages = lastMessageData.get(playerUUID);
        if (itemMessages == null) return null;
        
        Map<String, Long> commandMessages = itemMessages.get(itemId);
        if (commandMessages == null) return null;
        
        return commandMessages.get(commandId);
    }
    
    /**
     * Set the last message time for a player, item, and command
     * @param playerUUID The player UUID
     * @param itemId The item ID
     * @param commandId The command ID (right, left, or item)
     * @param messageTime The last message time
     */
    private void setLastMessageTime(UUID playerUUID, String itemId, String commandId, long messageTime) {
        lastMessageData.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(itemId, k -> new ConcurrentHashMap<>())
                .put(commandId, messageTime);
    }
}

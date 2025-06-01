package com.yrhv.coreitems.give.model;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a custom item with configurable properties
 */
public class CustomItem {
    private final String identifier;
    private Material material;
    private String displayName;
    private List<String> lore;
    private int customModelData = -1;
    private boolean unbreakable = false;
    private boolean hideAttributes = false;
    private boolean glowing = false;
    private boolean enchanted = false;
    private Map<Enchantment, Integer> enchantments = new HashMap<>();
    private List<ItemFlag> itemFlags = new ArrayList<>();
    private CommandProperties rightClickProperties = null;
    private CommandProperties leftClickProperties = null;
    private int cooldown = -1; // -1 means use global cooldown (legacy support)
    private String cooldownMessage = null; // null means use global message (legacy support)
    private int cooldownMessageInterval = -1; // -1 means use global interval (legacy support)
    private boolean cancelRightClick = true;
    private boolean cancelLeftClick = true;
    
    public CustomItem(String identifier) {
        this.identifier = identifier;
        this.lore = new ArrayList<>();
    }
    
    /**
     * Gets the unique identifier for this custom item
     * @return The identifier string
     */
    public String getId() {
        return identifier;
    }
    
    /**
     * Converts this custom item to a Bukkit ItemStack
     * @return The created ItemStack
     */
    public ItemStack toItemStack() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            if (displayName != null) {
                meta.setDisplayName(displayName);
            }
            
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }
            
            if (customModelData != -1) {
                meta.setCustomModelData(customModelData);
            }
            
            meta.setUnbreakable(unbreakable);
            
            if (!itemFlags.isEmpty()) {
                itemFlags.forEach(meta::addItemFlags);
            }
            
            if (hideAttributes) {
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            }
            
            item.setItemMeta(meta);
        }
        
        if (!enchantments.isEmpty()) {
            enchantments.forEach((enchantment, level) -> 
                item.addUnsafeEnchantment(enchantment, level));
        }
        
        if ((glowing || enchanted) && enchantments.isEmpty()) {
            // Add a basic enchantment and hide it to create the glowing effect
            Enchantment sharpness = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("sharpness"));
            if (sharpness != null) {
                item.addUnsafeEnchantment(sharpness, 1);
                ItemMeta glowMeta = item.getItemMeta();
                if (glowMeta != null) {
                    glowMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    item.setItemMeta(glowMeta);
                }
            }
        }
        
        return item;
    }

    // Getters and setters
    public String getIdentifier() {
        return identifier;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore;
    }

    public void addLoreLine(String line) {
        this.lore.add(line);
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
    }

    public boolean isUnbreakable() {
        return unbreakable;
    }

    public void setUnbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
    }

    public boolean isHideAttributes() {
        return hideAttributes;
    }

    public void setHideAttributes(boolean hideAttributes) {
        this.hideAttributes = hideAttributes;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
    }
    
    public boolean isEnchanted() {
        return enchanted;
    }
    
    public void setEnchanted(boolean enchanted) {
        this.enchanted = enchanted;
    }

    public Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }

    public void addEnchantment(Enchantment enchantment, int level) {
        this.enchantments.put(enchantment, level);
    }

    public List<ItemFlag> getItemFlags() {
        return itemFlags;
    }

    public void addItemFlag(ItemFlag flag) {
        this.itemFlags.add(flag);
    }
    
    public String getRightClickCommand() {
        return rightClickProperties != null ? rightClickProperties.getCommand() : null;
    }
    
    public void setRightClickCommand(String command) {
        if (command == null) {
            this.rightClickProperties = null;
        } else if (this.rightClickProperties == null) {
            this.rightClickProperties = new CommandProperties(command);
        } else {
            this.rightClickProperties.setCommand(command);
        }
    }
    
    public CommandProperties getRightClickProperties() {
        return rightClickProperties;
    }
    
    public void setRightClickProperties(CommandProperties properties) {
        this.rightClickProperties = properties;
    }
    
    public String getLeftClickCommand() {
        return leftClickProperties != null ? leftClickProperties.getCommand() : null;
    }
    
    public void setLeftClickCommand(String command) {
        if (command == null) {
            this.leftClickProperties = null;
        } else if (this.leftClickProperties == null) {
            this.leftClickProperties = new CommandProperties(command);
        } else {
            this.leftClickProperties.setCommand(command);
        }
    }
    
    public CommandProperties getLeftClickProperties() {
        return leftClickProperties;
    }
    
    public void setLeftClickProperties(CommandProperties properties) {
        this.leftClickProperties = properties;
    }
    
    public int getCooldown() {
        return cooldown;
    }
    
    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }
    
    public String getCooldownMessage() {
        return cooldownMessage;
    }
    
    public void setCooldownMessage(String cooldownMessage) {
        this.cooldownMessage = cooldownMessage;
    }
    
    public int getCooldownMessageInterval() {
        return cooldownMessageInterval;
    }
    
    public void setCooldownMessageInterval(int cooldownMessageInterval) {
        this.cooldownMessageInterval = cooldownMessageInterval;
    }
    
    public boolean shouldCancelRightClick() {
        return cancelRightClick;
    }
    
    public void setCancelRightClick(boolean cancelRightClick) {
        this.cancelRightClick = cancelRightClick;
    }
    
    public boolean shouldCancelLeftClick() {
        return cancelLeftClick;
    }
    
    public void setCancelLeftClick(boolean cancelLeftClick) {
        this.cancelLeftClick = cancelLeftClick;
    }
    
    /**
     * Executes the command for this item with player placeholders replaced
     * @param command The command to execute (with placeholders)
     * @param player The player to execute the command for
     */
    public void executeCommand(String command, Player player) {
        if (command == null) return;
        
        // Replace player placeholders - support both formats
        String processedCommand = command
                .replace("%player%", player.getName())
                .replace("{player}", player.getName());
        
        // Execute the command
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
    }
    
    /**
     * Executes the command properties for this item with player placeholders replaced
     * @param properties The command properties to execute
     * @param player The player to execute the command for
     */
    public void executeCommandProperties(CommandProperties properties, Player player) {
        if (properties == null || properties.getCommand() == null) return;
        executeCommand(properties.getCommand(), player);
    }
    
    /**
     * Gets the effective cooldown for this item
     * @param globalCooldown The global cooldown to use if item has no specific cooldown
     * @return The cooldown in milliseconds
     */
    public long getEffectiveCooldown(long globalCooldown) {
        return cooldown > 0 ? cooldown : globalCooldown;
    }
    
    /**
     * Gets the effective cooldown message interval for this item
     * @param globalInterval The global interval to use if item has no specific interval
     * @return The number of messages to send during a cooldown period
     */
    public int getEffectiveCooldownMessageInterval(int globalInterval) {
        return cooldownMessageInterval > 0 ? cooldownMessageInterval : globalInterval;
    }
}

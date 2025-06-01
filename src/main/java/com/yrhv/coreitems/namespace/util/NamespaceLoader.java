package com.yrhv.coreitems.namespace.util;

import com.yrhv.coreitems.give.model.CommandProperties;
import com.yrhv.coreitems.give.model.CustomItem;
import com.yrhv.coreitems.namespace.model.ItemNamespace;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for loading item namespaces from configuration
 */
public class NamespaceLoader {
    
    private final JavaPlugin plugin;
    
    /**
     * Creates a new namespace loader
     * 
     * @param plugin The JavaPlugin instance
     */
    public NamespaceLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Loads items into a namespace from its configuration file
     * 
     * @param namespace The namespace to load
     */
    public void loadNamespace(ItemNamespace namespace) {
        // Clear any existing items
        namespace.clearItems();
        
        // Load the configuration
        FileConfiguration config = YamlConfiguration.loadConfiguration(namespace.getConfigFile());
        
        // Process each item section
        for (String key : config.getKeys(false)) {
            if (key.startsWith("#")) continue; // Skip comment sections
            
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;
            
            try {
                CustomItem item = parseCustomItem(key, section);
                namespace.addItem(key, item);
                plugin.getLogger().info("Loaded custom item: " + namespace.getName() + ":" + key);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load custom item: " + namespace.getName() + ":" + key, e);
            }
        }
        
        plugin.getLogger().info("Loaded " + namespace.getItems().size() + " items in namespace: " + namespace.getName());
    }
    
    /**
     * Parse a custom item from its configuration section
     */
    private CustomItem parseCustomItem(String identifier, ConfigurationSection section) {
        CustomItem item = new CustomItem(identifier);
        
        // Material is required
        String materialName = section.getString("material");
        if (materialName == null) {
            throw new IllegalArgumentException("Custom item " + identifier + " is missing material property");
        }
        
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            throw new IllegalArgumentException("Invalid material for custom item " + identifier + ": " + materialName);
        }
        
        item.setMaterial(material);
        
        // Parse optional properties
        if (section.contains("name")) {
            String name = section.getString("name");
            name = translateHexColorCodes(name);
            item.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        }
        
        if (section.contains("lore")) {
            List<String> lore = section.getStringList("lore").stream()
                    .map(this::translateHexColorCodes)
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .collect(Collectors.toList());
            item.setLore(lore);
        }
        
        if (section.contains("custom-model-data")) {
            item.setCustomModelData(section.getInt("custom-model-data"));
        }
        
        if (section.contains("unbreakable")) {
            item.setUnbreakable(section.getBoolean("unbreakable"));
        }
        
        if (section.contains("hide-attributes")) {
            item.setHideAttributes(section.getBoolean("hide-attributes"));
        }
        
        if (section.contains("glowing")) {
            item.setGlowing(section.getBoolean("glowing"));
        }
        
        if (section.contains("is_enchanted")) {
            item.setEnchanted(section.getBoolean("is_enchanted"));
        }
        
        // Parse enchantments
        if (section.contains("enchantments")) {
            ConfigurationSection enchants = section.getConfigurationSection("enchantments");
            if (enchants != null) {
                for (String enchantKey : enchants.getKeys(false)) {
                    try {
                        // Convert enchantment name to minecraft key format (lowercase, no underscores)
                        String formattedKey = enchantKey.toLowerCase().replace("_", "");
                        Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(formattedKey));
                        if (enchantment != null) {
                            int level = enchants.getInt(enchantKey);
                            item.addEnchantment(enchantment, level);
                        } else {
                            plugin.getLogger().warning("Unknown enchantment: " + enchantKey + " for item " + identifier);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Error adding enchantment: " + enchantKey, e);
                    }
                }
            }
        }
        
        // Parse item flags
        if (section.contains("item-flags")) {
            List<String> flagList = section.getStringList("item-flags");
            for (String flagName : flagList) {
                try {
                    ItemFlag flag = ItemFlag.valueOf(flagName.toUpperCase());
                    item.addItemFlag(flag);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Unknown item flag: " + flagName + " for item " + identifier);
                }
            }
        }
        
        // Parse click commands
        if (section.contains("right-click-command")) {
            Object rightClickObj = section.get("right-click-command");
            
            // Check if this is a string or a section with nested properties
            if (rightClickObj instanceof String) {
                // Simple string command
                item.setRightClickCommand((String) rightClickObj);
            } else if (section.isConfigurationSection("right-click-command")) {
                // Nested command properties
                ConfigurationSection rightClickSection = section.getConfigurationSection("right-click-command");
                String command = rightClickSection.getString("command");
                if (command == null) {
                    // Old format - the key itself is the command
                    command = rightClickSection.getName();
                }
                
                // Create command properties
                CommandProperties properties = new CommandProperties(command);
                item.setRightClickProperties(properties);
                
                // Parse cooldown properties
                if (rightClickSection.contains("cooldown")) {
                    properties.setCooldown(rightClickSection.getInt("cooldown"));
                }
                
                if (rightClickSection.contains("cooldown-message")) {
                    String message = rightClickSection.getString("cooldown-message");
                    message = translateHexColorCodes(message);
                    properties.setCooldownMessage(ChatColor.translateAlternateColorCodes('&', message));
                }
                
                if (rightClickSection.contains("cooldown-message-interval")) {
                    properties.setCooldownMessageInterval(rightClickSection.getInt("cooldown-message-interval"));
                }
                
                // Handle show-item-cooldown property
                if (rightClickSection.contains("show-item-cooldown")) {
                    properties.setShowItemCooldown(rightClickSection.getBoolean("show-item-cooldown"));
                }
            } else {
                // Just a simple command
                item.setRightClickCommand(rightClickObj.toString());
            }
        }
        
        if (section.contains("left-click-command")) {
            Object leftClickObj = section.get("left-click-command");
            
            // Check if this is a string or a section with nested properties
            if (leftClickObj instanceof String) {
                // Simple string command
                item.setLeftClickCommand((String) leftClickObj);
            } else if (section.isConfigurationSection("left-click-command")) {
                // Nested command properties
                ConfigurationSection leftClickSection = section.getConfigurationSection("left-click-command");
                String command = leftClickSection.getString("command");
                if (command == null) {
                    // Old format - the key itself is the command
                    command = leftClickSection.getName();
                }
                
                // Create command properties
                CommandProperties properties = new CommandProperties(command);
                item.setLeftClickProperties(properties);
                
                // Parse cooldown properties
                if (leftClickSection.contains("cooldown")) {
                    properties.setCooldown(leftClickSection.getInt("cooldown"));
                }
                
                if (leftClickSection.contains("cooldown-message")) {
                    String message = leftClickSection.getString("cooldown-message");
                    message = translateHexColorCodes(message);
                    properties.setCooldownMessage(ChatColor.translateAlternateColorCodes('&', message));
                }
                
                if (leftClickSection.contains("cooldown-message-interval")) {
                    properties.setCooldownMessageInterval(leftClickSection.getInt("cooldown-message-interval"));
                }
                
                // Parse Minecraft cooldown properties for left-click command
                // Handle show-item-cooldown property
                if (leftClickSection.contains("show-item-cooldown")) {
                    properties.setShowItemCooldown(leftClickSection.getBoolean("show-item-cooldown"));
                }
            } else {
                // Just a simple command
                item.setLeftClickCommand(leftClickObj.toString());
            }
        }
        
        // Parse per-item click action properties
        if (section.contains("cooldown")) {
            item.setCooldown(section.getInt("cooldown"));
        }
        
        if (section.contains("cooldown-message")) {
            String message = section.getString("cooldown-message");
            message = translateHexColorCodes(message);
            item.setCooldownMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
        
        if (section.contains("cooldown-message-interval")) {
            item.setCooldownMessageInterval(section.getInt("cooldown-message-interval"));
        }
        
        // Parse show-item-cooldown property
        if (section.contains("show-item-cooldown")) {
            item.setShowItemCooldown(section.getBoolean("show-item-cooldown"));
        }
        
        if (section.contains("cancel_right_click")) {
            item.setCancelRightClick(section.getBoolean("cancel_right_click"));
        }
        
        if (section.contains("cancel_left_click")) {
            item.setCancelLeftClick(section.getBoolean("cancel_left_click"));
        }
        
        // Parse droppable property
        if (section.contains("droppable")) {
            item.setDroppable(section.getBoolean("droppable"));
        }
        
        // Parse drop message
        if (section.contains("drop-message")) {
            String message = section.getString("drop-message");
            message = translateHexColorCodes(message);
            item.setDropMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
        
        return item;
    }
    
    /**
     * Translates hex color codes in the format &#RRGGBB to the Bukkit color format
     * @param message Message containing hex color codes
     * @return Translated message with proper hex color codes
     */
    private String translateHexColorCodes(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        // Translate hex color codes (&#RRGGBB)
        final Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            // Convert to the format Bukkit uses for hex colors: §x§R§G§B§B§B§B
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hexCode.toCharArray()) {
                replacement.append("§").append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        
        matcher.appendTail(buffer);
        return buffer.toString();
    }
    
    /**
     * Creates a default namespace with example items
     * 
     * @param namespaceDir The directory for the namespace
     * @throws IOException If there is an error creating the configuration file
     */
    public void createDefaultNamespace(File namespaceDir) throws IOException {
        File configFile = new File(namespaceDir, "customs.yml");
        
        // Copy the customs.yml file directly from resources
        try (InputStream is = plugin.getResource("customs.yml")) {
            if (is == null) {
                plugin.getLogger().severe("Could not find customs.yml in plugin resources!");
                return;
            }
            
            // Ensure the file is created
            if (!configFile.exists()) {
                configFile.createNewFile();
            }
            
            // Copy the file content directly to preserve all comments
            Files.copy(is, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Created default customs.yml file from template");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create default customs.yml file: " + e.getMessage());
            throw e;
        }
    }
}

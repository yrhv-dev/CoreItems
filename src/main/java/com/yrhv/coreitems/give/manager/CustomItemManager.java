package com.yrhv.coreitems.give.manager;

import com.yrhv.coreitems.give.model.CustomItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CustomItemManager {
    private final JavaPlugin plugin;
    private final Map<String, CustomItem> customItems;
    private File customFile;
    private FileConfiguration customConfig;

    public CustomItemManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.customItems = new HashMap<>();
        loadCustomItems();
    }

    /**
     * Loads custom items from the customs.yml file
     */
    public void loadCustomItems() {
        customItems.clear();
        
        // Ensure the file exists
        customFile = new File(plugin.getDataFolder(), "customs.yml");
        if (!customFile.exists()) {
            plugin.getDataFolder().mkdirs();
            saveDefaultCustomsFile();
        }
        
        // Load the configuration
        customConfig = YamlConfiguration.loadConfiguration(customFile);
        
        // Process each item section
        for (String key : customConfig.getKeys(false)) {
            if (key.startsWith("#")) continue; // Skip comment sections
            
            ConfigurationSection section = customConfig.getConfigurationSection(key);
            if (section == null) continue;
            
            try {
                CustomItem item = parseCustomItem(key, section);
                customItems.put(key.toLowerCase(), item);
                plugin.getLogger().info("Loaded custom item: " + key);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load custom item: " + key, e);
            }
        }
        
        plugin.getLogger().info("Loaded " + customItems.size() + " custom items");
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
        
        // Parse per-item click action properties
        if (section.contains("cooldown")) {
            item.setCooldown(section.getInt("cooldown"));
        }
        
        if (section.contains("cancel_right_click")) {
            item.setCancelRightClick(section.getBoolean("cancel_right_click"));
        }
        
        if (section.contains("cancel_left_click")) {
            item.setCancelLeftClick(section.getBoolean("cancel_left_click"));
        }
        
        // Check for right-click and left-click commands
        if (section.contains("right-click-command")) {
            item.setRightClickCommand(section.getString("right-click-command"));
        }
        
        if (section.contains("left-click-command")) {
            item.setLeftClickCommand(section.getString("left-click-command"));
        }
        
        return item;
    }
    
    /**
     * Save the default customs.yml file if it doesn't exist
     */
    private void saveDefaultCustomsFile() {
        try (InputStream in = plugin.getResource("customs.yml")) {
            if (in != null) {
                Files.copy(in, customFile.toPath());
            } else {
                // Create a default file with sample item
                FileConfiguration config = new YamlConfiguration();
                
                // Add comments at the top of the file using standard YAML comments
                StringBuilder comments = new StringBuilder();
                comments.append("# CoreItems Custom Items Configuration\n");
                comments.append("# Available properties for items:\n");
                comments.append("# - material: The Minecraft material type (REQUIRED)\n");
                comments.append("# - name: Custom display name (supports color codes with & and hex colors with &#RRGGBB)\n");
                comments.append("# - lore: List of lore lines (supports color codes with & and hex colors with &#RRGGBB)\n");
                comments.append("# - custom-model-data: Custom model data integer for resource packs\n");
                comments.append("# - unbreakable: true/false to make item unbreakable\n");
                comments.append("# - hide-attributes: true/false to hide item attributes\n");
                comments.append("# - glowing: true/false to make item glow without enchantments\n");
                comments.append("# - is_enchanted: true/false to add enchantment glint without showing enchantments\n");
                comments.append("# - enchantments: Map of enchantment names to level values\n");
                comments.append("# - item-flags: List of item flags (HIDE_ENCHANTS, HIDE_ATTRIBUTES, etc.)\n");
                comments.append("# - right-click-command: Command to execute when right-clicking with item (use {player} as placeholder)\n");
                comments.append("# - left-click-command: Command to execute when left-clicking with item (use {player} as placeholder)\n");
                
                // Save comments at the top manually
                try {
                    Files.writeString(customFile.toPath(), comments.toString());
                    config.save(customFile); // This will append the config after the comments
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not save comments to customs.yml", e);
                }
                
                // Add example item
                ConfigurationSection exampleItem = config.createSection("example_sword");
                exampleItem.set("material", "DIAMOND_SWORD");
                exampleItem.set("name", "&6Example Sword");
                exampleItem.set("lore", List.of(
                    "&7A powerful example sword",
                    "&cUse /coreitems give <player> example_sword"
                ));
                exampleItem.set("custom-model-data", 1001);
                exampleItem.set("unbreakable", true);
                exampleItem.set("glowing", true);
                
                ConfigurationSection enchants = exampleItem.createSection("enchantments");
                enchants.set("DAMAGE_ALL", 5);
                enchants.set("FIRE_ASPECT", 2);
                
                exampleItem.set("item-flags", List.of("HIDE_ATTRIBUTES", "HIDE_UNBREAKABLE"));
                
                // Save the configuration
                config.save(customFile);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save default customs.yml", e);
        }
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
     * Get a custom item by its identifier
     * @param identifier Item identifier
     * @return The CustomItem or null if not found
     */
    public CustomItem getItem(String identifier) {
        return customItems.get(identifier.toLowerCase());
    }
    
    /**
     * Get all loaded custom items
     * @return Map of identifier to CustomItem
     */
    public Map<String, CustomItem> getAllItems() {
        return new HashMap<>(customItems);
    }
    
    /**
     * Give a custom item to a player
     * @param player Target player
     * @param itemId Custom item identifier
     * @return true if successful, false if item not found
     */
    public boolean giveItemToPlayer(Player player, String itemId) {
        CustomItem customItem = getItem(itemId);
        if (customItem == null) {
            return false;
        }
        
        player.getInventory().addItem(customItem.toItemStack());
        return true;
    }
}

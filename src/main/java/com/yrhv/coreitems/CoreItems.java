package com.yrhv.coreitems;

import com.yrhv.coreitems.give.command.CoreItemsCommand;
import com.yrhv.coreitems.give.listener.CustomItemListener;
import com.yrhv.coreitems.give.storage.PlayerDataManager;
import com.yrhv.coreitems.gui.manager.MenuManager;
import com.yrhv.coreitems.gui.util.MenuListener;
import com.yrhv.coreitems.namespace.manager.NamespaceManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class CoreItems extends JavaPlugin {
    private NamespaceManager namespaceManager;
    private MenuManager menuManager;
    private PlayerDataManager playerDataManager;
    
    @Override
    public void onEnable() {
        // Create data folder if it doesn't exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        // Save default config.yml to the plugin folder if it doesn't exist
        saveDefaultConfig();
        
        // Make sure to reload the config in case it was already there
        reloadConfig();
        getLogger().info("Loaded configuration file");
        
        // Create template_file folder with customs.yml template
        createTemplateFolder();
        
        // Initialize the namespace manager
        namespaceManager = new NamespaceManager(this);
        namespaceManager.loadNamespaces();
        
        // Initialize the menu manager
        menuManager = new MenuManager(this);
        
        // Initialize the player data manager
        playerDataManager = new PlayerDataManager(this, namespaceManager);
        
        // Register commands
        registerCommands();
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new CustomItemListener(this, namespaceManager, playerDataManager), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        
        getLogger().info("CoreItems plugin enabled!");
    }
    
    private void registerCommands() {
        CoreItemsCommand commandExecutor = new CoreItemsCommand(this);
        
        // Register the main command
        PluginCommand mainCommand = getCommand("coreitems");
        if (mainCommand != null) {
            mainCommand.setExecutor(commandExecutor);
            mainCommand.setTabCompleter(commandExecutor);
        }
        
        // Register the alias command
        PluginCommand aliasCommand = getCommand("citems");
        if (aliasCommand != null) {
            aliasCommand.setExecutor(commandExecutor);
            aliasCommand.setTabCompleter(commandExecutor);
        }
    }
    
    /**
     * Reloads the plugin configuration and player data
     */
    public void reload() {
        // Reload config
        reloadConfig();
        
        // Reload namespaces
        namespaceManager.loadNamespaces();
        
        // Reload player data
        playerDataManager.onReload();
        
        getLogger().info("CoreItems plugin configuration reloaded!");
    }

    @Override
    public void onDisable() {
        // Save player data and clean up
        if (playerDataManager != null) {
            playerDataManager.shutdown();
        }
        
        getLogger().info("CoreItems plugin disabled!");
    }
    
    /**
     * Get the namespace manager
     * @return The namespace manager
     */
    public NamespaceManager getNamespaceManager() {
        return namespaceManager;
    }
    
    /**
     * Get the menu manager
     * @return The menu manager
     */
    public MenuManager getMenuManager() {
        return menuManager;
    }
    
    /**
     * Get the player data manager
     * @return The player data manager
     */
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    
    /**
     * Creates a template_file folder in the plugin's main directory and copies the customs.yml template
     * for user reference and editing.
     */
    private void createTemplateFolder() {
        // Create template_file directory
        File templateDir = new File(getDataFolder(), "template_file");
        if (!templateDir.exists()) {
            templateDir.mkdirs();
            getLogger().info("Created template_file directory");
        }
        
        // Copy the customs.yml template to the template_file directory
        File templateFile = new File(templateDir, "customs.yml");
        try (InputStream is = getResource("customs.yml")) {
            if (is == null) {
                getLogger().warning("Could not find customs.yml in plugin resources!");
                return;
            }
            
            // Create the file if it doesn't exist
            if (!templateFile.exists()) {
                templateFile.createNewFile();
            }
            
            // Copy the file content directly to preserve all comments
            Files.copy(is, templateFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            getLogger().info("Copied customs.yml template to template_file directory for reference");
            
        } catch (IOException e) {
            getLogger().warning("Failed to copy customs.yml template: " + e.getMessage());
        }
    }
}

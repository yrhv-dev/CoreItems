package com.yrhv.coreitems.gui.search;

import com.yrhv.coreitems.CoreItems;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handler for namespace search functionality
 */
public class NamespaceSearchHandler implements Listener {
    
    private final CoreItems plugin;
    private final Map<UUID, Long> awaitingInput;
    
    /**
     * Constructor for the namespace search handler
     * 
     * @param plugin The CoreItems plugin instance
     */
    public NamespaceSearchHandler(CoreItems plugin) {
        this.plugin = plugin;
        this.awaitingInput = new HashMap<>();
    }
    
    /**
     * Prompt a player to enter a search term
     * 
     * @param player The player to prompt
     */
    public void promptSearch(Player player) {
        // Register this instance as a listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // Mark player as awaiting input
        awaitingInput.put(player.getUniqueId(), System.currentTimeMillis());
        
        // Send search prompt message
        player.sendMessage(ChatColor.GREEN + "Please type the namespace you want to search for.");
        player.sendMessage(ChatColor.YELLOW + "Type 'cancel' to cancel the search.");
        
        // Set up automatic timeout after 30 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                Long startTime = awaitingInput.get(player.getUniqueId());
                if (startTime != null && System.currentTimeMillis() - startTime > 30000) {
                    cancelSearch(player);
                    player.sendMessage(ChatColor.RED + "Search timed out. Please try again.");
                }
            }
        }.runTaskLater(plugin, 600); // 30 seconds (20 ticks per second)
    }
    
    /**
     * Cancel a player's ongoing search
     * 
     * @param player The player whose search to cancel
     */
    public void cancelSearch(Player player) {
        awaitingInput.remove(player.getUniqueId());
        
        // If no players are awaiting input, unregister this listener
        if (awaitingInput.isEmpty()) {
            HandlerList.unregisterAll(this);
        }
        
        // Reopen the main menu
        plugin.getMenuManager().openMainMenu(player);
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        // Check if this player is awaiting search input
        if (awaitingInput.containsKey(player.getUniqueId())) {
            event.setCancelled(true); // Don't send the search term to chat
            
            String searchTerm = event.getMessage();
            
            // Handle cancel request
            if (searchTerm.equalsIgnoreCase("cancel")) {
                // Must run on the main thread
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        cancelSearch(player);
                        player.sendMessage(ChatColor.YELLOW + "Search cancelled.");
                    }
                }.runTask(plugin);
                return;
            }
            
            // Process the search (must run on the main thread)
            final String finalSearchTerm = searchTerm.toLowerCase();
            new BukkitRunnable() {
                @Override
                public void run() {
                    awaitingInput.remove(player.getUniqueId());
                    
                    // If no players are awaiting input, unregister this listener
                    if (awaitingInput.isEmpty()) {
                        HandlerList.unregisterAll(NamespaceSearchHandler.this);
                    }
                    
                    // Open the search results menu
                    new SearchResultsMenu(plugin, player, finalSearchTerm).open();
                }
            }.runTask(plugin);
        }
    }
}

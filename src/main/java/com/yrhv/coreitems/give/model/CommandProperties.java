package com.yrhv.coreitems.give.model;

/**
 * Represents properties for a command on a custom item
 */
public class CommandProperties {
    private String command;
    private int cooldown = -1; // -1 means use global cooldown
    private String cooldownMessage = null; // null means use global message
    private int cooldownMessageInterval = -1; // -1 means use global interval
    
    // Whether to show a visual Minecraft cooldown effect when the command is on cooldown
    private boolean showItemCooldown = false;
    
    public CommandProperties(String command) {
        this.command = command;
    }
    
    public String getCommand() {
        return command;
    }
    
    public void setCommand(String command) {
        this.command = command;
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
    
    /**
     * Gets the effective cooldown for this command
     * @param globalCooldown The global cooldown to use if command has no specific cooldown
     * @return The cooldown in milliseconds
     */
    public long getEffectiveCooldown(long globalCooldown) {
        return cooldown > 0 ? cooldown : globalCooldown;
    }
    
    /**
     * Gets the effective cooldown message interval for this command
     * @param globalInterval The global interval to use if command has no specific interval
     * @return The number of messages to send during a cooldown period
     */
    public int getEffectiveCooldownMessageInterval(int globalInterval) {
        return cooldownMessageInterval > 0 ? cooldownMessageInterval : globalInterval;
    }
    
    public boolean isShowItemCooldown() {
        return showItemCooldown;
    }
    
    public void setShowItemCooldown(boolean showItemCooldown) {
        this.showItemCooldown = showItemCooldown;
    }
}

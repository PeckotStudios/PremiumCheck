package com.peckot.bukkit.PremiumCheck;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class PremiumCheck extends JavaPlugin {
    private DataManager dataManager;

    private boolean papi;
    private boolean debug;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        debug = getConfig().getBoolean("debug");
        debug("Debug mode enabled");
        DataManager.DataType dataType;
        DataManager.Encryption encryption;
        debug("Loading data type...");
        switch (getConfig().getString("data.type").toUpperCase()) {
            case "YAML":
                dataType = DataManager.DataType.YAML;
                break;
            case "JSON":
                dataType = DataManager.DataType.JSON;
                break;
            default:
                dataType = DataManager.DataType.SQLITE;
                break;
        }
        debug("Loading data encryption...");
        switch (getConfig().getString("data.encryption").toUpperCase()) {
            case "MD5":
                encryption = DataManager.Encryption.MD5;
                break;
            case "BASE64":
                encryption = DataManager.Encryption.BASE64;
                break;
            case "NONE":
                encryption = DataManager.Encryption.NONE;
                break;
            default:
                encryption = DataManager.Encryption.SHA1;
                break;
        }
        debug("Initializing data manager...");
        dataManager = new DataManager(this, dataType, encryption);
        debug("Registering commands...");
        getCommand("premium").setExecutor(new CommandHandler(this));
        debug("Hooking into PlaceholderAPI...");
        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            debug("PlaceholderAPI found, now hooking...");
            papi = true;
            new PlaceholderManager(this).register();
        } else {
            debug("PlaceholderAPI not found, skipping...");
            papi = false;
        }
    }

    @Override
    public void onDisable() {
        dataManager.destroy();
    }

    public void reload() {
        getLogger().info("Starting reload...");
        Bukkit.getPluginManager().disablePlugin(this);
        Bukkit.getPluginManager().enablePlugin(this);
    }

    public boolean isPremium(Player player) {
        return dataManager.isPremium(player);
    }

    public void setPremium(Player player, boolean premium) {
        dataManager.setPremium(player, premium);
    }

    public void saveData() {
        dataManager.saveData();
    }

    public void debug(String msg) {
        if (debug) getLogger().log(Level.WARNING, msg);
    }

    public boolean papi() {
        return papi;
    }

}

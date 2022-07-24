package com.peckot.bukkit.premiumcheck;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class PremiumCheck extends JavaPlugin {

    private FileConfiguration data;
    private String dataEncoder;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        File dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) saveResource("data.yml", false);
        data = YamlConfiguration.loadConfiguration(dataFile);
        dataEncoder = getConfig().getString("dataEncoder");
        getCommand("premium").setExecutor(new CommandHandler(this));
        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new Placeholders(this).register();
        }
    }

    public void reload() {
        Bukkit.getPluginManager().disablePlugin(this);
        Bukkit.getPluginManager().enablePlugin(this);
    }

    public boolean isPremium(String player) {
        return isPremium(getServer().getPlayer(player));
    }

    public boolean isPremium(Player player) {
        String key = player.getName() + ".premium." + player.getUniqueId().toString();
        switch (dataEncoder) {
            case "base64":
                return data.getBoolean(Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8)));
            case "md5":
                try {
                    byte[] bytes = MessageDigest.getInstance("MD5").digest(key.getBytes(StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    for (byte b : bytes) {
                        sb.append(Integer.toHexString(b & 0xff));
                    }
                    return data.getBoolean(sb.toString());
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    return false;
                }
            case "sha1":
                try {
                    byte[] bytes = MessageDigest.getInstance("SHA").digest(key.getBytes(StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    for (byte b : bytes) {
                        sb.append(Integer.toHexString(b & 0xff));
                    }
                    return data.getBoolean(sb.toString());
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    return false;
                }
            default:
                return data.getBoolean(key, false);
        }
    }

    public void setPremium(Player player, boolean premium) {
        String key = player.getName() + ".premium." + player.getUniqueId().toString();
        switch (dataEncoder) {
            case "base64":
                data.set(Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8)), premium);
                break;
            case "md5":
                byte[] md5Bytes = new byte[0];
                try {
                    md5Bytes = MessageDigest.getInstance("MD5").digest(key.getBytes(StandardCharsets.UTF_8));
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                StringBuilder md5 = new StringBuilder();
                for (byte b : md5Bytes) {
                    md5.append(Integer.toHexString(b & 0xff));
                }
                data.set(md5.toString(), premium);
                break;
            case "sha1":
                byte[] shaBytes = new byte[0];
                try {
                    shaBytes = MessageDigest.getInstance("SHA").digest(key.getBytes(StandardCharsets.UTF_8));
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                StringBuilder sha = new StringBuilder();
                for (byte b : shaBytes) {
                    sha.append(Integer.toHexString(b & 0xff));
                }
                data.set(sha.toString(), premium);
                break;
            default:
                data.set(key, premium);
        }
        saveData();
    }

    public void saveData() {
        try {
            data.save(new File(getDataFolder(), "data.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

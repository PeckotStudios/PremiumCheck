package com.peckot.bukkit.PremiumCheck;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PlaceholderManager extends PlaceholderExpansion {

    private final PremiumCheck plugin;

    public PlaceholderManager(PremiumCheck plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "premium";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Pectics";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getConfig().getString("config_version");
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String header = "[Placeholder] ";
        plugin.debug(header + "Requesting " + params + " for " + player.getName());
        if (params.startsWith("check")) {
            String result = "false";
            if (params.equalsIgnoreCase("check")) {
                result = String.valueOf(plugin.isPremium(player.getPlayer()));
                plugin.debug(header + "Result: " + result);
            } else if (params.matches("check_([A-Za-z0-9_]{3,16})")) {
                result = String.valueOf(plugin.isPremium(plugin.getServer().getPlayer(params.replaceFirst("check_", ""))));
                plugin.debug(header + "Result: " + result);
            }
            return result;
        }
        plugin.debug(header + "Invalid placeholder");
        return null;
    }

}

package com.peckot.bukkit.premiumcheck;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class Placeholders extends PlaceholderExpansion {

    private final PremiumCheck plugin;

    public Placeholders(PremiumCheck plugin) {
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
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if(params.startsWith("check")){
            if (params.equalsIgnoreCase("check")) {
                return String.valueOf(plugin.isPremium(player.getPlayer()));
            } else if (params.matches("check_([A-Za-z0-9_]{3,16})")) {
                return String.valueOf(plugin.isPremium(params.replaceFirst("check_", "")));
            } else {
                return "false";
            }
        }
        return null;
    }

}

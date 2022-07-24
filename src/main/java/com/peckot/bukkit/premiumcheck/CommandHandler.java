package com.peckot.bukkit.premiumcheck;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class CommandHandler implements CommandExecutor {

    private static PremiumCheck plugin;

    public CommandHandler (PremiumCheck plugin) {
        CommandHandler.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("premium")) {
            if (args.length == 0) {
                if (sender.hasPermission("premium.use")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        if (plugin.isPremium(player)) {
                            sender.sendMessage(plugin.getConfig().getString("messages.already").replaceAll("&", "§"));
                        } else {
                            sender.sendMessage(plugin.getConfig().getString("messages.checking").replaceAll("&", "§"));
                            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                String checkUrl = plugin.getConfig().getString("uuidApi").replaceAll("\\{player}", player.getName());
                                HttpClient client = HttpClient.newHttpClient();
                                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(checkUrl)).build();
                                HttpResponse<String> response;
                                try {
                                    response = client.send(request, HttpResponse.BodyHandlers.ofString());
                                } catch (IOException | InterruptedException e) {
                                    e.printStackTrace();
                                    sender.sendMessage(plugin.getConfig().getString("messages.networkError").replaceAll("&", "§"));
                                    return;
                                }
                                if (response.body().equalsIgnoreCase(player.getUniqueId().toString().replaceAll("-", ""))) {
                                    plugin.setPremium(player, true);
                                    sender.sendMessage(plugin.getConfig().getString("messages.success").replaceAll("&", "§"));
                                    Bukkit.getScheduler().runTask(plugin, () -> {
                                        for (String cmd : plugin.getConfig().getStringList("premiumCommands"))
                                            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), PlaceholderAPI.setPlaceholders(player, cmd));
                                    });
                                } else if (response.body().contains("not ")) {
                                    sender.sendMessage(plugin.getConfig().getString("messages.notFound").replaceAll("&", "§"));
                                } else {
                                    sender.sendMessage(plugin.getConfig().getString("messages.failed").replaceAll("&", "§"));
                                }
                            });
                        }
                    } else {
                        sender.sendMessage(plugin.getConfig().getString("messages.playerOnly").replaceAll("&", "§"));
                    }
                } else {
                    sender.sendMessage(plugin.getConfig().getString("messages.noPermission").replaceAll("&", "§"));
                }
            } else {
                Player player = plugin.getServer().getPlayer(args[0]);
                if (null == player) {
                    if (args[0].equalsIgnoreCase("help")) {
                        sender.sendMessage(plugin.getConfig().getString("messages.help").replaceAll("&", "§"));
                    } else if (args[0].equalsIgnoreCase("reload")) {
                        if (sender.hasPermission("premium.reload")) {
                            plugin.reload();
                            sender.sendMessage(plugin.getConfig().getString("messages.reloaded").replaceAll("&", "§"));
                        } else {
                            sender.sendMessage(plugin.getConfig().getString("messages.noPermission").replaceAll("&", "§"));
                        }
                    } else if (args[0].equalsIgnoreCase("check") && args.length >= 2) {
                        if (sender.hasPermission("premium.check")) {
                            Player checkPlayer = plugin.getServer().getPlayer(args[1]);
                            if (!sender.hasPermission("premium.check")) {
                                sender.sendMessage(plugin.getConfig().getString("messages.noPermission").replaceAll("&", "§"));
                            } else if (plugin.isPremium(checkPlayer)) {
                                sender.sendMessage(plugin.getConfig().getString("messages.playerIsPremium").replaceAll("&", "§"));
                            } else {
                                sender.sendMessage(plugin.getConfig().getString("messages.playerIsNotPremium").replaceAll("&", "§"));
                            }
                        } else {
                            sender.sendMessage(plugin.getConfig().getString("messages.noPermission").replaceAll("&", "§"));
                        }
                    } else {
                        if (sender.hasPermission("premium.other")) {
                            sender.sendMessage(plugin.getConfig().getString("messages.playerNotFound").replaceAll("&", "§"));
                        } else {
                            sender.sendMessage(plugin.getConfig().getString("messages.noPermission").replaceAll("&", "§"));
                        }
                    }
                } else if (plugin.isPremium(player)) {
                    if (sender.hasPermission("premium.other")) {
                        sender.sendMessage(plugin.getConfig().getString("messages.playerIsPremium").replaceAll("&", "§"));
                    } else {
                        sender.sendMessage(plugin.getConfig().getString("messages.noPermission").replaceAll("&", "§"));
                    }
                } else {
                    if (sender.hasPermission("premium.other")) {
                        sender.sendMessage(plugin.getConfig().getString("messages.checking").replaceAll("&", "§"));
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                            String checkUrl = plugin.getConfig().getString("uuidApi").replaceAll("\\{player}", player.getName());
                            HttpClient client = HttpClient.newHttpClient();
                            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(checkUrl)).build();
                            HttpResponse<String> response;
                            try {
                                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                                sender.sendMessage(plugin.getConfig().getString("messages.networkError").replaceAll("&", "§"));
                                return;
                            }
                            if (response.body().equalsIgnoreCase(player.getUniqueId().toString().replaceAll("-", ""))) {
                                plugin.setPremium(player, true);
                                sender.sendMessage(plugin.getConfig().getString("messages.success").replaceAll("&", "§"));
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    for (String cmd : plugin.getConfig().getStringList("premiumCommands"))
                                        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), PlaceholderAPI.setPlaceholders(player, cmd));
                                });
                            } else if (response.body().contains("not ")) {
                                sender.sendMessage(plugin.getConfig().getString("messages.notFound").replaceAll("&", "§"));
                            } else {
                                sender.sendMessage(plugin.getConfig().getString("messages.failed").replaceAll("&", "§"));
                            }
                        });
                    } else {
                        sender.sendMessage(plugin.getConfig().getString("messages.noPermission").replaceAll("&", "§"));
                    }
                }
            }
        }
        return true;
    }

}

package com.peckot.bukkit.PremiumCheck;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import me.clip.placeholderapi.PlaceholderAPI;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CommandHandler implements CommandExecutor {

    private final String header = "[CommandHandler";
    private final PremiumCheck plugin;
    private final FileConfiguration config;

    public CommandHandler(PremiumCheck plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("premium")) {
            String header = this.header + "] ";
            // Trigger of /premium
            if (args.length == 0) {
                plugin.debug(header + "Triggered: /premium");
                // Has permission premium.use
                if (sender.hasPermission("premium.use")) {
                    plugin.debug(header + "Has permission premium.use");
                    // Is player
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        // Is premium
                        if (plugin.isPremium(player)) {
                            plugin.debug(header + "Player is premium");
                            sender.sendMessage(getMessage("messages.already"));
                        }
                        // Is not premium
                        else {
                            plugin.debug(header + "Player is not premium");
                            sender.sendMessage(getMessage("messages.checking"));
                            plugin.debug(header + "Run validate task");
                            runValidateTask(player, sender);
                        }
                    }
                    // Is console
                    else {
                        plugin.debug(header + "From console");
                        sender.sendMessage(getMessage("messages.player_only"));
                    }
                }
                // No permission premium.use
                else {
                    plugin.debug(header + "No permission premium.use");
                    sender.sendMessage(getMessage("messages.no_permission"));
                }
            }
            // Trigger of /premium <args>
            else {
                Player player = plugin.getServer().getPlayer(args[0]);
                // Player not found
                if (null == player) {
                    // Trigger of /premium help
                    if (args[0].equalsIgnoreCase("help")) {
                        plugin.debug(header + "Triggered: /premium help");
                        sender.sendMessage(getMessage("messages.help"));
                    }
                    // Trigger of /premium reload
                    else if (args[0].equalsIgnoreCase("reload")) {
                        plugin.debug(header + "Triggered: /premium reload");
                        // Has permission premium.reload
                        if (sender.hasPermission("premium.reload")) {
                            plugin.debug(header + "Has permission premium.reload");
                            plugin.reload();
                            sender.sendMessage(getMessage("messages.reloaded"));
                        }
                        // No permission premium.reload
                        else {
                            plugin.debug(header + "No permission premium.reload");
                            sender.sendMessage(getMessage("messages.no_permission"));
                        }
                    }
                    // Trigger of /premium check <player>
                    else if (args[0].equalsIgnoreCase("check") && args.length >= 2) {
                        plugin.debug(header + "Triggered: /premium check <player>");
                        // Has permission premium.check
                        if (sender.hasPermission("premium.check")) {
                            plugin.debug(header + "Has permission premium.check");
                            Player checkPlayer = plugin.getServer().getPlayer(args[1]);
                            if (plugin.isPremium(checkPlayer)) {
                                plugin.debug(header + "Player is premium");
                                sender.sendMessage(getMessage("messages.player_premium"));
                            } else {
                                plugin.debug(header + "Player is not premium");
                                sender.sendMessage(getMessage("messages.player_not_premium"));
                            }
                        }
                        // No permission premium.check
                        else {
                            plugin.debug(header + "No permission premium.check");
                            sender.sendMessage(getMessage("messages.no_permission"));
                        }
                    }
                    // Trigger of /premium <player>
                    // Player is not found
                    else {
                        plugin.debug(header + "Triggered: /premium <player>");
                        // Has permission premium.other
                        if (sender.hasPermission("premium.other")) {
                            plugin.debug(header + "Has permission premium.other");
                            plugin.debug(header + "Player not found");
                            sender.sendMessage(getMessage("messages.player_not_found"));
                        }
                        // No permission premium.other
                        else {
                            plugin.debug(header + "No permission premium.other");
                            sender.sendMessage(getMessage("messages.no_permission"));
                        }
                    }
                }
                // Player is premium
                else if (plugin.isPremium(player)) {
                    plugin.debug(header + "Triggered: /premium <player>");
                    // Has permission premium.other
                    if (sender.hasPermission("premium.other")) {
                        plugin.debug(header + "Has permission premium.other");
                        plugin.debug(header + "Player is premium");
                        sender.sendMessage(getMessage("messages.player_premium"));
                    }
                    // No permission premium.other
                    else {
                        plugin.debug(header + "No permission premium.other");
                        sender.sendMessage(getMessage("messages.no_permission"));
                    }
                }
                // Player is not premium
                else {
                    plugin.debug(header + "Triggered: /premium <player>");
                    // Has permission premium.other
                    if (sender.hasPermission("premium.other")) {
                        plugin.debug(header + "Has permission premium.other");
                        plugin.debug(header + "Player is not premium");
                        sender.sendMessage(getMessage("messages.checking"));
                        plugin.debug(header + "Run validate task");
                        runValidateTask(player, sender);
                    }
                    // No permission premium.other
                    else {
                        plugin.debug(header + "No permission premium.other");
                        sender.sendMessage(getMessage("messages.no_permission"));
                    }
                }
            }
        }
        return true;
    }

    private void runValidateTask(Player player, CommandSender sender) {
        plugin.debug(header + "Starting asynchronous task...");
        String header = this.header + ".AsyncTask] ";
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.debug(header + "Async task started.");
            plugin.debug(header + "Getting local UUID...");
            String uuid = player.getUniqueId().toString().replaceAll("-", ""), query = "";
            plugin.debug(header + "Local UUID: " + uuid);
            plugin.debug(header + "Querying online UUID...");
            try {
                query = queryUUID(player.getName());
            } catch (IOException e) {
                plugin.debug(header + "Network error.");
                e.printStackTrace();
                sender.sendMessage(getMessage("messages.network_error"));
                return;
            } catch (IllegalArgumentException e) {
                plugin.debug(header + "Invalid config.");
                e.printStackTrace();
                sender.sendMessage(getMessage("messages.config_error"));
                return;
            }
            plugin.debug(header + "Online UUID: " + query);
            plugin.debug(header + "Validating...");
            if (uuid.equalsIgnoreCase(query)) {
                plugin.debug(header + "Validate passed.");
                plugin.debug(header + "Modifying data...");
                plugin.setPremium(player, true);
                plugin.debug(header + "Data modified.");
                sender.sendMessage(getMessage("messages.success"));
                plugin.debug(header + "Running commands...");
                Bukkit.getScheduler().runTask(plugin, () -> {
                    String header2 = this.header + ".CommandExecutor] ";
                    for (String cmd : config.getStringList("commands")) {
                        if (plugin.papi()) cmd = PlaceholderAPI.setPlaceholders(player, cmd);
                        plugin.debug(header2 + "Running command: " + cmd);
                        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),cmd);
                    }
                    plugin.debug(header2 + "Commands executed.");
                });
            } else {
                plugin.debug(header + "Validate failed.");
                sender.sendMessage(getMessage("messages.failed"));
            }
            plugin.debug(header + "Saving data...");
            plugin.saveData();
            plugin.debug(header + "Data saved.");
        });
    }

    private String queryUUID(String playerName) throws IllegalArgumentException, IOException {
        plugin.debug(header + "Getting method...");
        String method = config.getString("uuid_source.request.method");
        if (!method.equalsIgnoreCase("get") && !method.equalsIgnoreCase("post")) {
            plugin.debug(header + "Invalid method.");
            throw new IllegalArgumentException("Invalid request method");
        }
        else method = method.toUpperCase();
        plugin.debug(header + "Method: " + method);
        plugin.debug(header + "Getting URL...");
        String url = config.getString("uuid_source.request.url")
                .replaceAll("\\{player}", playerName)
                .replaceAll("\\{timestamp}", String.valueOf(System.currentTimeMillis()));
        plugin.debug(header + "URL: " + url);

        plugin.debug(header + "Starting query...");
        String response;
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            CloseableHttpResponse res;
            switch (method) {
                case "GET":
                    plugin.debug(header + "GET.Initializing...");
                    HttpGet get = new HttpGet(url);
                    plugin.debug(header + "GET.Executing...");
                    res = httpclient.execute(get);
                    response = EntityUtils.toString(res.getEntity());
                    plugin.debug(header + "GET.Response: " + response);
                    break;
                case "POST":
                    plugin.debug(header + "POST.Initializing...");
                    HttpPost post = new HttpPost(url);
                    List<NameValuePair> body = new ArrayList<>();
                    plugin.debug(header + "POST.Setting headers and body...");
                    config.getValues(true).forEach((k, v) -> {
                        if (k.startsWith("uuid_source.post_data.header.")) {
                            String key = k.replaceFirst("uuid_source\\.post_data\\.header\\.", "");
                            plugin.debug(header + "POST.Header: " + key + " = " + v.toString());
                            post.setHeader(key, v.toString());
                        }
                        else if (k.startsWith("uuid_source.post_data.body.")) {
                            String key = k.replaceFirst("uuid_source\\.post_data\\.body\\.", "");
                            plugin.debug(header + "POST.Body: " + key + " = " + v.toString());
                            body.add(new BasicNameValuePair(key, v.toString()));
                        }
                    });
                    post.setEntity(EntityBuilder.create().setParameters(body).build());
                    plugin.debug(header + "POST.Executing...");
                    res = httpclient.execute(post);
                    response = EntityUtils.toString(res.getEntity());
                    plugin.debug(header + "POST.Response: " + response);
                    break;
                default:
                    plugin.debug(header + "Invalid method: " + method);
                    return null;
            }
        }

        plugin.debug(header + "Getting return type...");
        String returnType = config.getString("uuid_source.return.type");
        if (returnType.equalsIgnoreCase("JSON")) {
            plugin.debug(header + "Return type: JSON");
            plugin.debug(header + "Getting JSON path...");
            String path = config.getString("uuid_source.return.path");
            plugin.debug(header + "JSON path: " + path);
            plugin.debug(header + "Parsing value...");
            response = getValueByPath(JSONObject.parseObject(response), path, String.class);
        } else {
            plugin.debug(header + "Return type: TEXT");
        }
        response = response.replaceAll("-", "");
        plugin.debug(header + "Value: " + response);
        return response;
    }

    private String getMessage(String path) {
        return config.getString(path).replaceAll("&", "\u00A7");
    }

    public static <T> T getValueByPath(JSONObject jsonObject, String path, Class<T> clazz) {
        if (jsonObject == null || path == null || path.isEmpty() || clazz == null) {
            return null;
        }
        String[] pathSegments = path.split("\\.");
        for (int i = 0; i < pathSegments.length; i++) {
            String segment = pathSegments[i];
            if (segment.isEmpty()) {
                return null; // Invalid segment
            }
            int indexStart = segment.indexOf("[");
            if (indexStart >= 0) {
                String key = segment.substring(0, indexStart);
                int indexEnd = segment.indexOf("]", indexStart);
                if (indexEnd < 0) {
                    return null; // Invalid segment
                }
                int index = Integer.parseInt(segment.substring(indexStart + 1, indexEnd));
                JSONArray jsonArray = jsonObject.getJSONArray(key);
                if (jsonArray == null || index < 0 || index >= jsonArray.size()) {
                    return null; // Invalid index or key not found
                }
                jsonObject = jsonArray.getJSONObject(index);
            } else {
                Object value = jsonObject.get(segment);
                if (i == pathSegments.length - 1 && value != null) {
                    return clazz.cast(value);
                }
                jsonObject = jsonObject.getJSONObject(segment);
                if (jsonObject == null) {
                    return null; // Key not found or not an object
                }
            }
        }
        return clazz.cast(jsonObject);
    }

}

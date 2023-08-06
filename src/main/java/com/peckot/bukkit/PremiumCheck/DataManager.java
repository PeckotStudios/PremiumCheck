package com.peckot.bukkit.PremiumCheck;

import com.alibaba.fastjson2.JSONObject;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Base64;

public class DataManager {

    private final PremiumCheck plugin;
    private final String header = "[DataManager] ";

    public enum DataType {
        SQLITE,
        YAML,
        JSON
    }
    private DataType type;

    public enum Encryption {
        NONE,
        MD5,
        BASE64,
        SHA1
    }
    private Encryption encryption;

    private Connection sql;
    private FileConfiguration yaml;
    private JSONObject json;

    public DataManager(PremiumCheck plugin, DataType type, Encryption encryption) {
        this.plugin = plugin;
        if (type != DataType.SQLITE) {
            plugin.debug(header + "Initializing data file...");
            String fileName = "data." + (type == DataType.JSON ? "json" : "yml");
            File dataFile = new File(plugin.getDataFolder(), fileName);
            try {
                boolean ret = dataFile.createNewFile();
                if (!ret) throw new IOException("Failed to create data file");
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            plugin.debug(header + "Data file path: " + dataFile.getAbsolutePath());
            try {
                initialize(type, fileName);
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                initialize(type, new File(plugin.getDataFolder(), "data.db").getAbsolutePath());
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }
        this.type = type;
        this.encryption = encryption;
    }

    private void initialize(DataType type, String fileName) throws SQLException, IOException {
        plugin.debug(header + "Initializing data file...");
        switch (type) {
            case JSON:
                plugin.debug(header + "Loading JSON data file...");
                json = JSONObject.parseObject(new String(Files.readAllBytes(new File(plugin.getDataFolder(), fileName).toPath())));
                plugin.debug(header + "JSON data file loaded");
                break;
            case YAML:
                plugin.debug(header + "Loading YAML data file...");
                yaml = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), fileName));
                plugin.debug(header + "YAML data file loaded");
                break;
            default:
                plugin.debug(header + "Loading SQLite database...");
                sql = DriverManager.getConnection("jdbc:sqlite:" + fileName);
                if (sql == null) {
                    plugin.debug(header + "Failed to connect to database");
                    throw new SQLException("Failed to connect to database");
                }
                try (Statement state = sql.createStatement()) {
                    plugin.debug(header + "Creating table if not exists...");
                    String cmd = "CREATE TABLE IF NOT EXISTS users (id TEXT PRIMARY KEY, name TEXT, premium BOOLEAN DEFAULT 0, time INTEGER DEFAULT 0)";
                    state.execute(cmd);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                plugin.debug(header + "SQLite database loaded");
                break;
        }
    }

    private String uuidEncode(Player player) {
        String uuid = player.getUniqueId().toString();
        plugin.debug(header + "Encoding UUID: " + uuid);
        String key = "pkey-" + uuid;
        switch (encryption) {
            case BASE64:
                plugin.debug("Encoding with Base64...");
                key = Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8));
                plugin.debug("Encoded key: " + key);
                return key;
            case MD5:
                plugin.debug("Encoding with MD5...");
                try {
                    byte[] bytes = MessageDigest.getInstance("MD5").digest(key.getBytes(StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    for (byte b : bytes) {
                        sb.append(Integer.toHexString(b & 0xff));
                    }
                    plugin.debug("Encoded key: " + sb.toString());
                    return sb.toString();
                } catch (NoSuchAlgorithmException e) {
                    plugin.debug("Failed to encode with MD5");
                    e.printStackTrace();
                }
                break;
            case NONE:
                plugin.debug("No encoding");
                plugin.debug("UUID key: " + uuid);
                return uuid;
            default:
                plugin.debug("Encoding with SHA-1...");
                try {
                    byte[] bytes = MessageDigest.getInstance("SHA").digest(key.getBytes(StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    for (byte b : bytes) {
                        sb.append(Integer.toHexString(b & 0xff));
                    }
                    plugin.debug("Encoded key: " + sb.toString());
                    return sb.toString();
                } catch (NoSuchAlgorithmException e) {
                    plugin.debug("Failed to encode with SHA-1");
                    e.printStackTrace();
                }
                break;
        }
        plugin.debug("No encoding");
        plugin.debug("UUID key: " + uuid);
        return uuid;
    }

    public void setPremium(Player player, boolean premium) {
        plugin.debug(header + "Setting premium status of " + player.getName() + " to " + premium);
        String key = uuidEncode(player);
        switch (type) {
            case JSON:
                plugin.debug(header + "Setting JSON value...");
                json.put(key, premium);
                plugin.debug(header + "JSON value set");
                break;
            case YAML:
                plugin.debug(header + "Setting YAML value...");
                yaml.set(key, premium);
                plugin.debug(header + "YAML value set");
                break;
            default:
                plugin.debug(header + "Setting SQLite value...");
                String cmd = "INSERT OR REPLACE INTO users (id, name, premium, time) VALUES (?,?,?,?)";
                try (PreparedStatement statement = sql.prepareStatement(cmd)) {
                    statement.setString(1, key);
                    statement.setString(2, player.getName());
                    statement.setInt(3, premium ? 1 : 0);
                    statement.setLong(4, System.currentTimeMillis());
                    plugin.debug(header + "Executing SQL command...");
                    statement.executeUpdate();
                } catch (SQLException e) {
                    plugin.debug(header + "Failed to execute SQL command");
                    e.printStackTrace();
                }
                plugin.debug(header + "SQLite value set");
                break;
        }
    }


    public boolean isPremium(Player player) {
        plugin.debug(header + "Checking premium status of " + player.getName());
        String key = uuidEncode(player);
        boolean status = false;
        switch (type) {
            case JSON:
                status = json.getBoolean(key);
                break;
            case YAML:
                status = yaml.getBoolean(key);
                break;
            default:
                plugin.debug(header + "Checking SQLite value...");
                String cmd = "SELECT premium FROM users WHERE id = '" + key + "'";
                try (Statement statement = sql.createStatement()) {
                    plugin.debug(header + "Executing SQL command...");
                    ResultSet resultSet = statement.executeQuery(cmd);
                    plugin.debug(header + "Got result set: " + resultSet);
                    if (resultSet.next()) {
                        status = resultSet.getBoolean("premium");
                        resultSet.close();
                    }
                } catch (SQLException e) {
                    plugin.debug(header + "Failed to execute SQL command");
                    e.printStackTrace();
                }
                break;
        }
        plugin.debug(header + "Result: " + status);
        return status;
    }


    public void saveData() {
        switch (type) {
            case YAML:
                plugin.debug(header + "Saving YAML data file...");
                try {
                    yaml.save(new File(plugin.getDataFolder(), "data.yml"));
                } catch (IOException e) {
                    plugin.debug(header + "Failed to save YAML data file");
                    e.printStackTrace();
                }
                plugin.debug(header + "YAML data file saved");
                break;
            case JSON:
                plugin.debug(header + "Saving JSON data file...");
                try {
                    Files.write(new File(plugin.getDataFolder(), "data.json").toPath(), json.toJSONString().getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    plugin.debug(header + "Failed to save JSON data file");
                    e.printStackTrace();
                }
                plugin.debug(header + "JSON data file saved");
                break;
        }
    }

    public void destroy() {
        if (type == DataType.SQLITE) {
            plugin.debug(header + "Destroying sqlite connection...");
            try {
                sql.close();
            } catch (SQLException e) {
                plugin.debug(header + "Failed to close sqlite connection");
                e.printStackTrace();
            }
            plugin.debug(header + "Sqlite connection closed");
        }
    }

}

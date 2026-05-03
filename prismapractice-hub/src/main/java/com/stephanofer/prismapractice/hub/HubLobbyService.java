package com.stephanofer.prismapractice.hub;

import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.config.ManagedConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public final class HubLobbyService {

    private static final String CONFIG_ID = "hub-lobby";

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private volatile HubLobbyConfig config;

    public HubLobbyService(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.config = configManager.get(CONFIG_ID, HubLobbyConfig.class);
    }

    public void reload() {
        this.config = configManager.get(CONFIG_ID, HubLobbyConfig.class);
    }

    public boolean isConfigured() {
        return config.lobby() != null;
    }

    public Optional<Location> lobbyLocation() {
        HubLobbyPoint lobby = config.lobby();
        if (lobby == null) {
            return Optional.empty();
        }
        World world = Bukkit.getWorld(lobby.world());
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(new Location(world, lobby.x(), lobby.y(), lobby.z(), lobby.yaw(), lobby.pitch()));
    }

    public boolean teleportToLobby(Player player) {
        Objects.requireNonNull(player, "player");
        Optional<Location> location = lobbyLocation();
        if (location.isEmpty()) {
            return false;
        }
        return player.teleport(location.get());
    }

    public void save(Player player) {
        Objects.requireNonNull(player, "player");
        Location location = player.getLocation();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("config-schema-version", 1);
        yaml.set("lobby.set", true);
        yaml.set("lobby.world", location.getWorld() == null ? "world" : location.getWorld().getName());
        yaml.set("lobby.x", location.getX());
        yaml.set("lobby.y", location.getY());
        yaml.set("lobby.z", location.getZ());
        yaml.set("lobby.yaw", location.getYaw());
        yaml.set("lobby.pitch", location.getPitch());
        ManagedConfig<?> managed = configManager.findManaged(CONFIG_ID).orElseThrow(() -> new IllegalStateException("Missing managed config '" + CONFIG_ID + "'"));
        try {
            yaml.save(managed.file().toFile());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save hub lobby config", exception);
        }
        this.config = new HubLobbyConfig(new HubLobbyPoint(
                location.getWorld() == null ? "world" : location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        ));
    }
}

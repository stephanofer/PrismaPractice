package com.stephanofer.prismapractice.hub;

import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.config.ConfigPlatforms;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class HubLobbyServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void defaultLobbyConfigStartsUnset() {
        ConfigManager manager = new ConfigManager(
                ConfigPlatforms.fromClassLoader(tempDir, getClass().getClassLoader(), new ArrayList<String>()::add),
                List.of(HubLobbyConfigDescriptorFactory.descriptor())
        );
        manager.loadAll();

        HubLobbyService service = new HubLobbyService(Mockito.mock(JavaPlugin.class), manager);

        assertFalse(service.isConfigured());
        assertTrue(service.lobbyLocation().isEmpty());
    }

    @Test
    void saveWritesLobbyCoordinatesToConfig() throws Exception {
        ConfigManager manager = new ConfigManager(
                ConfigPlatforms.fromClassLoader(tempDir, getClass().getClassLoader(), new ArrayList<String>()::add),
                List.of(HubLobbyConfigDescriptorFactory.descriptor())
        );
        manager.loadAll();

        HubLobbyService service = new HubLobbyService(Mockito.mock(JavaPlugin.class), manager);
        Player player = Mockito.mock(Player.class);
        World world = Mockito.mock(World.class);
        when(world.getName()).thenReturn("hub-world");
        when(player.getLocation()).thenReturn(new Location(world, 12.5, 70.0, -4.25, 90.0F, 10.0F));

        service.save(player);

        assertTrue(service.isConfigured());
        String stored = Files.readString(tempDir.resolve("hub-lobby.yml"));
        assertTrue(stored.contains("set: true"));
        assertTrue(stored.contains("world: hub-world"));
        assertTrue(stored.contains("x: 12.5"));
    }
}

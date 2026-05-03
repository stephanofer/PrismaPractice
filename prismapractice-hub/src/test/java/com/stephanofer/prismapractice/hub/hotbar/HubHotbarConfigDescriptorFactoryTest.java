package com.stephanofer.prismapractice.hub.hotbar;

import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.config.ConfigPlatforms;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HubHotbarConfigDescriptorFactoryTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadDefaultHubItemsConfig() {
        ConfigManager manager = new ConfigManager(
                ConfigPlatforms.fromClassLoader(tempDir, getClass().getClassLoader(), new ArrayList<String>()::add),
                List.of(HubHotbarConfigDescriptorFactory.descriptor())
        );

        manager.loadAll();
        HubHotbarConfig config = manager.get("hub-items", HubHotbarConfig.class);

        assertTrue(config.profiles().containsKey("default_hub"));
        assertTrue(config.profiles().containsKey("queue_waiting"));
        HubHotbarProfileConfig defaultHub = config.profiles().get("default_hub");
        assertEquals(0, defaultHub.selectedSlot());
        assertEquals("unranked", defaultHub.items().get("unranked-queue-menu").action().target());
        assertEquals(HubHotbarActionType.OPEN_MENU, defaultHub.items().get("unranked-queue-menu").action().type());
        assertFalse(defaultHub.items().get("unranked-queue-menu").name().isBlank());
    }

    @Test
    void shouldMigrateLegacySlotKeyedItemsToStableKeys() throws Exception {
        Files.writeString(tempDir.resolve("hub-items.yml"), """
                config-schema-version: 1

                profiles:
                  queue_waiting:
                    selected-slot: 0
                    reset-inventory: true
                    constraints:
                      deny-move: true
                      deny-drop: true
                      deny-place: true
                      deny-pickup: true
                      deny-swap-offhand: true
                    items:
                      '0':
                        key: queue-context-browser
                        material: NETHER_STAR
                        amount: 1
                        name: test
                        action:
                          type: CUSTOM
                          trigger: RIGHT_CLICK
                          custom-key: queue-context-source
                      '8':
                        key: leave-queue
                        material: RED_DYE
                        amount: 1
                        name: leave
                        action:
                          type: LEAVE_QUEUE
                          trigger: RIGHT_CLICK
                """);

        ConfigManager manager = new ConfigManager(
                ConfigPlatforms.fromClassLoader(tempDir, getClass().getClassLoader(), new ArrayList<String>()::add),
                List.of(HubHotbarConfigDescriptorFactory.descriptor())
        );

        manager.loadAll();
        HubHotbarConfig config = manager.get("hub-items", HubHotbarConfig.class);

        HubHotbarProfileConfig queueWaiting = config.profiles().get("queue_waiting");
        assertTrue(queueWaiting.items().containsKey("queue-context-browser"));
        assertTrue(queueWaiting.items().containsKey("leave-queue"));
        assertEquals(0, queueWaiting.items().get("queue-context-browser").slot());
        assertEquals(8, queueWaiting.items().get("leave-queue").slot());
        String updated = Files.readString(tempDir.resolve("hub-items.yml"));
        assertTrue(updated.contains("config-schema-version: 2"));
        assertTrue(updated.contains("leave-queue:"));
        assertTrue(updated.contains("slot: 8"));
        assertFalse(updated.contains("'8':"));
    }
}

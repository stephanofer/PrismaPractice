package com.stephanofer.prismapractice.hub.hotbar;

import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.config.ConfigPlatforms;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
        assertEquals("unranked", defaultHub.items().get(0).action().target());
        assertEquals(HubHotbarActionType.OPEN_MENU, defaultHub.items().get(0).action().type());
        assertFalse(defaultHub.items().get(0).name().isBlank());
    }
}

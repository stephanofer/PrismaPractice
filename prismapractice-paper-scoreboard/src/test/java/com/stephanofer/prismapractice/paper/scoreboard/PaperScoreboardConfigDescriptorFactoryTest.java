package com.stephanofer.prismapractice.paper.scoreboard;

import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.config.ConfigPlatforms;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperScoreboardConfigDescriptorFactoryTest {

    @Test
    void loadsScenesAndConditions() throws Exception {
        Path directory = Files.createTempDirectory("paper-scoreboards-config-test");
        ConfigManager manager = new ConfigManager(
                ConfigPlatforms.fromClassLoader(directory, getClass().getClassLoader(), message -> {
                }),
                List.of(PaperScoreboardConfigDescriptorFactory.descriptor("hub-scoreboards", "scoreboards.yml", "defaults/test-scoreboards.yml"))
        );

        manager.loadAll();
        PaperScoreboardConfig config = manager.get("hub-scoreboards", PaperScoreboardConfig.class);

        assertTrue(config.settings().enabled());
        assertEquals(20, config.settings().tickInterval());
        assertTrue(config.scenesByKey().containsKey("hub_default"));
        assertTrue(config.scenesByKey().containsKey("hub_queue"));
        assertTrue(config.scenesByKey().containsKey("hub_party"));
        assertEquals(3, config.scenes().size());
        assertEquals(200, config.scenesByKey().get("hub_queue").priority());
        assertEquals(Set.of(Boolean.TRUE), config.scenesByKey().get("hub_party").conditions().partyMembership());
    }

    @Test
    void skipsDisabledScenesAndMigratesSettingsFlag() throws Exception {
        Path directory = Files.createTempDirectory("paper-scoreboards-config-disabled-test");
        Files.writeString(directory.resolve("scoreboards.yml"), """
                config-schema-version: 1

                settings:
                  tick-interval: 20
                  default-refresh-ticks: 20
                  hide-when-disabled-in-settings: true
                  allow-placeholderapi: false

                scenes:
                  hub_default:
                    priority: 100
                    conditions:
                      runtime-types: [HUB]
                    title: "<aqua>Practice"
                    lines:
                      - "<gray>Hello"

                  hub_ranked_menu:
                    enabled: false
                    priority: 90
                    conditions:
                      ui-focuses: [RANKED_MENU]
                    title: "<red>Ranked"
                    lines:
                      - "<white>ranked"
                """);

        ConfigManager manager = new ConfigManager(
                ConfigPlatforms.fromClassLoader(directory, getClass().getClassLoader(), message -> {
                }),
                List.of(PaperScoreboardConfigDescriptorFactory.descriptor("hub-scoreboards", "scoreboards.yml", "defaults/test-scoreboards.yml"))
        );

        manager.loadAll();
        PaperScoreboardConfig config = manager.get("hub-scoreboards", PaperScoreboardConfig.class);

        assertTrue(config.settings().enabled());
        assertTrue(config.scenesByKey().containsKey("hub_default"));
        assertTrue(!config.scenesByKey().containsKey("hub_ranked_menu"));
        String updated = Files.readString(directory.resolve("scoreboards.yml"));
        assertTrue(updated.contains("config-schema-version: 2"));
        assertTrue(updated.contains("enabled: true"));
    }
}

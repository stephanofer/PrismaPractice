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

        assertEquals(20, config.settings().tickInterval());
        assertTrue(config.scenesByKey().containsKey("hub_default"));
        assertTrue(config.scenesByKey().containsKey("hub_queue"));
        assertTrue(config.scenesByKey().containsKey("hub_party"));
        assertEquals(3, config.scenes().size());
        assertEquals(200, config.scenesByKey().get("hub_queue").priority());
        assertEquals(Set.of(Boolean.TRUE), config.scenesByKey().get("hub_party").conditions().partyMembership());
    }
}

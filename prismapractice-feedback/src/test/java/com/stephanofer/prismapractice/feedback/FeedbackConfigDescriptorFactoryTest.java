package com.stephanofer.prismapractice.feedback;

import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.config.ConfigPlatforms;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class FeedbackConfigDescriptorFactoryTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadFeedbackTemplatesWithPersistentActionBarAndBossBar() throws Exception {
        ConfigManager manager = new ConfigManager(
                ConfigPlatforms.fromClassLoader(tempDir, getClass().getClassLoader(), new ArrayList<String>()::add),
                List.of(FeedbackConfigDescriptorFactory.descriptor("feedback", "feedback.yml", "defaults/test-feedback.yml"))
        );

        manager.loadAll();

        FeedbackConfig config = manager.get("feedback", FeedbackConfig.class);
        FeedbackTemplate queueSearching = config.template("queue-searching");
        ActionBarFeedbackDelivery actionBar = assertInstanceOf(ActionBarFeedbackDelivery.class, queueSearching.deliveries().getFirst());
        assertEquals(FeedbackDeliveryMode.PERSISTENT, actionBar.mode());
        assertEquals("queue-search", actionBar.persistence().slot());
        assertEquals(20, actionBar.persistence().intervalTicks());
        assertEquals(100, actionBar.persistence().priority());

        FeedbackTemplate queueFound = config.template("queue-found");
        BossBarFeedbackDelivery bossBar = assertInstanceOf(BossBarFeedbackDelivery.class, queueFound.deliveries().get(2));
        assertEquals(FeedbackDeliveryMode.ONE_SHOT, bossBar.mode());
        assertEquals(60, bossBar.durationTicks());
    }

    @Test
    void shouldCreateMissingFeedbackConfigFromDefaults() throws Exception {
        ConfigManager manager = new ConfigManager(
                ConfigPlatforms.fromClassLoader(tempDir, getClass().getClassLoader(), new ArrayList<String>()::add),
                List.of(FeedbackConfigDescriptorFactory.descriptor("feedback", "feedback.yml", "defaults/test-feedback.yml"))
        );

        manager.loadAll();

        assertEquals(true, Files.exists(tempDir.resolve("feedback.yml")));
    }
}

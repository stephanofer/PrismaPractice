package com.stephanofer.prismapractice.debug;

import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.config.ConfigPlatforms;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DebugConfigDescriptorFactoryTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadDefaultDebugConfig() {
        ConfigManager manager = new ConfigManager(
                ConfigPlatforms.fromClassLoader(tempDir, getClass().getClassLoader(), message -> {
                }),
                List.of(DebugConfigDescriptorFactory.descriptor())
        );

        manager.loadAll();
        DebugConfig config = manager.get("runtime-debug", DebugConfig.class);

        assertEquals(DebugSeverity.WARN, config.consoleSeverity());
        assertEquals(DebugDetailLevel.BASIC, config.categoryLevel(DebugCategories.BOOTSTRAP));
        assertEquals(DebugDetailLevel.OFF, config.categoryLevel("unknown-category"));
    }
}

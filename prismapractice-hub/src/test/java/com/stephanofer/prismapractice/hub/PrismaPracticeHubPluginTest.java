package com.stephanofer.prismapractice.hub;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PrismaPracticeHubPluginTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldBootstrapDemoConfigsAndLogValues() {
        List<String> logs = new ArrayList<>();

        HubDemoConfigBootstrap.bootstrap(tempDir, getClass().getClassLoader(), logs::add);

        assertTrue(Files.exists(tempDir.resolve("config.yml")));
        assertTrue(Files.exists(tempDir.resolve("messages/demo.yml")));
        assertTrue(logs.stream().anyMatch(log -> log.contains("sample-text=hub-default-text")));
        assertTrue(logs.stream().anyMatch(log -> log.contains("greeting=hub says hello")));
        assertTrue(logs.stream().anyMatch(log -> log.contains("created=") && log.contains("config.yml") && log.contains("messages/demo.yml")));
    }
}

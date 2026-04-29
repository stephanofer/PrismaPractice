package com.stephanofer.prismapractice.ffa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PrismaPracticeFfaPluginTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldBootstrapDemoConfigsAndLogValues() {
        List<String> logs = new ArrayList<>();

        FfaDemoConfigBootstrap.bootstrap(tempDir, getClass().getClassLoader(), logs::add);

        assertTrue(Files.exists(tempDir.resolve("config.yml")));
        assertTrue(Files.exists(tempDir.resolve("messages/demo.yml")));
        assertTrue(logs.stream().anyMatch(log -> log.contains("sample-text=ffa-default-text")));
        assertTrue(logs.stream().anyMatch(log -> log.contains("greeting=ffa says hello")));
        assertTrue(logs.stream().anyMatch(log -> log.contains("created=") && log.contains("config.yml") && log.contains("messages/demo.yml")));
    }
}

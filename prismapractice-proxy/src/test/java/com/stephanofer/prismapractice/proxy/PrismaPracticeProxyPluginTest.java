package com.stephanofer.prismapractice.proxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PrismaPracticeProxyPluginTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldBootstrapDemoConfigsAndLogValues() {
        List<String> logs = new ArrayList<>();

        ProxyDemoConfigBootstrap.bootstrap(tempDir, getClass().getClassLoader(), logs::add);

        assertTrue(Files.exists(tempDir.resolve("config.yml")));
        assertTrue(Files.exists(tempDir.resolve("messages/demo.yml")));
        assertTrue(logs.stream().anyMatch(log -> log.contains("sample-text=proxy-default-text")));
        assertTrue(logs.stream().anyMatch(log -> log.contains("greeting=proxy says hello")));
        assertTrue(logs.stream().anyMatch(log -> log.contains("created=") && log.contains("config.yml") && log.contains("messages/demo.yml")));
    }
}

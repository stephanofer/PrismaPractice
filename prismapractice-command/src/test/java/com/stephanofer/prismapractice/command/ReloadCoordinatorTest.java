package com.stephanofer.prismapractice.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReloadCoordinatorTest {

    @Test
    void shouldExecuteAllScopesInRegistrationOrder() {
        StringBuilder executionOrder = new StringBuilder();
        ReloadCoordinator coordinator = new ReloadCoordinator()
                .register("config", "base config", () -> {
                    executionOrder.append("config>");
                    return ReloadResult.of("config-ok");
                })
                .register("scoreboard", "scoreboard", () -> {
                    executionOrder.append("scoreboard>");
                    return ReloadResult.of("scoreboard-ok");
                });

        ReloadReport report = coordinator.reload(null);

        assertTrue(report.successful());
        assertEquals(List.of("config", "scoreboard"), report.resolvedScopes());
        assertEquals("config>scoreboard>", executionOrder.toString());
    }

    @Test
    void shouldStopAfterFirstFailure() {
        ReloadCoordinator coordinator = new ReloadCoordinator()
                .register("config", "base config", () -> ReloadResult.of("config-ok"))
                .register("scoreboard", "scoreboard", () -> {
                    throw new IllegalStateException("broken-scene");
                })
                .register("ui", "ui", () -> ReloadResult.of("ui-ok"));

        ReloadReport report = coordinator.reload("all");

        assertFalse(report.successful());
        assertEquals("scoreboard", report.failureScope());
        assertEquals(2, report.entries().size());
    }

    @Test
    void shouldResolveDependenciesBeforeRequestedScope() {
        StringBuilder executionOrder = new StringBuilder();
        ReloadCoordinator coordinator = new ReloadCoordinator()
                .register("config", "base config", () -> {
                    executionOrder.append("config>");
                    return ReloadResult.of("config-ok");
                })
                .register("hotbar", "hotbar", List.of("config"), () -> {
                    executionOrder.append("hotbar>");
                    return ReloadResult.of("hotbar-ok");
                });

        ReloadReport report = coordinator.reload("hotbar");

        assertTrue(report.successful());
        assertEquals(List.of("config", "hotbar"), report.resolvedScopes());
        assertEquals("config>hotbar>", executionOrder.toString());
    }

    @Test
    void shouldDeduplicateSharedDependencies() {
        StringBuilder executionOrder = new StringBuilder();
        ReloadCoordinator coordinator = new ReloadCoordinator()
                .register("config", "base config", () -> {
                    executionOrder.append("config>");
                    return ReloadResult.of("config-ok");
                })
                .register("feedback", "feedback", List.of("config"), () -> {
                    executionOrder.append("feedback>");
                    return ReloadResult.of("feedback-ok");
                })
                .register("scoreboard", "scoreboard", List.of("config"), () -> {
                    executionOrder.append("scoreboard>");
                    return ReloadResult.of("scoreboard-ok");
                });

        ReloadReport report = coordinator.reload("all");

        assertTrue(report.successful());
        assertEquals("config>feedback>scoreboard>", executionOrder.toString());
    }

    @Test
    void shouldExposeAllAndRegisteredScopes() {
        ReloadCoordinator coordinator = new ReloadCoordinator()
                .register("config", "base config", () -> ReloadResult.of("ok"));

        assertEquals(List.of("all", "config"), coordinator.scopes());
    }

    @Test
    void shouldRejectUnknownScope() {
        ReloadCoordinator coordinator = new ReloadCoordinator()
                .register("config", "base config", () -> ReloadResult.of("ok"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> coordinator.reload("missing"));
        assertTrue(exception.getMessage().contains("Available: all, config"));
    }
}

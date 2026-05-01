package com.stephanofer.prismapractice.debug;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DebugControllerTest {

    @Test
    void shouldReloadConfigAndResizeBufferWithoutDroppingNewestEvents() {
        DebugController controller = new DebugController(
                "hub",
                new DebugConfig(
                        true,
                        DebugSeverity.WARN,
                        4,
                        DebugDetailLevel.BASIC,
                        java.util.Map.of(DebugCategories.SCOREBOARD, DebugDetailLevel.BASIC),
                        new DebugConfig.SlowThresholds(25L, 50L, 20L, 15L),
                        new DebugConfig.WatchSettings(10, 60),
                        true
                ),
                DebugConsoleSink.noop()
        );

        controller.debug(DebugCategories.SCOREBOARD, DebugDetailLevel.BASIC, "one", "first", controller.context().build());
        controller.debug(DebugCategories.SCOREBOARD, DebugDetailLevel.BASIC, "two", "second", controller.context().build());
        controller.debug(DebugCategories.SCOREBOARD, DebugDetailLevel.BASIC, "three", "third", controller.context().build());

        controller.reload(new DebugConfig(
                true,
                DebugSeverity.WARN,
                2,
                DebugDetailLevel.OFF,
                java.util.Map.of(DebugCategories.SCOREBOARD, DebugDetailLevel.TRACE),
                new DebugConfig.SlowThresholds(25L, 50L, 20L, 15L),
                new DebugConfig.WatchSettings(10, 60),
                true
        ));

        controller.debug(DebugCategories.SCOREBOARD, DebugDetailLevel.TRACE, "four", "fourth", controller.context().build());

        List<DebugEvent> recent = controller.recent(5);
        assertEquals(2, controller.config().ringBufferSize());
        assertEquals(2, controller.bufferedEventCount());
        assertEquals(List.of("four", "three"), recent.stream().map(DebugEvent::name).toList());
    }
}

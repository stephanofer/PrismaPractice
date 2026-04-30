package com.stephanofer.prismapractice.paper.feedback;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistentActionBarRegistryTest {

    @Test
    void shouldPreferHigherPriorityEntry() {
        PersistentActionBarRegistry registry = new PersistentActionBarRegistry();
        registry.upsert("low", Component.text("low"), 20, 10);
        registry.upsert("high", Component.text("high"), 20, 100);

        PersistentActionBarRegistry.TickDecision decision = registry.tick(1L);

        assertTrue(decision.shouldRender());
        assertEquals(Component.text("high"), decision.component());
    }

    @Test
    void shouldPreferMostRecentEntryWhenPriorityTies() {
        PersistentActionBarRegistry registry = new PersistentActionBarRegistry();
        registry.upsert("first", Component.text("first"), 20, 50);
        registry.upsert("second", Component.text("second"), 20, 50);

        PersistentActionBarRegistry.TickDecision decision = registry.tick(1L);

        assertTrue(decision.shouldRender());
        assertEquals(Component.text("second"), decision.component());
    }

    @Test
    void shouldClearDisplayWhenLastEntryIsRemoved() {
        PersistentActionBarRegistry registry = new PersistentActionBarRegistry();
        registry.upsert("queue", Component.text("queue"), 20, 100);
        registry.tick(1L);

        registry.clear("queue");
        PersistentActionBarRegistry.TickDecision decision = registry.tick(2L);

        assertTrue(decision.shouldRender());
        assertTrue(decision.clearDisplay());
        assertEquals(Component.empty(), decision.component());
        assertFalse(registry.tick(3L).shouldRender());
    }
}

package com.stephanofer.prismapractice.paper.feedback;

import net.kyori.adventure.text.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final class PersistentActionBarRegistry {

    private final Map<String, Entry> entries = new HashMap<>();
    private long sequenceCounter;
    private String lastRenderedSlot;

    void upsert(String slot, Component component, int intervalTicks, int priority) {
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(component, "component");
        Entry entry = entries.computeIfAbsent(slot, Entry::new);
        entry.component = component;
        entry.intervalTicks = intervalTicks;
        entry.priority = priority;
        entry.sequence = ++sequenceCounter;
        entry.nextRenderTick = 0L;
    }

    boolean clear(String slot) {
        Objects.requireNonNull(slot, "slot");
        Entry removed = entries.remove(slot);
        return removed != null;
    }

    void clear() {
        entries.clear();
        lastRenderedSlot = null;
    }

    boolean isEmpty() {
        return entries.isEmpty();
    }

    TickDecision tick(long currentTick) {
        Optional<Entry> active = active();
        if (active.isEmpty()) {
            if (lastRenderedSlot != null) {
                lastRenderedSlot = null;
                return TickDecision.clear();
            }
            return TickDecision.idle();
        }

        Entry entry = active.get();
        if (!entry.slot.equals(lastRenderedSlot) || currentTick >= entry.nextRenderTick) {
            entry.nextRenderTick = currentTick + entry.intervalTicks;
            lastRenderedSlot = entry.slot;
            return TickDecision.render(entry.component);
        }

        return TickDecision.idle();
    }

    private Optional<Entry> active() {
        return entries.values().stream()
                .max(Comparator.comparingInt(Entry::priority).thenComparingLong(Entry::sequence));
    }

    record TickDecision(Component component, boolean clearDisplay) {

        static TickDecision render(Component component) {
            return new TickDecision(component, false);
        }

        static TickDecision clear() {
            return new TickDecision(Component.empty(), true);
        }

        static TickDecision idle() {
            return new TickDecision(null, false);
        }

        boolean shouldRender() {
            return component != null;
        }
    }

    private static final class Entry {
        private final String slot;
        private Component component = Component.empty();
        private int intervalTicks = 20;
        private int priority;
        private long sequence;
        private long nextRenderTick;

        private Entry(String slot) {
            this.slot = slot;
        }

        private int priority() {
            return priority;
        }

        private long sequence() {
            return sequence;
        }
    }
}

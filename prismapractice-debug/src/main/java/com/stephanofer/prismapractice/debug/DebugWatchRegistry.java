package com.stephanofer.prismapractice.debug;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

final class DebugWatchRegistry {

    private final Clock clock;
    private final AtomicLong sequence = new AtomicLong();
    private final Map<Long, Watch> watches = new ConcurrentHashMap<>();

    DebugWatchRegistry(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    Watch add(WatchType type, String subject, DebugDetailLevel level, Duration duration) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(duration, "duration");
        pruneExpired();
        long id = sequence.incrementAndGet();
        Watch watch = new Watch(id, type, normalize(type, subject), level, Instant.now(clock).plus(duration));
        watches.put(id, watch);
        return watch;
    }

    void clear() {
        watches.clear();
    }

    int clearByType(WatchType type) {
        pruneExpired();
        int removed = 0;
        for (Watch watch : new ArrayList<>(watches.values())) {
            if (watch.type() == type && watches.remove(watch.id()) != null) {
                removed++;
            }
        }
        return removed;
    }

    Resolution resolve(String category, DebugContext context) {
        pruneExpired();
        DebugDetailLevel resolved = DebugDetailLevel.OFF;
        boolean matched = false;
        for (Watch watch : watches.values()) {
            if (!matches(watch, category, context)) {
                continue;
            }
            matched = true;
            resolved = DebugDetailLevel.max(resolved, watch.level());
        }
        return new Resolution(matched, resolved);
    }

    List<Watch> active() {
        pruneExpired();
        return watches.values().stream()
                .sorted(Comparator.comparing(Watch::expiresAt).thenComparing(Watch::id))
                .toList();
    }

    private boolean matches(Watch watch, String category, DebugContext context) {
        return switch (watch.type()) {
            case CATEGORY -> DebugCategories.normalize(category).equals(watch.subject());
            case PLAYER -> subject(context, "playerName", "playerId").equals(watch.subject());
            case MATCH -> subject(context, "matchId").equals(watch.subject());
            case TRACE -> subject(context, "traceId").equals(watch.subject());
        };
    }

    private String subject(DebugContext context, String... keys) {
        for (String key : keys) {
            Object value = context.get(key);
            if (value != null) {
                return normalize(WatchType.PLAYER, value.toString());
            }
        }
        return "";
    }

    private String normalize(WatchType type, String subject) {
        return type == WatchType.CATEGORY
                ? DebugCategories.normalize(subject)
                : subject.trim().toLowerCase(Locale.ROOT);
    }

    private void pruneExpired() {
        Instant now = Instant.now(clock);
        for (Watch watch : new ArrayList<>(watches.values())) {
            if (!watch.expiresAt().isAfter(now)) {
                watches.remove(watch.id());
            }
        }
    }

    enum WatchType {
        PLAYER,
        MATCH,
        TRACE,
        CATEGORY
    }

    record Watch(long id, WatchType type, String subject, DebugDetailLevel level, Instant expiresAt) {
    }

    record Resolution(boolean matched, DebugDetailLevel level) {
    }
}

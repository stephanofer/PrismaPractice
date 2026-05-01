package com.stephanofer.prismapractice.debug;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class DebugController {

    private static final DebugController NOOP = new DebugController(
            "noop",
            DebugConfig.defaults(),
            DebugConsoleSink.noop(),
            Clock.systemUTC(),
            true
    );

    private final String runtimeName;
    private final DebugConfig config;
    private final DebugConsoleSink sink;
    private final Clock clock;
    private final DebugWatchRegistry watchRegistry;
    private final DebugRingBuffer ringBuffer;
    private final Map<String, DebugDetailLevel> runtimeCategoryOverrides;
    private final AtomicLong sequence;
    private final boolean noop;

    public DebugController(String runtimeName, DebugConfig config, DebugConsoleSink sink) {
        this(runtimeName, config, sink, Clock.systemUTC(), false);
    }

    DebugController(String runtimeName, DebugConfig config, DebugConsoleSink sink, Clock clock, boolean noop) {
        this.runtimeName = Objects.requireNonNull(runtimeName, "runtimeName");
        this.config = Objects.requireNonNull(config, "config");
        this.sink = Objects.requireNonNull(sink, "sink");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.watchRegistry = new DebugWatchRegistry(clock);
        this.ringBuffer = new DebugRingBuffer(config.ringBufferSize());
        this.runtimeCategoryOverrides = new ConcurrentHashMap<>();
        this.sequence = new AtomicLong();
        this.noop = noop;
    }

    public static DebugController noop() {
        return NOOP;
    }

    public String runtimeName() {
        return runtimeName;
    }

    public DebugConfig config() {
        return config;
    }

    public boolean enabled() {
        return config.enabled();
    }

    public DebugContext.Builder context() {
        return DebugContext.builder().runtime(runtimeName);
    }

    public void debug(String category, DebugDetailLevel detailLevel, String name, String message, DebugContext context) {
        emit(category, detailLevel, DebugSeverity.DEBUG, name, message, context, null);
    }

    public void info(String category, DebugDetailLevel detailLevel, String name, String message, DebugContext context) {
        emit(category, detailLevel, DebugSeverity.INFO, name, message, context, null);
    }

    public void warn(String category, String name, String message, DebugContext context) {
        emit(category, DebugDetailLevel.BASIC, DebugSeverity.WARN, name, message, context, null);
    }

    public void error(String category, String name, String message, DebugContext context, Throwable throwable) {
        emit(category, DebugDetailLevel.BASIC, DebugSeverity.ERROR, name, message, context, throwable);
    }

    public void emit(String category, DebugDetailLevel detailLevel, DebugSeverity severity, String name, String message, DebugContext context, Throwable throwable) {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(detailLevel, "detailLevel");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(message, "message");
        if (noop) {
            return;
        }

        DebugContext resolvedContext = context == null ? context().build() : context().build().merge(context);
        DebugWatchRegistry.Resolution watchResolution = watchRegistry.resolve(category, resolvedContext);
        DebugDetailLevel configuredLevel = effectiveCategoryLevel(category);
        DebugDetailLevel allowedLevel = DebugDetailLevel.max(configuredLevel, watchResolution.level());
        boolean detailPermitted = allowedLevel.permits(detailLevel);
        boolean alwaysVisible = severity.isAtLeast(DebugSeverity.WARN);

        if (!detailPermitted && !alwaysVisible) {
            return;
        }

        Map<String, String> fields = sanitizeFields(resolvedContext.fields());
        String throwableSummary = throwable == null ? null : throwableSummary(throwable);
        DebugEvent event = new DebugEvent(
                sequence.incrementAndGet(),
                Instant.now(clock),
                runtimeName,
                DebugCategories.normalize(category),
                name,
                severity,
                detailLevel,
                message,
                fields,
                throwableSummary,
                watchResolution.matched()
        );
        ringBuffer.add(event);

        if (watchResolution.matched() || severity.isAtLeast(config.consoleSeverity())) {
            logToConsole(event, throwable);
        }
    }

    public <T> T measure(String category, String operation, DebugContext context, long slowThresholdMs, Supplier<T> supplier) {
        long startedAt = System.nanoTime();
        try {
            T value = supplier.get();
            long elapsedMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
            if (elapsedMs >= slowThresholdMs) {
                warn(category, operation + ".slow", "Slow operation detected", withDuration(context, elapsedMs));
            }
            return value;
        } catch (RuntimeException exception) {
            error(category, operation + ".failed", "Operation failed", context, exception);
            throw exception;
        }
    }

    public void measure(String category, String operation, DebugContext context, long slowThresholdMs, Runnable runnable) {
        measure(category, operation, context, slowThresholdMs, () -> {
            runnable.run();
            return null;
        });
    }

    public List<DebugEvent> recent(int limit) {
        return ringBuffer.recent(limit, event -> true);
    }

    public int bufferedEventCount() {
        return ringBuffer.size();
    }

    public List<DebugEvent> recent(int limit, Predicate<DebugEvent> filter) {
        return ringBuffer.recent(limit, filter);
    }

    public DebugWatch watchPlayer(String playerNameOrId, DebugDetailLevel level, Duration duration) {
        return toPublicWatch(watchRegistry.add(DebugWatchRegistry.WatchType.PLAYER, playerNameOrId, level, duration));
    }

    public DebugWatch watchMatch(String matchId, DebugDetailLevel level, Duration duration) {
        return toPublicWatch(watchRegistry.add(DebugWatchRegistry.WatchType.MATCH, matchId, level, duration));
    }

    public DebugWatch watchTrace(String traceId, DebugDetailLevel level, Duration duration) {
        return toPublicWatch(watchRegistry.add(DebugWatchRegistry.WatchType.TRACE, traceId, level, duration));
    }

    public DebugWatch watchCategory(String category, DebugDetailLevel level, Duration duration) {
        return toPublicWatch(watchRegistry.add(DebugWatchRegistry.WatchType.CATEGORY, category, level, duration));
    }

    public void clearWatches() {
        watchRegistry.clear();
    }

    public int clearCategoryWatches() {
        return watchRegistry.clearByType(DebugWatchRegistry.WatchType.CATEGORY);
    }

    public List<DebugWatch> activeWatches() {
        return watchRegistry.active().stream().map(this::toPublicWatch).toList();
    }

    public void setRuntimeCategoryLevel(String category, DebugDetailLevel level) {
        runtimeCategoryOverrides.put(DebugCategories.normalize(category), level);
    }

    public void clearRuntimeCategoryLevels() {
        runtimeCategoryOverrides.clear();
    }

    public Map<String, DebugDetailLevel> runtimeCategoryLevels() {
        return runtimeCategoryOverrides.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> right,
                        LinkedHashMap::new
                ));
    }

    private DebugWatch toPublicWatch(DebugWatchRegistry.Watch watch) {
        return new DebugWatch(watch.id(), watch.type().name().toLowerCase(Locale.ROOT), watch.subject(), watch.level(), watch.expiresAt());
    }

    private DebugContext withDuration(DebugContext context, long elapsedMs) {
        return (context == null ? context().build() : context().build().merge(context))
                .merge(DebugContext.builder().field("durationMs", elapsedMs).build());
    }

    private DebugDetailLevel effectiveCategoryLevel(String category) {
        DebugDetailLevel configured = config.categoryLevel(category);
        DebugDetailLevel overridden = runtimeCategoryOverrides.getOrDefault(DebugCategories.normalize(category), DebugDetailLevel.OFF);
        return DebugDetailLevel.max(configured, overridden);
    }

    private void logToConsole(DebugEvent event, Throwable throwable) {
        String formatted = format(event);
        switch (event.severity()) {
            case DEBUG -> sink.debug(formatted);
            case INFO -> sink.info(formatted);
            case WARN -> sink.warn(formatted);
            case ERROR -> sink.error(formatted);
        }
        if (throwable != null && config.includeExceptionStackTraces()) {
            switch (event.severity()) {
                case DEBUG -> sink.debug(stackTrace(throwable));
                case INFO -> sink.info(stackTrace(throwable));
                case WARN -> sink.warn(stackTrace(throwable));
                case ERROR -> sink.error(stackTrace(throwable));
            }
        }
    }

    private String format(DebugEvent event) {
        StringBuilder builder = new StringBuilder("[debug]")
                .append(" runtime=").append(event.runtime())
                .append(", category=").append(event.category())
                .append(", event=").append(event.name())
                .append(", severity=").append(event.severity().name().toLowerCase(Locale.ROOT))
                .append(", detail=").append(event.detailLevel().name().toLowerCase(Locale.ROOT));
        if (event.watchTriggered()) {
            builder.append(", watch=true");
        }
        builder.append(" :: ").append(event.message());
        if (!event.fields().isEmpty()) {
            builder.append(" | ");
            boolean first = true;
            for (Map.Entry<String, String> entry : event.fields().entrySet()) {
                if (!first) {
                    builder.append(", ");
                }
                builder.append(entry.getKey()).append('=').append(entry.getValue());
                first = false;
            }
        }
        if (event.throwableSummary() != null && !event.throwableSummary().isBlank()) {
            builder.append(" | exception=").append(event.throwableSummary());
        }
        return builder.toString();
    }

    private Map<String, String> sanitizeFields(Map<String, Object> rawFields) {
        Map<String, String> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : rawFields.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String key = entry.getKey();
            String lowerKey = key.toLowerCase(Locale.ROOT);
            if (lowerKey.contains("password") || lowerKey.contains("secret") || (lowerKey.contains("token") && !lowerKey.equals("traceid"))) {
                sanitized.put(key, "<redacted>");
                continue;
            }
            String value = String.valueOf(entry.getValue()).replace('\n', ' ').replace('\r', ' ');
            if (value.length() > 220) {
                value = value.substring(0, 217) + "...";
            }
            sanitized.put(key, value);
        }
        return Map.copyOf(sanitized);
    }

    private String throwableSummary(Throwable throwable) {
        return throwable.getClass().getSimpleName() + ": " + (throwable.getMessage() == null ? "<no-message>" : throwable.getMessage());
    }

    private String stackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private static final class DebugRingBuffer {

        private final int capacity;
        private final Deque<DebugEvent> events;

        private DebugRingBuffer(int capacity) {
            this.capacity = capacity;
            this.events = new ArrayDeque<>(capacity);
        }

        private synchronized void add(DebugEvent event) {
            if (events.size() == capacity) {
                events.removeFirst();
            }
            events.addLast(event);
        }

        private synchronized List<DebugEvent> recent(int limit, Predicate<DebugEvent> filter) {
            List<DebugEvent> matched = new ArrayList<>();
            for (DebugEvent event : events) {
                if (filter.test(event)) {
                    matched.add(event);
                }
            }
            matched.sort(Comparator.comparingLong(DebugEvent::sequence).reversed());
            if (matched.size() <= limit) {
                return List.copyOf(matched);
            }
            return List.copyOf(matched.subList(0, limit));
        }

        private synchronized int size() {
            return events.size();
        }
    }
}

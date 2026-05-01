package com.stephanofer.prismapractice.paper.ui.dialog;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class DialogSession {

    private final String playerKey;
    private final Duration ttl;
    private final Deque<String> history;
    private final Map<String, String> values;
    private Instant lastAccessAt;
    private String currentDialogId;

    DialogSession(String playerKey, Duration ttl) {
        this.playerKey = Objects.requireNonNull(playerKey, "playerKey");
        this.ttl = Objects.requireNonNull(ttl, "ttl");
        this.history = new ArrayDeque<>();
        this.values = new LinkedHashMap<>();
        this.lastAccessAt = Instant.now();
    }

    public String playerKey() {
        return playerKey;
    }

    public String currentDialogId() {
        return currentDialogId;
    }

    public void markCurrent(String dialogId) {
        touch();
        this.currentDialogId = dialogId;
    }

    public void pushHistory(String dialogId) {
        if (dialogId == null || dialogId.isBlank()) {
            return;
        }
        touch();
        history.push(dialogId);
    }

    public String popHistory() {
        touch();
        return history.poll();
    }

    public void put(String key, String value) {
        if (key == null || key.isBlank() || value == null) {
            return;
        }
        touch();
        values.put(key, value);
    }

    public void putAll(Map<String, String> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        touch();
        entries.forEach(this::put);
    }

    public String value(String key) {
        touch();
        return values.get(key);
    }

    public Map<String, String> snapshot() {
        touch();
        return Map.copyOf(values);
    }

    public void clear() {
        values.clear();
        history.clear();
        currentDialogId = null;
        touch();
    }

    public boolean isExpired(Instant now) {
        return lastAccessAt.plus(ttl).isBefore(now);
    }

    public void touch() {
        this.lastAccessAt = Instant.now();
    }
}

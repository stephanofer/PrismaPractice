package com.stephanofer.prismapractice.paper.ui.dialog;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class DialogSessionStore {

    private final Duration ttl;
    private final Map<String, DialogSession> sessions;

    public DialogSessionStore(Duration ttl) {
        this.ttl = Objects.requireNonNull(ttl, "ttl");
        this.sessions = new ConcurrentHashMap<>();
    }

    public DialogSession session(String playerKey) {
        pruneExpired();
        return sessions.compute(playerKey, (key, existing) -> {
            if (existing == null || existing.isExpired(Instant.now())) {
                return new DialogSession(key, ttl);
            }
            existing.touch();
            return existing;
        });
    }

    public Optional<DialogSession> find(String playerKey) {
        pruneExpired();
        DialogSession session = sessions.get(playerKey);
        if (session == null) {
            return Optional.empty();
        }
        session.touch();
        return Optional.of(session);
    }

    public void remove(String playerKey) {
        sessions.remove(playerKey);
    }

    public void clear() {
        sessions.clear();
    }

    public void pruneExpired() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }
}

package com.stephanofer.prismapractice.debug;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class DebugCategories {

    public static final String BOOTSTRAP = "bootstrap";
    public static final String COMMAND = "command";
    public static final String PROFILE = "profile";
    public static final String PLAYER_LIFECYCLE = "player.lifecycle";
    public static final String PLAYER_STATE = "state";
    public static final String PERMISSION = "permission";
    public static final String QUEUE = "queue";
    public static final String MATCH = "match";
    public static final String STORAGE_MYSQL = "storage.mysql";
    public static final String STORAGE_REDIS = "storage.redis";
    public static final String REDIS_CONNECTION = "redis.connection";
    public static final String SCOREBOARD = "scoreboard";
    public static final String UI = "ui";
    public static final String RELOAD = "reload";

    private static final Set<String> KNOWN = Set.of(
            BOOTSTRAP,
            COMMAND,
            PROFILE,
            PLAYER_LIFECYCLE,
            PLAYER_STATE,
            PERMISSION,
            QUEUE,
            MATCH,
            STORAGE_MYSQL,
            STORAGE_REDIS,
            REDIS_CONNECTION,
            SCOREBOARD,
            UI,
            RELOAD
    );

    private DebugCategories() {
    }

    public static String normalize(String category) {
        return category == null ? "" : category.trim().toLowerCase(Locale.ROOT);
    }

    public static Set<String> known() {
        return new LinkedHashSet<>(KNOWN);
    }
}

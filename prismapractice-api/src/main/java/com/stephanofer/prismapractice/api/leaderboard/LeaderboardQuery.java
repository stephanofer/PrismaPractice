package com.stephanofer.prismapractice.api.leaderboard;

import java.util.Objects;

public record LeaderboardQuery(LeaderboardScope scope, int limit, int offset) {

    public LeaderboardQuery {
        Objects.requireNonNull(scope, "scope");
        if (limit <= 0 || limit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0");
        }
    }
}

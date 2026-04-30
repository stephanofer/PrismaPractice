package com.stephanofer.prismapractice.api.leaderboard;

import com.stephanofer.prismapractice.api.common.PlayerId;

import java.util.Objects;

public record LeaderboardEntry(PlayerId playerId, String playerName, int score, String rankKey, int position) {

    public LeaderboardEntry {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(playerName, "playerName");
        Objects.requireNonNull(rankKey, "rankKey");
        if (playerName.isBlank() || rankKey.isBlank()) {
            throw new IllegalArgumentException("playerName/rankKey must not be blank");
        }
        if (position < 1) {
            throw new IllegalArgumentException("position must be >= 1");
        }
    }
}

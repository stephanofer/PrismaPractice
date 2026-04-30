package com.stephanofer.prismapractice.api.leaderboard;

import com.stephanofer.prismapractice.api.common.ModeId;

import java.util.Objects;

public record LeaderboardScope(LeaderboardType type, ModeId modeId, String seasonId) {

    public LeaderboardScope {
        Objects.requireNonNull(type, "type");
        if ((type == LeaderboardType.MODE || type == LeaderboardType.SEASON_MODE) && modeId == null) {
            throw new IllegalArgumentException("modeId is required for mode leaderboards");
        }
        if ((type == LeaderboardType.SEASON_GLOBAL || type == LeaderboardType.SEASON_MODE) && (seasonId == null || seasonId.isBlank())) {
            throw new IllegalArgumentException("seasonId is required for seasonal leaderboards");
        }
    }
}

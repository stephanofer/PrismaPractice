package com.stephanofer.prismapractice.api.leaderboard;

import com.stephanofer.prismapractice.api.common.PlayerId;

import java.util.List;

public interface LeaderboardProjectionRepository {

    void upsert(LeaderboardScope scope, LeaderboardEntry entry);

    void remove(LeaderboardScope scope, PlayerId playerId);

    List<LeaderboardEntry> top(LeaderboardQuery query);

    void clear(LeaderboardScope scope);
}

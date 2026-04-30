package com.stephanofer.prismapractice.api.history;

import com.stephanofer.prismapractice.api.match.MatchSession;
import com.stephanofer.prismapractice.api.rating.RatingApplyResult;

import java.util.List;
import java.util.Objects;

public record MatchHistoryRecordRequest(
        MatchSession matchSession,
        RatingApplyResult ratingApplyResult,
        List<MatchHistoryPlayerSnapshot> players,
        List<MatchHistoryStatEntry> stats,
        List<MatchHistoryEvent> events
) {

    public MatchHistoryRecordRequest {
        Objects.requireNonNull(matchSession, "matchSession");
        Objects.requireNonNull(players, "players");
        Objects.requireNonNull(stats, "stats");
        Objects.requireNonNull(events, "events");
        players = List.copyOf(players);
        stats = List.copyOf(stats);
        events = List.copyOf(events);
    }
}

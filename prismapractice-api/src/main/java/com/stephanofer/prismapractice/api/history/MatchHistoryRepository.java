package com.stephanofer.prismapractice.api.history;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.match.MatchId;

import java.util.List;
import java.util.Optional;

public interface MatchHistoryRepository {

    boolean exists(MatchId matchId);

    MatchHistorySummary save(MatchHistorySummary summary);

    Optional<MatchHistorySummary> findByMatchId(MatchId matchId);

    List<MatchHistorySummary> findRecentByPlayerId(PlayerId playerId, int limit, int offset);
}

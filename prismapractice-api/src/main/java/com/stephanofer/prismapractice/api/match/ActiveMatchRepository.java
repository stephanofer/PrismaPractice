package com.stephanofer.prismapractice.api.match;

import com.stephanofer.prismapractice.api.common.PlayerId;

import java.util.Optional;

public interface ActiveMatchRepository {

    Optional<ActiveMatch> findByMatchId(MatchId matchId);

    Optional<ActiveMatch> findByPlayerId(PlayerId playerId);

    ActiveMatch save(ActiveMatch activeMatch);

    void remove(MatchId matchId, PlayerId leftPlayerId, PlayerId rightPlayerId);
}

package com.stephanofer.prismapractice.api.match;

import java.util.Optional;

public interface MatchRepository {

    Optional<MatchSession> findById(MatchId matchId);

    MatchSession save(MatchSession matchSession);
}

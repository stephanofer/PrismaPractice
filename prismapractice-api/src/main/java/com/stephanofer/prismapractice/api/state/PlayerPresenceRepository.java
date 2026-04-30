package com.stephanofer.prismapractice.api.state;

import com.stephanofer.prismapractice.api.common.PlayerId;

import java.util.Optional;

public interface PlayerPresenceRepository {

    Optional<PracticePresence> find(PlayerId playerId);

    PracticePresence save(PracticePresence presence);

    void delete(PlayerId playerId);
}

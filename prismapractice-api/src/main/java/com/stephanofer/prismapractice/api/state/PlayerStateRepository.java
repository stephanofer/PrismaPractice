package com.stephanofer.prismapractice.api.state;

import com.stephanofer.prismapractice.api.common.PlayerId;

import java.util.Optional;

public interface PlayerStateRepository {

    Optional<PlayerState> find(PlayerId playerId);

    PlayerState save(PlayerState state);

    void delete(PlayerId playerId);
}

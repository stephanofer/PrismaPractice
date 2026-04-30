package com.stephanofer.prismapractice.api.arena;

import java.util.Optional;

public interface ArenaOperationalStateRepository {

    Optional<ArenaOperationalState> find(ArenaId arenaId);

    ArenaOperationalState save(ArenaOperationalState state);
}

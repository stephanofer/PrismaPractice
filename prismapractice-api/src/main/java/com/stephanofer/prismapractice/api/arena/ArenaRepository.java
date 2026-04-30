package com.stephanofer.prismapractice.api.arena;

import com.stephanofer.prismapractice.api.common.ModeId;
import com.stephanofer.prismapractice.api.common.PlayerType;
import com.stephanofer.prismapractice.api.common.RuntimeType;
import com.stephanofer.prismapractice.api.matchmaking.RegionId;

import java.util.List;
import java.util.Optional;

public interface ArenaRepository {

    Optional<ArenaDefinition> findById(ArenaId arenaId);

    List<ArenaDefinition> findCompatible(ArenaType arenaType, ModeId modeId, PlayerType playerType, RuntimeType runtimeType, RegionId regionId);

    ArenaDefinition save(ArenaDefinition arenaDefinition);
}

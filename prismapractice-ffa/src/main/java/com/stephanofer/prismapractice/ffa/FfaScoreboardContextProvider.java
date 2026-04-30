package com.stephanofer.prismapractice.ffa;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.RuntimeType;
import com.stephanofer.prismapractice.api.state.PlayerPresenceRepository;
import com.stephanofer.prismapractice.api.state.PlayerStateRepository;
import com.stephanofer.prismapractice.paper.scoreboard.PlayerScoreboardDataCache;
import com.stephanofer.prismapractice.paper.scoreboard.ScoreboardContextProvider;
import com.stephanofer.prismapractice.paper.scoreboard.ScoreboardContextSnapshot;
import com.stephanofer.prismapractice.paper.scoreboard.ScoreboardPartyRole;
import com.stephanofer.prismapractice.paper.scoreboard.ScoreboardUiFocus;
import org.bukkit.entity.Player;

import java.util.Objects;

final class FfaScoreboardContextProvider implements ScoreboardContextProvider {

    private final PlayerStateRepository playerStateRepository;
    private final PlayerPresenceRepository playerPresenceRepository;
    private final PlayerScoreboardDataCache dataCache;

    FfaScoreboardContextProvider(PlayerStateRepository playerStateRepository, PlayerPresenceRepository playerPresenceRepository, PlayerScoreboardDataCache dataCache) {
        this.playerStateRepository = Objects.requireNonNull(playerStateRepository, "playerStateRepository");
        this.playerPresenceRepository = Objects.requireNonNull(playerPresenceRepository, "playerPresenceRepository");
        this.dataCache = Objects.requireNonNull(dataCache, "dataCache");
    }

    @Override
    public ScoreboardContextSnapshot snapshot(Player player) {
        PlayerId playerId = new PlayerId(player.getUniqueId());
        return new ScoreboardContextSnapshot(
                RuntimeType.FFA,
                playerStateRepository.find(playerId).orElse(null),
                playerPresenceRepository.find(playerId).orElse(null),
                dataCache.profile(playerId).orElse(null),
                dataCache.settings(playerId),
                false,
                ScoreboardPartyRole.NONE,
                ScoreboardUiFocus.NONE,
                null
        );
    }
}

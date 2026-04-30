package com.stephanofer.prismapractice.hub.hotbar;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.queue.PlayerPartyIndexRepository;
import com.stephanofer.prismapractice.core.application.state.PlayerStateService;

import java.util.Objects;

final class HubPlayerHotbarContextService {

    private final PlayerStateService playerStateService;
    private final PlayerPartyIndexRepository playerPartyIndexRepository;

    HubPlayerHotbarContextService(PlayerStateService playerStateService, PlayerPartyIndexRepository playerPartyIndexRepository) {
        this.playerStateService = Objects.requireNonNull(playerStateService, "playerStateService");
        this.playerPartyIndexRepository = Objects.requireNonNull(playerPartyIndexRepository, "playerPartyIndexRepository");
    }

    HubPlayerHotbarContext snapshot(PlayerId playerId) {
        boolean inParty = playerPartyIndexRepository.isInParty(playerId);
        return new HubPlayerHotbarContext(
                playerStateService.findCurrentState(playerId).orElse(null),
                inParty,
                inParty ? HubPartyRole.MEMBER : HubPartyRole.NONE
        );
    }
}

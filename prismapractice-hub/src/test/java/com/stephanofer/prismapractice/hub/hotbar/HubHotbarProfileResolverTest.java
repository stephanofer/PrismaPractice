package com.stephanofer.prismapractice.hub.hotbar;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.state.PlayerState;
import com.stephanofer.prismapractice.api.state.PlayerStatus;
import com.stephanofer.prismapractice.api.state.PlayerSubStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HubHotbarProfileResolverTest {

    private final HubHotbarProfileResolver resolver = new HubHotbarProfileResolver();

    @Test
    void shouldPrioritizeQueueProfileWhenPlayerIsInQueue() {
        HubPlayerHotbarContext context = new HubPlayerHotbarContext(state(PlayerStatus.IN_QUEUE), false, HubPartyRole.NONE);

        assertEquals(List.of(HubHotbarProfileResolver.QUEUE_WAITING, HubHotbarProfileResolver.DEFAULT_HUB), resolver.resolveCandidates(context));
    }

    @Test
    void shouldReturnPartyLeaderThenMemberThenDefaultWhenPlayerIsPartyLeaderInHub() {
        HubPlayerHotbarContext context = new HubPlayerHotbarContext(state(PlayerStatus.HUB), true, HubPartyRole.LEADER);

        assertEquals(
                List.of(HubHotbarProfileResolver.PARTY_LEADER, HubHotbarProfileResolver.PARTY_MEMBER, HubHotbarProfileResolver.DEFAULT_HUB),
                resolver.resolveCandidates(context)
        );
    }

    private PlayerState state(PlayerStatus status) {
        return new PlayerState(new PlayerId(UUID.randomUUID()), status, PlayerSubStatus.NONE, Instant.now());
    }
}

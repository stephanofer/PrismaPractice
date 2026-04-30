package com.stephanofer.prismapractice.core.application.state;

import com.stephanofer.prismapractice.api.state.PlayerStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class PlayerStateTransitionPolicy {

    private final Map<PlayerStatus, Set<PlayerStatus>> transitions;

    public PlayerStateTransitionPolicy() {
        this.transitions = new EnumMap<>(PlayerStatus.class);
        allow(PlayerStatus.OFFLINE, PlayerStatus.OFFLINE, PlayerStatus.HUB);
        allow(PlayerStatus.HUB, PlayerStatus.HUB, PlayerStatus.IN_QUEUE, PlayerStatus.TRANSFERRING, PlayerStatus.IN_PUBLIC_FFA,
                PlayerStatus.IN_PARTY_FFA, PlayerStatus.IN_EVENT, PlayerStatus.EDITING_LAYOUT, PlayerStatus.OFFLINE);
        allow(PlayerStatus.IN_QUEUE, PlayerStatus.IN_QUEUE, PlayerStatus.HUB, PlayerStatus.TRANSFERRING, PlayerStatus.OFFLINE);
        allow(PlayerStatus.TRANSFERRING, PlayerStatus.TRANSFERRING, PlayerStatus.IN_MATCH, PlayerStatus.HUB, PlayerStatus.OFFLINE);
        allow(PlayerStatus.IN_MATCH, PlayerStatus.IN_MATCH, PlayerStatus.HUB, PlayerStatus.SPECTATING, PlayerStatus.OFFLINE);
        allow(PlayerStatus.SPECTATING, PlayerStatus.SPECTATING, PlayerStatus.HUB, PlayerStatus.OFFLINE);
        allow(PlayerStatus.IN_PUBLIC_FFA, PlayerStatus.IN_PUBLIC_FFA, PlayerStatus.HUB, PlayerStatus.OFFLINE);
        allow(PlayerStatus.IN_PARTY_FFA, PlayerStatus.IN_PARTY_FFA, PlayerStatus.HUB, PlayerStatus.OFFLINE);
        allow(PlayerStatus.IN_EVENT, PlayerStatus.IN_EVENT, PlayerStatus.HUB, PlayerStatus.SPECTATING, PlayerStatus.OFFLINE);
        allow(PlayerStatus.EDITING_LAYOUT, PlayerStatus.EDITING_LAYOUT, PlayerStatus.HUB, PlayerStatus.OFFLINE);
    }

    public boolean canTransition(PlayerStatus from, PlayerStatus to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        return transitions.getOrDefault(from, Set.of()).contains(to);
    }

    private void allow(PlayerStatus from, PlayerStatus... targets) {
        transitions.put(from, EnumSet.copyOf(java.util.List.of(targets)));
    }
}

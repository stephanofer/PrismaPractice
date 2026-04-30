package com.stephanofer.prismapractice.hub.hotbar;

import com.stephanofer.prismapractice.api.state.PlayerStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class HubHotbarProfileResolver {

    static final String DEFAULT_HUB = "default_hub";
    static final String QUEUE_WAITING = "queue_waiting";
    static final String PARTY_MEMBER = "party_member";
    static final String PARTY_LEADER = "party_leader";
    static final String LAYOUT_EDITING = "layout_editing";

    List<String> resolveCandidates(HubPlayerHotbarContext context) {
        Objects.requireNonNull(context, "context");
        if (context.state() == null) {
            return List.of();
        }

        List<String> candidates = new ArrayList<>();
        PlayerStatus status = context.state().status();
        switch (status) {
            case IN_QUEUE -> candidates.add(QUEUE_WAITING);
            case EDITING_LAYOUT -> candidates.add(LAYOUT_EDITING);
            case HUB -> {
                if (context.inParty()) {
                    if (context.partyRole() == HubPartyRole.LEADER) {
                        candidates.add(PARTY_LEADER);
                    }
                    candidates.add(PARTY_MEMBER);
                }
                candidates.add(DEFAULT_HUB);
            }
            default -> {
                return List.of();
            }
        }
        if (!candidates.contains(DEFAULT_HUB)) {
            candidates.add(DEFAULT_HUB);
        }
        return List.copyOf(candidates);
    }
}

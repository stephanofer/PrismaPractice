package com.stephanofer.prismapractice.hub.hotbar;

import com.stephanofer.prismapractice.api.state.PlayerState;

import java.util.Objects;

record HubPlayerHotbarContext(PlayerState state, boolean inParty, HubPartyRole partyRole) {

    HubPlayerHotbarContext {
        Objects.requireNonNull(partyRole, "partyRole");
    }
}

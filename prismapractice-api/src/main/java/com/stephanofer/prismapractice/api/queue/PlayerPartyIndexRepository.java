package com.stephanofer.prismapractice.api.queue;

import com.stephanofer.prismapractice.api.common.PlayerId;

public interface PlayerPartyIndexRepository {

    boolean isInParty(PlayerId playerId);
}

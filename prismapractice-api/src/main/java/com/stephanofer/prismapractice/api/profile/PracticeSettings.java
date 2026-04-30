package com.stephanofer.prismapractice.api.profile;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.matchmaking.PingRangePreference;

import java.util.Objects;

public record PracticeSettings(
        PlayerId playerId,
        boolean chatEnabled,
        boolean allowDuels,
        boolean friendsOnlyDuels,
        boolean allowPartyInvites,
        boolean allowSpectators,
        boolean showLobbyPlayers,
        boolean showScoreboard,
        boolean allowEventAlerts,
        PingRangePreference pingRangePreference
) {

    public PracticeSettings {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(pingRangePreference, "pingRangePreference");
    }

    public static PracticeSettings defaults(PlayerId playerId) {
        return new PracticeSettings(playerId, true, true, false, true, true, true, true, true, PingRangePreference.WITHIN_100);
    }
}

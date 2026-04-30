package com.stephanofer.prismapractice.paper.scoreboard;

import com.stephanofer.prismapractice.api.common.RuntimeType;
import com.stephanofer.prismapractice.api.profile.PracticeProfile;
import com.stephanofer.prismapractice.api.profile.PracticeSettings;
import com.stephanofer.prismapractice.api.state.PlayerState;
import com.stephanofer.prismapractice.api.state.PlayerStatus;
import com.stephanofer.prismapractice.api.state.PlayerSubStatus;
import com.stephanofer.prismapractice.api.state.PracticePresence;

import java.util.Objects;

public record ScoreboardContextSnapshot(
        RuntimeType runtimeType,
        PlayerState state,
        PracticePresence presence,
        PracticeProfile profile,
        PracticeSettings settings,
        boolean inParty,
        ScoreboardPartyRole partyRole,
        ScoreboardUiFocus uiFocus,
        ScoreboardQueueView queueView
) {

    public ScoreboardContextSnapshot {
        Objects.requireNonNull(runtimeType, "runtimeType");
        Objects.requireNonNull(partyRole, "partyRole");
        Objects.requireNonNull(uiFocus, "uiFocus");
    }

    public PlayerStatus status() {
        return state == null ? null : state.status();
    }

    public PlayerSubStatus subStatus() {
        return state == null ? null : state.subStatus();
    }

    public boolean showScoreboard() {
        return settings == null || settings.showScoreboard();
    }
}

package com.stephanofer.prismapractice.paper.scoreboard;

import com.stephanofer.prismapractice.api.common.PlayerType;
import com.stephanofer.prismapractice.api.common.RuntimeType;
import com.stephanofer.prismapractice.api.queue.QueueType;
import com.stephanofer.prismapractice.api.state.PlayerStatus;
import com.stephanofer.prismapractice.api.state.PlayerSubStatus;

import java.util.Objects;
import java.util.Set;

public record ScoreboardSceneConditions(
        Set<RuntimeType> runtimeTypes,
        Set<PlayerStatus> statuses,
        Set<PlayerSubStatus> subStatuses,
        Set<Boolean> partyMembership,
        Set<ScoreboardPartyRole> partyRoles,
        Set<QueueType> queueTypes,
        Set<PlayerType> queuePlayerTypes,
        Set<ScoreboardUiFocus> uiFocuses
) {

    public ScoreboardSceneConditions {
        runtimeTypes = copy(runtimeTypes);
        statuses = copy(statuses);
        subStatuses = copy(subStatuses);
        partyMembership = copy(partyMembership);
        partyRoles = copy(partyRoles);
        queueTypes = copy(queueTypes);
        queuePlayerTypes = copy(queuePlayerTypes);
        uiFocuses = copy(uiFocuses);
    }

    public boolean matches(ScoreboardContextSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return matches(runtimeTypes, snapshot.runtimeType())
                && matches(statuses, snapshot.status())
                && matches(subStatuses, snapshot.subStatus())
                && matches(partyMembership, snapshot.inParty())
                && matches(partyRoles, snapshot.partyRole())
                && matches(queueTypes, snapshot.queueView() == null ? null : snapshot.queueView().queueType())
                && matches(queuePlayerTypes, snapshot.queueView() == null ? null : snapshot.queueView().playerType())
                && matches(uiFocuses, snapshot.uiFocus());
    }

    private static <T> Set<T> copy(Set<T> values) {
        return values == null ? Set.of() : Set.copyOf(values);
    }

    private static <T> boolean matches(Set<T> accepted, T current) {
        return accepted.isEmpty() || (current != null && accepted.contains(current));
    }
}

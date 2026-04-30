package com.stephanofer.prismapractice.api.queue;

import com.stephanofer.prismapractice.api.common.ModeId;
import com.stephanofer.prismapractice.api.common.PlayerType;
import com.stephanofer.prismapractice.api.common.QueueId;
import com.stephanofer.prismapractice.api.common.RuntimeType;
import com.stephanofer.prismapractice.api.state.PlayerStatus;

import java.util.Objects;
import java.util.Set;

public record QueueDefinition(
        QueueId queueId,
        ModeId modeId,
        String displayName,
        QueueType queueType,
        PlayerType playerType,
        MatchmakingProfile matchmakingProfile,
        boolean enabled,
        boolean visibleInMenu,
        boolean rated,
        boolean usesSkillRating,
        boolean usesPingRange,
        boolean usesRegionSelection,
        SearchExpansionStrategy searchExpansionStrategy,
        boolean blockedIfInParty,
        Set<PlayerStatus> allowedStatuses,
        Set<PlayerStatus> blockedStatuses,
        boolean affectsGlobalRating,
        boolean affectsVisibleRank,
        boolean affectsSeasonStats,
        boolean affectsLeaderboards,
        RuntimeType targetRuntime
) {

    public QueueDefinition {
        Objects.requireNonNull(queueId, "queueId");
        Objects.requireNonNull(modeId, "modeId");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(queueType, "queueType");
        Objects.requireNonNull(playerType, "playerType");
        Objects.requireNonNull(matchmakingProfile, "matchmakingProfile");
        Objects.requireNonNull(searchExpansionStrategy, "searchExpansionStrategy");
        Objects.requireNonNull(allowedStatuses, "allowedStatuses");
        Objects.requireNonNull(blockedStatuses, "blockedStatuses");
        Objects.requireNonNull(targetRuntime, "targetRuntime");
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        allowedStatuses = Set.copyOf(allowedStatuses);
        blockedStatuses = Set.copyOf(blockedStatuses);
    }
}

package com.stephanofer.prismapractice.core.application.matchmaking;

import com.stephanofer.prismapractice.api.matchmaking.MatchmakingSearchWindow;
import com.stephanofer.prismapractice.api.matchmaking.MatchmakingSnapshot;
import com.stephanofer.prismapractice.api.queue.QueueDefinition;
import com.stephanofer.prismapractice.api.state.PlayerState;
import com.stephanofer.prismapractice.api.state.PlayerStatus;

import java.util.Objects;

public final class MatchmakingEligibilityPolicy {

    public MatchmakingEligibilityResult validate(
            QueueDefinition queueDefinition,
            MatchmakingSnapshot left,
            MatchmakingSnapshot right,
            PlayerState leftState,
            PlayerState rightState,
            MatchmakingSearchWindow window,
            RegionSelectionResult regionSelection
    ) {
        Objects.requireNonNull(queueDefinition, "queueDefinition");
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(window, "window");

        if (left.playerId().equals(right.playerId())) {
            return MatchmakingEligibilityResult.deny(MatchmakingHardFailureReason.SAME_PLAYER);
        }
        if (!left.queueId().equals(right.queueId())) {
            return MatchmakingEligibilityResult.deny(MatchmakingHardFailureReason.DIFFERENT_QUEUE);
        }
        if (leftState == null || rightState == null) {
            return MatchmakingEligibilityResult.deny(MatchmakingHardFailureReason.PLAYER_NOT_IN_QUEUE);
        }
        if (leftState.status() != PlayerStatus.IN_QUEUE || rightState.status() != PlayerStatus.IN_QUEUE) {
            return MatchmakingEligibilityResult.deny(MatchmakingHardFailureReason.STATE_NOT_QUEUE);
        }
        if (queueDefinition.usesRegionSelection()) {
            if (left.regionPings().isEmpty() || right.regionPings().isEmpty()) {
                return MatchmakingEligibilityResult.deny(MatchmakingHardFailureReason.SNAPSHOT_MISSING_REGION_DATA);
            }
            if (regionSelection == null) {
                return MatchmakingEligibilityResult.deny(MatchmakingHardFailureReason.NO_COMMON_REGION);
            }
        }
        if (queueDefinition.usesPingRange() && regionSelection != null) {
            int difference = regionSelection.pingDifference();
            if (difference > left.pingRangePreference().maxDifferenceMillis()
                    || difference > right.pingRangePreference().maxDifferenceMillis()) {
                return MatchmakingEligibilityResult.deny(MatchmakingHardFailureReason.PING_RANGE_INCOMPATIBLE);
            }
        }
        if (queueDefinition.usesSkillRating()) {
            int skillDelta = Math.abs(left.skillValue() - right.skillValue());
            if (skillDelta > window.maxSkillDelta()) {
                return MatchmakingEligibilityResult.deny(MatchmakingHardFailureReason.SKILL_DELTA_TOO_HIGH);
            }
        }
        return MatchmakingEligibilityResult.allow();
    }
}

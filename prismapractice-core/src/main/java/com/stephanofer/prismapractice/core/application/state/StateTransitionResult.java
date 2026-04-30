package com.stephanofer.prismapractice.core.application.state;

import com.stephanofer.prismapractice.api.state.PlayerState;

public record StateTransitionResult(boolean success, PlayerState state, StateTransitionFailureReason failureReason) {

    public static StateTransitionResult success(PlayerState state) {
        return new StateTransitionResult(true, state, null);
    }

    public static StateTransitionResult failure(PlayerState state, StateTransitionFailureReason failureReason) {
        return new StateTransitionResult(false, state, failureReason);
    }
}

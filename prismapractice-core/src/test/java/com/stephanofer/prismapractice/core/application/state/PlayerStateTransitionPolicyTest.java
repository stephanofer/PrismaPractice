package com.stephanofer.prismapractice.core.application.state;

import com.stephanofer.prismapractice.api.state.PlayerStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerStateTransitionPolicyTest {

    private final PlayerStateTransitionPolicy policy = new PlayerStateTransitionPolicy();

    @Test
    void shouldAllowMainHappyPathTransitions() {
        assertTrue(policy.canTransition(PlayerStatus.HUB, PlayerStatus.IN_QUEUE));
        assertTrue(policy.canTransition(PlayerStatus.IN_QUEUE, PlayerStatus.TRANSFERRING));
        assertTrue(policy.canTransition(PlayerStatus.TRANSFERRING, PlayerStatus.IN_MATCH));
        assertTrue(policy.canTransition(PlayerStatus.IN_MATCH, PlayerStatus.HUB));
    }

    @Test
    void shouldRejectInvalidTransitions() {
        assertFalse(policy.canTransition(PlayerStatus.IN_MATCH, PlayerStatus.IN_QUEUE));
        assertFalse(policy.canTransition(PlayerStatus.TRANSFERRING, PlayerStatus.EDITING_LAYOUT));
        assertFalse(policy.canTransition(PlayerStatus.IN_PUBLIC_FFA, PlayerStatus.IN_MATCH));
    }
}

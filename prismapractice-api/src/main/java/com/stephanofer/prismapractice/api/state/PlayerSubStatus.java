package com.stephanofer.prismapractice.api.state;

public enum PlayerSubStatus {
    NONE,
    WAITING_MATCH,
    WAITING_PLAYERS,
    PRE_FIGHT,
    IN_ROUND,
    ROUND_ENDING,
    REGISTERED,
    ACTIVE,
    ELIMINATED,
    SPECTATING_EVENT
}

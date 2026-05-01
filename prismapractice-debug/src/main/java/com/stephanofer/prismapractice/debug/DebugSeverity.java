package com.stephanofer.prismapractice.debug;

public enum DebugSeverity {
    DEBUG,
    INFO,
    WARN,
    ERROR;

    public boolean isAtLeast(DebugSeverity other) {
        return this.ordinal() >= other.ordinal();
    }
}

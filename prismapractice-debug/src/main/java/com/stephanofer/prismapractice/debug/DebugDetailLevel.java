package com.stephanofer.prismapractice.debug;

import java.util.Locale;

public enum DebugDetailLevel {
    OFF,
    BASIC,
    VERBOSE,
    TRACE;

    public boolean permits(DebugDetailLevel requested) {
        return this.ordinal() >= requested.ordinal();
    }

    public static DebugDetailLevel parse(String value) {
        return DebugDetailLevel.valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
    }

    public static DebugDetailLevel max(DebugDetailLevel left, DebugDetailLevel right) {
        return left.ordinal() >= right.ordinal() ? left : right;
    }
}

package com.stephanofer.prismapractice.command;

import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

public record CommandGuardResult(boolean allowed, @Nullable Component message) {

    private static final CommandGuardResult ALLOW = new CommandGuardResult(true, null);

    public static CommandGuardResult allow() {
        return ALLOW;
    }

    public static CommandGuardResult deny(final Component message) {
        return new CommandGuardResult(false, message);
    }
}

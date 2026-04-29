package com.stephanofer.prismapractice.command;

import com.mojang.brigadier.arguments.ArgumentType;
import java.util.Objects;

public final class CommandArgumentSpec<T> extends CommandNodeSpec<CommandArgumentSpec<T>> {

    private final String name;
    private final ArgumentType<T> argumentType;

    private CommandArgumentSpec(final String name, final ArgumentType<T> argumentType) {
        this.name = Objects.requireNonNull(name, "name");
        this.argumentType = Objects.requireNonNull(argumentType, "argumentType");
    }

    public static <T> CommandArgumentSpec<T> argument(final String name, final ArgumentType<T> argumentType) {
        return new CommandArgumentSpec<>(name, argumentType);
    }

    public String name() {
        return this.name;
    }

    public ArgumentType<T> argumentType() {
        return this.argumentType;
    }

    @Override
    protected CommandArgumentSpec<T> self() {
        return this;
    }
}

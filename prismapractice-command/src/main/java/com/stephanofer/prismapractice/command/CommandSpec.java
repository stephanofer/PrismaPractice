package com.stephanofer.prismapractice.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class CommandSpec extends CommandLiteralSpec {

    private final List<String> aliases = new ArrayList<>();
    private @Nullable String description;

    private CommandSpec(final String literal) {
        super(literal);
    }

    public static CommandSpec root(final String literal) {
        return new CommandSpec(literal);
    }

    public CommandSpec aliases(final String... aliases) {
        for (final String alias : aliases) {
            this.alias(alias);
        }
        return this;
    }

    public CommandSpec aliases(final Collection<String> aliases) {
        for (final String alias : aliases) {
            this.alias(alias);
        }
        return this;
    }

    public CommandSpec alias(final String alias) {
        this.aliases.add(Objects.requireNonNull(alias, "alias"));
        return this;
    }

    public CommandSpec description(final String description) {
        this.description = Objects.requireNonNull(description, "description");
        return this;
    }

    public List<String> aliases() {
        return List.copyOf(this.aliases);
    }

    public @Nullable String description() {
        return this.description;
    }
}

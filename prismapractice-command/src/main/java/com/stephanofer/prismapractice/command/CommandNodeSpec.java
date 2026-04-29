package com.stephanofer.prismapractice.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public abstract class CommandNodeSpec<S extends CommandNodeSpec<S>> {

    private final List<CommandNodeSpec<?>> children = new ArrayList<>();
    private final List<CommandRequirement> requirements = new ArrayList<>();
    private final List<CommandGuard> guards = new ArrayList<>();
    private CommandSenderScope senderScope = CommandSenderScope.ANY;
    private @Nullable String permission;
    private boolean restricted;
    private @Nullable String usage;
    private @Nullable CommandExecutionHandler handler;
    private @Nullable CommandSuggestionProvider suggestionProvider;

    protected abstract S self();

    public final S child(final CommandNodeSpec<?> child) {
        this.children.add(Objects.requireNonNull(child, "child"));
        return this.self();
    }

    public final S children(final CommandNodeSpec<?>... children) {
        for (final CommandNodeSpec<?> child : children) {
            this.child(child);
        }
        return this.self();
    }

    public final S requires(final CommandRequirement requirement) {
        this.requirements.add(Objects.requireNonNull(requirement, "requirement"));
        return this.self();
    }

    public final S guard(final CommandGuard guard) {
        this.guards.add(Objects.requireNonNull(guard, "guard"));
        return this.self();
    }

    public final S senderScope(final CommandSenderScope senderScope) {
        this.senderScope = Objects.requireNonNull(senderScope, "senderScope");
        return this.self();
    }

    public final S permission(final String permission) {
        this.permission = Objects.requireNonNull(permission, "permission");
        return this.self();
    }

    public final S restricted(final boolean restricted) {
        this.restricted = restricted;
        return this.self();
    }

    public final S usage(final String usage) {
        this.usage = Objects.requireNonNull(usage, "usage");
        return this.self();
    }

    public final S executes(final CommandExecutionHandler handler) {
        this.handler = Objects.requireNonNull(handler, "handler");
        return this.self();
    }

    public final S suggests(final CommandSuggestionProvider suggestionProvider) {
        this.suggestionProvider = Objects.requireNonNull(suggestionProvider, "suggestionProvider");
        return this.self();
    }

    public final List<CommandNodeSpec<?>> children() {
        return List.copyOf(this.children);
    }

    public final List<CommandRequirement> requirements() {
        return List.copyOf(this.requirements);
    }

    public final List<CommandGuard> guards() {
        return List.copyOf(this.guards);
    }

    public final CommandSenderScope senderScope() {
        return this.senderScope;
    }

    public final @Nullable String permission() {
        return this.permission;
    }

    public final boolean restricted() {
        return this.restricted;
    }

    public final @Nullable String usage() {
        return this.usage;
    }

    public final @Nullable CommandExecutionHandler handler() {
        return this.handler;
    }

    public final @Nullable CommandSuggestionProvider suggestionProvider() {
        return this.suggestionProvider;
    }
}

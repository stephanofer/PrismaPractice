package com.stephanofer.prismapractice.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Objects;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

public final class PaperCommandCompiler {

    private final PaperCommandServiceContainer services;

    public PaperCommandCompiler(final PaperCommandServiceContainer services) {
        this.services = Objects.requireNonNull(services, "services");
    }

    public LiteralCommandNode<CommandSourceStack> compile(final CommandSpec spec) {
        final LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(spec.literal());
        this.configureNode(root, spec);
        return root.build();
    }

    private ArgumentBuilder<CommandSourceStack, ?> compileNode(final CommandNodeSpec<?> node) {
        if (node instanceof CommandLiteralSpec literalSpec) {
            final LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(literalSpec.literal());
            this.configureNode(builder, node);
            return builder;
        }

        if (node instanceof CommandArgumentSpec<?> argumentSpec) {
            return this.configureArgumentNode(argumentSpec);
        }

        throw new IllegalStateException("Unsupported command node: " + node.getClass().getName());
    }

    private <T> RequiredArgumentBuilder<CommandSourceStack, T> configureArgumentNode(final CommandArgumentSpec<T> spec) {
        final RequiredArgumentBuilder<CommandSourceStack, T> builder = Commands.argument(spec.name(), spec.argumentType());
        if (spec.suggestionProvider() != null) {
            builder.suggests((context, suggestionsBuilder) -> spec.suggestionProvider().suggest(this.context(context), suggestionsBuilder));
        }
        this.configureNode(builder, spec);
        return builder;
    }

    private void configureNode(final ArgumentBuilder<CommandSourceStack, ?> builder, final CommandNodeSpec<?> node) {
        final Predicate<CommandSourceStack> predicate = this.buildPredicate(node);
        builder.requires(node.restricted() ? Commands.restricted(predicate) : predicate);

        if (node.handler() != null) {
            builder.executes(context -> this.execute(context, node));
        } else if (!node.children().isEmpty()) {
            builder.executes(context -> this.sendUsage(this.context(context), node.usage()));
        }

        for (final CommandNodeSpec<?> child : node.children()) {
            builder.then(this.compileNode(child));
        }
    }

    private Predicate<CommandSourceStack> buildPredicate(final CommandNodeSpec<?> node) {
        return source -> {
            if (!node.senderScope().matches(source)) {
                return false;
            }

            final @Nullable String permission = node.permission();
            if (permission != null && !source.getSender().hasPermission(permission)) {
                return false;
            }

            for (final CommandRequirement requirement : node.requirements()) {
                if (!requirement.test(source)) {
                    return false;
                }
            }

            return true;
        };
    }

    private int execute(final CommandContext<CommandSourceStack> brigadierContext, final CommandNodeSpec<?> node) throws CommandSyntaxException {
        final PaperCommandContext context = this.context(brigadierContext);

        if (!node.senderScope().matches(context.sender())) {
            context.reply(node.senderScope().denialMessage());
            return Command.SINGLE_SUCCESS;
        }

        for (final CommandGuard guard : node.guards()) {
            final CommandGuardResult result = guard.check(context);
            if (!result.allowed()) {
                if (result.message() != null) {
                    context.reply(result.message());
                }
                return Command.SINGLE_SUCCESS;
            }
        }

        if (node.handler() == null) {
            return this.sendUsage(context, node.usage());
        }

        return node.handler().execute(context);
    }

    private int sendUsage(final PaperCommandContext context, final @Nullable String usage) {
        if (usage == null || usage.isBlank()) {
            context.replyPlain("Incomplete command.");
            return Command.SINGLE_SUCCESS;
        }

        context.replyRich("<yellow>Usage:</yellow> <gray>" + usage + "</gray>");
        return Command.SINGLE_SUCCESS;
    }

    private PaperCommandContext context(final CommandContext<CommandSourceStack> brigadierContext) {
        return new PaperCommandContext(brigadierContext, this.services);
    }
}

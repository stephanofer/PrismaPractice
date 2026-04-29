package com.stephanofer.prismapractice.command;

import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public final class PaperCommandContext {

    private final CommandContext<CommandSourceStack> brigadierContext;
    private final PaperCommandServiceContainer services;

    public PaperCommandContext(
        final CommandContext<CommandSourceStack> brigadierContext,
        final PaperCommandServiceContainer services
    ) {
        this.brigadierContext = brigadierContext;
        this.services = services;
    }

    public CommandContext<CommandSourceStack> brigadierContext() {
        return this.brigadierContext;
    }

    public CommandSourceStack source() {
        return this.brigadierContext.getSource();
    }

    public CommandSender sender() {
        return this.source().getSender();
    }

    public @Nullable Entity executor() {
        return this.source().getExecutor();
    }

    public @Nullable Player playerSender() {
        return this.sender() instanceof Player player ? player : null;
    }

    public @Nullable Player playerExecutor() {
        return this.executor() instanceof Player player ? player : null;
    }

    public <T> T argument(final String name, final Class<T> type) {
        return this.brigadierContext.getArgument(name, type);
    }

    public <T> T service(final Class<T> type) {
        return this.services.require(type);
    }

    public <T> @Nullable T findService(final Class<T> type) {
        return this.services.find(type).orElse(null);
    }

    public void reply(final Component component) {
        this.sender().sendMessage(component);
    }

    public void replyRich(final String miniMessage) {
        this.sender().sendRichMessage(miniMessage);
    }

    public void replyPlain(final String plainMessage) {
        this.sender().sendPlainMessage(plainMessage);
    }
}

package com.stephanofer.prismapractice.command;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public enum CommandSenderScope {
    ANY {
        @Override
        public boolean matches(final CommandSourceStack source) {
            return true;
        }

        @Override
        public Component denialMessage() {
            return Component.text("You cannot use this command.");
        }
    },
    PLAYER_ONLY {
        @Override
        public boolean matches(final CommandSourceStack source) {
            return source.getSender() instanceof Player;
        }

        @Override
        public Component denialMessage() {
            return Component.text("Only players can use this command.");
        }
    },
    CONSOLE_ONLY {
        @Override
        public boolean matches(final CommandSourceStack source) {
            return source.getSender() instanceof ConsoleCommandSender;
        }

        @Override
        public Component denialMessage() {
            return Component.text("Only console can use this command.");
        }
    };

    public abstract boolean matches(CommandSourceStack source);

    public abstract Component denialMessage();

    public boolean matches(final CommandSender sender) {
        return switch (this) {
            case ANY -> true;
            case PLAYER_ONLY -> sender instanceof Player;
            case CONSOLE_ONLY -> sender instanceof ConsoleCommandSender;
        };
    }
}

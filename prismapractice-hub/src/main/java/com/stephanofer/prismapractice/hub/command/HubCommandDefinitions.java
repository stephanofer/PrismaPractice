package com.stephanofer.prismapractice.hub.command;

import com.mojang.brigadier.Command;
import com.stephanofer.prismapractice.command.CommandLiteralSpec;
import com.stephanofer.prismapractice.command.CommandSenderScope;
import com.stephanofer.prismapractice.command.CommandSpec;
import java.util.List;

public final class HubCommandDefinitions {

    private HubCommandDefinitions() {
    }

    public static List<CommandSpec> create() {
        final CommandSpec practice = CommandSpec.root("practice");
        practice.aliases(List.of("pp"));
        practice.description("PrismaPractice root command");
        practice.usage("/practice help");
        practice.executes(context -> {
            context.replyRich("<gold>PrismaPractice Hub</gold> <gray>- usá /practice help</gray>");
            return Command.SINGLE_SUCCESS;
        });

        practice.child(CommandLiteralSpec.literal("help")
            .usage("/practice help")
            .executes(context -> {
                context.replyRich("<yellow>Comandos:</yellow> <gray>/practice help, /practice info</gray>");
                return Command.SINGLE_SUCCESS;
            }));

        practice.child(CommandLiteralSpec.literal("info")
            .senderScope(CommandSenderScope.ANY)
            .usage("/practice info")
            .executes(context -> {
                context.replyRich("<aqua>Runtime:</aqua> <gray>hub</gray>");
                return Command.SINGLE_SUCCESS;
            }));

        return List.of(practice);
    }
}

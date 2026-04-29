package com.stephanofer.prismapractice.match.command;

import com.mojang.brigadier.Command;
import com.stephanofer.prismapractice.command.CommandLiteralSpec;
import com.stephanofer.prismapractice.command.CommandSpec;
import java.util.List;

public final class MatchCommandDefinitions {

    private MatchCommandDefinitions() {
    }

    public static List<CommandSpec> create() {
        final CommandSpec practice = CommandSpec.root("practice");
        practice.aliases(List.of("pp"));
        practice.description("PrismaPractice root command");
        practice.usage("/practice help");
        practice.executes(context -> {
            context.replyRich("<gold>PrismaPractice Match</gold> <gray>- usá /practice help</gray>");
            return Command.SINGLE_SUCCESS;
        });

        practice.child(CommandLiteralSpec.literal("help")
            .usage("/practice help")
            .executes(context -> {
                context.replyRich("<yellow>Comandos:</yellow> <gray>/practice help, /practice info</gray>");
                return Command.SINGLE_SUCCESS;
            }));

        practice.child(CommandLiteralSpec.literal("info")
            .usage("/practice info")
            .executes(context -> {
                context.replyRich("<aqua>Runtime:</aqua> <gray>match</gray>");
                return Command.SINGLE_SUCCESS;
            }));

        return List.of(practice);
    }
}

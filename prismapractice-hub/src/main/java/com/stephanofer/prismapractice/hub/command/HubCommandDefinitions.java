package com.stephanofer.prismapractice.hub.command;

import com.mojang.brigadier.Command;
import com.stephanofer.prismapractice.command.CommandLiteralSpec;
import com.stephanofer.prismapractice.command.CommandSenderScope;
import com.stephanofer.prismapractice.command.CommandSpec;
import com.stephanofer.prismapractice.core.application.state.PlayerStateService;
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
                StringBuilder message = new StringBuilder("<aqua>Runtime:</aqua> <gray>hub</gray>");
                if (context.playerSender() != null) {
                    PlayerStateService playerStateService = context.findService(PlayerStateService.class);
                    if (playerStateService != null) {
                        playerStateService.findCurrentState(new com.stephanofer.prismapractice.api.common.PlayerId(context.playerSender().getUniqueId()))
                            .ifPresent(state -> message.append(" <dark_gray>|</dark_gray> <aqua>State:</aqua> <gray>")
                                .append(state.status().name())
                                .append("/" )
                                .append(state.subStatus().name())
                                .append("</gray>"));
                    }
                }
                context.replyRich(message.toString());
                return Command.SINGLE_SUCCESS;
            }));

        return List.of(practice);
    }
}

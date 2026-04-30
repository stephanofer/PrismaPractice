package com.stephanofer.prismapractice.hub.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.stephanofer.prismapractice.command.CommandArgumentSpec;
import com.stephanofer.prismapractice.command.CommandLiteralSpec;
import com.stephanofer.prismapractice.command.CommandSenderScope;
import com.stephanofer.prismapractice.command.CommandSpec;
import com.stephanofer.prismapractice.core.application.state.PlayerStateService;
import com.stephanofer.prismapractice.paper.feedback.PaperFeedbackService;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        practice.child(feedbackCommand());

        return List.of(practice);
    }

    private static CommandLiteralSpec feedbackCommand() {
        CommandLiteralSpec feedback = CommandLiteralSpec.literal("feedback")
                .senderScope(CommandSenderScope.PLAYER_ONLY)
                .usage("/practice feedback <chat|actionbar|title|sound|bossbar|persistent-actionbar|persistent-bossbar|all|clear>")
                .executes(context -> {
                    context.replyRich("<yellow>Usá:</yellow> <gray>/practice feedback <chat|actionbar|title|sound|bossbar|persistent-actionbar|persistent-bossbar|all|clear></gray>");
                    return Command.SINGLE_SUCCESS;
                });

        feedback.children(
                templateCommand("chat", "test-chat"),
                templateCommand("actionbar", "test-actionbar"),
                templateCommand("title", "test-title"),
                templateCommand("sound", "test-sound"),
                templateCommand("bossbar", "test-bossbar"),
                templateCommand("persistent-actionbar", "test-actionbar-persistent"),
                templateCommand("persistent-bossbar", "test-bossbar-persistent"),
                templateCommand("all", "test-all"),
                clearCommand()
        );
        return feedback;
    }

    private static CommandLiteralSpec templateCommand(String literal, String templateKey) {
        return CommandLiteralSpec.literal(literal)
                .senderScope(CommandSenderScope.PLAYER_ONLY)
                .usage("/practice feedback " + literal + " [value]")
                .executes(context -> sendTemplate(context.playerSender(), context.service(PaperFeedbackService.class), templateKey, defaultValue(templateKey)))
                .child(CommandArgumentSpec.argument("value", StringArgumentType.greedyString())
                        .executes(context -> sendTemplate(
                                context.playerSender(),
                                context.service(PaperFeedbackService.class),
                                templateKey,
                                context.argument("value", String.class)
                        )));
    }

    private static CommandLiteralSpec clearCommand() {
        return CommandLiteralSpec.literal("clear")
                .senderScope(CommandSenderScope.PLAYER_ONLY)
                .usage("/practice feedback clear <actionbar|bossbar|all>")
                .children(
                        CommandLiteralSpec.literal("actionbar")
                                .executes(context -> clearSlot(context.playerSender(), context.service(PaperFeedbackService.class), "test-actionbar", "<green>Persistent actionbar limpiado.</green>")),
                        CommandLiteralSpec.literal("bossbar")
                                .executes(context -> clearSlot(context.playerSender(), context.service(PaperFeedbackService.class), "test-bossbar", "<green>Persistent bossbar limpiado.</green>")),
                        CommandLiteralSpec.literal("all")
                                .executes(context -> {
                                    Player player = context.playerSender();
                                    context.service(PaperFeedbackService.class).clear(player);
                                    context.replyRich("<green>Todo el feedback activo fue limpiado.</green>");
                                    return Command.SINGLE_SUCCESS;
                                })
                );
    }

    private static int sendTemplate(Player player, PaperFeedbackService feedbackService, String templateKey, String value) {
        feedbackService.send(player, templateKey, placeholders(player, value));
        return Command.SINGLE_SUCCESS;
    }

    private static int clearSlot(Player player, PaperFeedbackService feedbackService, String slot, String confirmation) {
        feedbackService.clearSlot(player, slot);
        player.sendRichMessage(confirmation);
        return Command.SINGLE_SUCCESS;
    }

    private static Map<String, String> placeholders(Player player, String value) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("player", player.getName());
        placeholders.put("value", value);
        return Map.copyOf(placeholders);
    }

    private static String defaultValue(String templateKey) {
        return switch (templateKey) {
            case "test-chat" -> "chat-demo";
            case "test-actionbar" -> "actionbar-demo";
            case "test-title" -> "title-demo";
            case "test-sound" -> "sound-demo";
            case "test-bossbar" -> "bossbar-demo";
            case "test-actionbar-persistent" -> "persistent-actionbar-demo";
            case "test-bossbar-persistent" -> "persistent-bossbar-demo";
            case "test-all" -> "combo-demo";
            default -> "demo";
        };
    }
}

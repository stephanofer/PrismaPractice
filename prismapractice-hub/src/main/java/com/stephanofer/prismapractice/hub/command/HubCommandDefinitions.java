package com.stephanofer.prismapractice.hub.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.stephanofer.prismapractice.command.CommandArgumentSpec;
import com.stephanofer.prismapractice.command.CommandLiteralSpec;
import com.stephanofer.prismapractice.command.CommandSenderScope;
import com.stephanofer.prismapractice.command.CommandSpec;
import com.stephanofer.prismapractice.command.CommandSuggestions;
import com.stephanofer.prismapractice.command.DebugCommandDefinitions;
import com.stephanofer.prismapractice.command.ReloadCoordinator;
import com.stephanofer.prismapractice.command.ReloadReport;
import com.stephanofer.prismapractice.core.application.state.PlayerStateService;
import com.stephanofer.prismapractice.hub.hotbar.HubHotbarService;
import com.stephanofer.prismapractice.hub.hotbar.HubStaffModeService;
import com.stephanofer.prismapractice.hub.ui.HubUiModule;
import com.stephanofer.prismapractice.paper.feedback.PaperFeedbackService;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogService;
import com.stephanofer.prismapractice.paper.ui.menu.ZMenuUiService;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HubCommandDefinitions {

    private HubCommandDefinitions() {
    }

    private static final String ROOT_LITERAL = "prismapractice";
    private static final List<String> ROOT_ALIASES = List.of("practice", "pp");
    private static final String RELOAD_PERMISSION = "prismapractice.admin.reload";
    private static final String STAFF_MODE_PERMISSION = "prismapractice.staffmode";

    public static List<CommandSpec> create() {
        final CommandSpec practice = CommandSpec.root(ROOT_LITERAL);
        practice.aliases(ROOT_ALIASES);
        practice.description("PrismaPractice root command");
        practice.usage("/prismapractice help");
        practice.executes(context -> {
            context.replyRich("<gold>PrismaPractice Hub</gold> <gray>- usá /prismapractice help</gray>");
            return Command.SINGLE_SUCCESS;
        });

        practice.child(CommandLiteralSpec.literal("help")
            .usage("/prismapractice help")
            .executes(context -> {
                context.replyRich("<yellow>Comandos:</yellow> <gray>/prismapractice help, /prismapractice info, /prismapractice reload [scope]</gray>");
                return Command.SINGLE_SUCCESS;
            }));

        practice.child(CommandLiteralSpec.literal("info")
            .senderScope(CommandSenderScope.ANY)
            .usage("/prismapractice info")
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

        practice.child(reloadCommand());
        practice.child(staffModeCommand());
        DebugCommandDefinitions.appendToRoot(practice, "hub");

        practice.child(feedbackCommand());
        practice.child(uiCommand());

        return List.of(practice);
    }

    private static CommandLiteralSpec uiCommand() {
        CommandLiteralSpec ui = CommandLiteralSpec.literal("ui")
                .senderScope(CommandSenderScope.PLAYER_ONLY)
                .usage("/prismapractice ui <menu|dialog|reload|reset>")
                .executes(context -> {
                    context.replyRich("<yellow>Usá:</yellow> <gray>/prismapractice ui menu <demo-main|demo-dynamic>, /prismapractice ui dialog <id>, /prismapractice ui reload, /prismapractice ui reset</gray>");
                    return Command.SINGLE_SUCCESS;
                });

        ui.children(
                CommandLiteralSpec.literal("menu")
                        .usage("/prismapractice ui menu <demo-main|demo-dynamic>")
                        .child(CommandArgumentSpec.argument("menu", StringArgumentType.word())
                                .executes(context -> {
                                    String menu = context.argument("menu", String.class);
                                    boolean opened = context.service(ZMenuUiService.class).openMenu(context.playerSender(), "PrismaPracticeHub", menu, 1, true);
                                    if (!opened) {
                                        context.replyRich("<red>No pude abrir el menú:</red> <gray>" + menu + "</gray>");
                                        return 0;
                                    }
                                    context.replyRich("<green>Menú abierto:</green> <gray>" + menu + "</gray>");
                                    return Command.SINGLE_SUCCESS;
                                })),
                CommandLiteralSpec.literal("dialog")
                        .usage("/prismapractice ui dialog <id>")
                        .child(CommandArgumentSpec.argument("id", StringArgumentType.word())
                                .executes(context -> {
                                    String dialogId = context.argument("id", String.class);
                                    HubUiModule uiModule = context.service(HubUiModule.class);
                                    uiModule.dialogService().sessionStore().session(context.playerSender().getUniqueId().toString()).putAll(uiModule.demoStateStore().snapshot(context.playerSender()));
                                    boolean opened = context.service(PaperDialogService.class).open(context.playerSender(), dialogId);
                                    if (!opened) {
                                        context.replyRich("<red>No pude abrir el dialog:</red> <gray>" + dialogId + "</gray>");
                                        return 0;
                                    }
                                    context.replyRich("<green>Dialog abierto:</green> <gray>" + dialogId + "</gray>");
                                    return Command.SINGLE_SUCCESS;
                                })),
                CommandLiteralSpec.literal("reload")
                        .usage("/prismapractice ui reload")
                        .executes(context -> {
                            HubUiModule uiModule = context.service(HubUiModule.class);
                            uiModule.dialogService().reload();
                            if (uiModule.menuService().isAvailable()) {
                                uiModule.menuService().reload();
                            }
                            context.replyRich("<green>UI recargada.</green>");
                            return Command.SINGLE_SUCCESS;
                        }),
                CommandLiteralSpec.literal("reset")
                        .usage("/prismapractice ui reset")
                        .executes(context -> {
                            HubUiModule uiModule = context.service(HubUiModule.class);
                            uiModule.demoStateStore().reset(context.playerSender());
                            uiModule.dialogService().sessionStore().remove(context.playerSender().getUniqueId().toString());
                            uiModule.menuService().updateInventory(context.playerSender());
                            context.replyRich("<green>Estado demo de UI reseteado.</green>");
                            return Command.SINGLE_SUCCESS;
                        })
        );
        return ui;
    }

    private static CommandLiteralSpec feedbackCommand() {
        CommandLiteralSpec feedback = CommandLiteralSpec.literal("feedback")
                .senderScope(CommandSenderScope.PLAYER_ONLY)
                .usage("/prismapractice feedback <chat|actionbar|title|sound|bossbar|persistent-actionbar|persistent-bossbar|all|clear>")
                .executes(context -> {
                    context.replyRich("<yellow>Usá:</yellow> <gray>/prismapractice feedback <chat|actionbar|title|sound|bossbar|persistent-actionbar|persistent-bossbar|all|clear></gray>");
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
                .usage("/prismapractice feedback " + literal + " [value]")
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
                .usage("/prismapractice feedback clear <actionbar|bossbar|all>")
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

    private static CommandLiteralSpec reloadCommand() {
        return CommandLiteralSpec.literal("reload")
                .senderScope(CommandSenderScope.ANY)
                .permission(RELOAD_PERMISSION)
                .usage("/prismapractice reload [scope]")
                .executes(context -> executeReload(context, ReloadCoordinator.ALL_SCOPE))
                .child(CommandArgumentSpec.argument("scope", StringArgumentType.word())
                        .suggests((commandContext, builder) -> CommandSuggestions.completeStrings(builder, commandContext.service(ReloadCoordinator.class).scopes()))
                        .executes(context -> executeReload(context, context.argument("scope", String.class))));
    }

    private static CommandLiteralSpec staffModeCommand() {
        return CommandLiteralSpec.literal("staffmode")
                .senderScope(CommandSenderScope.PLAYER_ONLY)
                .permission(STAFF_MODE_PERMISSION)
                .usage("/prismapractice staffmode")
                .executes(context -> {
                    Player player = context.playerSender();
                    HubStaffModeService staffModeService = context.service(HubStaffModeService.class);
                    HubHotbarService hotbarService = context.service(HubHotbarService.class);
                    boolean enabled = staffModeService.toggle(player);
                    if (enabled) {
                        hotbarService.clear(player);
                        hotbarService.clearInventory(player);
                        context.replyRich("<green>Staff mode activado.</green> <gray>Las protecciones del hub quedaron deshabilitadas para vos.</gray>");
                        return Command.SINGLE_SUCCESS;
                    }
                    hotbarService.refresh(player, true);
                    context.replyRich("<yellow>Staff mode desactivado.</yellow> <gray>Se restauraron las protecciones y la hotbar del hub.</gray>");
                    return Command.SINGLE_SUCCESS;
                });
    }

    private static int executeReload(com.stephanofer.prismapractice.command.PaperCommandContext context, String scope) {
        try {
            ReloadReport report = context.service(ReloadCoordinator.class).reload(scope);
            for (ReloadReport.Entry entry : report.entries()) {
                context.replyRich("<gray>-</gray> <aqua>" + entry.scope() + "</aqua> <gray>(" + entry.durationMillis() + "ms)</gray> <gray>" + entry.message() + "</gray>");
            }
            if (report.successful()) {
                context.replyRich("<green>Reload completado.</green> <gray>scope=" + report.requestedScope() + ", total=" + report.durationMillis() + "ms</gray>");
                return Command.SINGLE_SUCCESS;
            }
            context.replyRich("<red>Reload falló.</red> <gray>scope=" + report.failureScope() + ", reason=" + report.failureMessage() + "</gray>");
            return 0;
        } catch (IllegalArgumentException exception) {
            context.replyRich("<red>Scope inválido:</red> <gray>" + exception.getMessage() + "</gray>");
            return 0;
        }
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

package com.stephanofer.prismapractice.match.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.stephanofer.prismapractice.command.CommandArgumentSpec;
import com.stephanofer.prismapractice.command.CommandLiteralSpec;
import com.stephanofer.prismapractice.command.CommandSuggestions;
import com.stephanofer.prismapractice.command.CommandSpec;
import com.stephanofer.prismapractice.command.DebugCommandDefinitions;
import com.stephanofer.prismapractice.command.ReloadCoordinator;
import com.stephanofer.prismapractice.command.ReloadReport;
import java.util.List;

public final class MatchCommandDefinitions {

    private MatchCommandDefinitions() {
    }

    private static final String ROOT_LITERAL = "prismapractice";
    private static final List<String> ROOT_ALIASES = List.of("practice", "pp");
    private static final String RELOAD_PERMISSION = "prismapractice.admin.reload";

    public static List<CommandSpec> create() {
        final CommandSpec practice = CommandSpec.root(ROOT_LITERAL);
        practice.aliases(ROOT_ALIASES);
        practice.description("PrismaPractice root command");
        practice.usage("/prismapractice help");
        practice.executes(context -> {
            context.replyRich("<gold>PrismaPractice Match</gold> <gray>- usá /prismapractice help</gray>");
            return Command.SINGLE_SUCCESS;
        });

        practice.child(CommandLiteralSpec.literal("help")
            .usage("/prismapractice help")
            .executes(context -> {
                context.replyRich("<yellow>Comandos:</yellow> <gray>/prismapractice help, /prismapractice info, /prismapractice reload [scope]</gray>");
                return Command.SINGLE_SUCCESS;
            }));

        practice.child(CommandLiteralSpec.literal("info")
            .usage("/prismapractice info")
            .executes(context -> {
                context.replyRich("<aqua>Runtime:</aqua> <gray>match</gray>");
                return Command.SINGLE_SUCCESS;
            }));

        practice.child(CommandLiteralSpec.literal("reload")
                .permission(RELOAD_PERMISSION)
                .usage("/prismapractice reload [scope]")
                .executes(context -> executeReload(context, ReloadCoordinator.ALL_SCOPE))
                .child(CommandArgumentSpec.argument("scope", StringArgumentType.word())
                        .suggests((commandContext, builder) -> CommandSuggestions.completeStrings(builder, commandContext.service(ReloadCoordinator.class).scopes()))
                        .executes(context -> executeReload(context, context.argument("scope", String.class)))));

        DebugCommandDefinitions.appendToRoot(practice, "match");

        return List.of(practice);
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
}

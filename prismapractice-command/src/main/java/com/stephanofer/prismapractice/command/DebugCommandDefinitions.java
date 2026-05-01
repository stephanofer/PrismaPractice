package com.stephanofer.prismapractice.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.state.PlayerState;
import com.stephanofer.prismapractice.api.state.PracticePresence;
import com.stephanofer.prismapractice.core.application.state.PlayerStateService;
import com.stephanofer.prismapractice.data.mysql.MySqlStorage;
import com.stephanofer.prismapractice.data.mysql.StorageHealthSnapshot;
import com.stephanofer.prismapractice.data.redis.RedisHealthSnapshot;
import com.stephanofer.prismapractice.data.redis.RedisStorage;
import com.stephanofer.prismapractice.debug.DebugCategories;
import com.stephanofer.prismapractice.debug.DebugController;
import com.stephanofer.prismapractice.debug.DebugDetailLevel;
import com.stephanofer.prismapractice.debug.DebugEvent;
import com.stephanofer.prismapractice.debug.DebugWatch;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class DebugCommandDefinitions {

    private static final String DEBUG_PERMISSION = "prismapractice.admin.debug";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private DebugCommandDefinitions() {
    }

    public static void appendToRoot(CommandSpec root, String runtimeName) {
        Objects.requireNonNull(root, "root");
        root.child(debugCommand(runtimeName));
    }

    private static CommandLiteralSpec debugCommand(String runtimeName) {
        CommandLiteralSpec debug = CommandLiteralSpec.literal("debug")
                .senderScope(CommandSenderScope.ANY)
                .permission(DEBUG_PERMISSION)
                .usage("/prismapractice debug <status|runtime|player|recent|watch|category>")
                .executes(context -> {
                    context.replyRich("<yellow>Usá:</yellow> <gray>/prismapractice debug status, runtime, player <name>, recent [limit], watch ..., category <category> <level></gray>");
                    return Command.SINGLE_SUCCESS;
                });

        debug.children(
                CommandLiteralSpec.literal("status")
                        .usage("/prismapractice debug status")
                        .executes(context -> debugStatus(context, runtimeName)),
                CommandLiteralSpec.literal("runtime")
                        .usage("/prismapractice debug runtime")
                        .executes(DebugCommandDefinitions::debugRuntime),
                CommandLiteralSpec.literal("recent")
                        .usage("/prismapractice debug recent [limit]")
                        .executes(context -> debugRecent(context, 10))
                        .child(CommandArgumentSpec.argument("limit", IntegerArgumentType.integer(1, 50))
                                .executes(context -> debugRecent(context, context.argument("limit", Integer.class)))),
                playerCommand(),
                watchCommand(),
                categoryCommand()
        );
        return debug;
    }

    private static CommandLiteralSpec playerCommand() {
        return CommandLiteralSpec.literal("player")
                .usage("/prismapractice debug player <player>")
                .child(CommandArgumentSpec.argument("player", StringArgumentType.word())
                        .suggests((commandContext, builder) -> CommandSuggestions.completeStrings(
                                builder,
                                Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted().toList()
                        ))
                        .executes(DebugCommandDefinitions::debugPlayer));
    }

    private static CommandLiteralSpec watchCommand() {
        CommandLiteralSpec watch = CommandLiteralSpec.literal("watch")
                .usage("/prismapractice debug watch <player|match|trace|category|clear>")
                .executes(context -> {
                    context.replyRich("<yellow>Usá:</yellow> <gray>/prismapractice debug watch player <name> [minutes], match <id> [minutes], trace <id> [minutes], category <category> [minutes], clear</gray>");
                    return Command.SINGLE_SUCCESS;
                });

        watch.children(
                CommandLiteralSpec.literal("clear")
                        .executes(context -> {
                            context.service(DebugController.class).clearWatches();
                            context.replyRich("<green>Watchers de debug limpiados.</green>");
                            return Command.SINGLE_SUCCESS;
                        }),
                watchSubjectCommand("player"),
                watchSubjectCommand("match"),
                watchSubjectCommand("trace"),
                watchSubjectCommand("category")
        );
        return watch;
    }

    private static CommandLiteralSpec watchSubjectCommand(String type) {
        CommandLiteralSpec literal = CommandLiteralSpec.literal(type)
                .usage("/prismapractice debug watch " + type + " <value> [minutes]")
                .child(CommandArgumentSpec.argument("value", StringArgumentType.word())
                        .suggests((commandContext, builder) -> {
                            if (!type.equals("player")) {
                                if (type.equals("category")) {
                                    return CommandSuggestions.completeStrings(builder, DebugCategories.known());
                                }
                                return builder.buildFuture();
                            }
                            return CommandSuggestions.completeStrings(builder, Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted().toList());
                        })
                        .executes(context -> activateWatch(context, type, context.argument("value", String.class), null))
                        .child(CommandArgumentSpec.argument("minutes", IntegerArgumentType.integer(1))
                                .executes(context -> activateWatch(context, type, context.argument("value", String.class), context.argument("minutes", Integer.class)))));
        return literal;
    }

    private static CommandLiteralSpec categoryCommand() {
        return CommandLiteralSpec.literal("category")
                .usage("/prismapractice debug category <category> <off|basic|verbose|trace>")
                .child(CommandArgumentSpec.argument("category", StringArgumentType.word())
                        .suggests((commandContext, builder) -> CommandSuggestions.completeStrings(builder, DebugCategories.known()))
                        .child(CommandArgumentSpec.argument("level", StringArgumentType.word())
                                .suggests((commandContext, builder) -> CommandSuggestions.completeStrings(builder, List.of("off", "basic", "verbose", "trace")))
                                .executes(DebugCommandDefinitions::setCategoryLevel)));
    }

    private static int debugStatus(PaperCommandContext context, String runtimeName) {
        DebugController debug = context.service(DebugController.class);
        List<DebugWatch> watches = debug.activeWatches();
        Map<String, DebugDetailLevel> overrides = debug.runtimeCategoryLevels();

        context.replyRich("<gold>Debug status</gold> <gray>runtime=" + runtimeName + "</gray>");
        context.replyRich("<gray>- enabled=</gray><aqua>" + debug.enabled() + "</aqua> <gray>buffered=</gray><aqua>" + debug.bufferedEventCount() + "/" + debug.config().ringBufferSize() + "</aqua> <gray>watches=</gray><aqua>" + watches.size() + "</aqua>");
        context.replyRich("<gray>- consoleSeverity=</gray><aqua>" + lower(debug.config().consoleSeverity().name()) + "</aqua> <gray>defaultLevel=</gray><aqua>" + lower(debug.config().defaultCategoryLevel().name()) + "</aqua>");
        if (!overrides.isEmpty()) {
            context.replyRich("<gray>- overrides:</gray> <aqua>" + overrides.entrySet().stream().map(entry -> entry.getKey() + "=" + lower(entry.getValue().name())).collect(Collectors.joining(", ")) + "</aqua>");
        }
        if (!watches.isEmpty()) {
            context.replyRich("<gray>- active watches:</gray>");
            for (DebugWatch watch : watches.stream().limit(5).toList()) {
                context.replyRich("  <gray>*</gray> <aqua>" + watch.type() + "</aqua> <gray>" + watch.subject() + "</gray> <dark_gray>(" + lower(watch.level().name()) + ", until " + TIME_FORMATTER.format(watch.expiresAt()) + ")</dark_gray>");
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int debugRuntime(PaperCommandContext context) {
        DebugController debug = context.service(DebugController.class);
        JavaPlugin plugin = context.service(JavaPlugin.class);
        MySqlStorage mysql = context.findService(MySqlStorage.class);
        RedisStorage redis = context.findService(RedisStorage.class);

        context.replyRich("<gold>Runtime snapshot</gold>");
        context.replyRich("<gray>- plugin=</gray><aqua>" + plugin.getName() + "</aqua> <gray>version=</gray><aqua>" + plugin.getPluginMeta().getVersion() + "</aqua> <gray>online=</gray><aqua>" + Bukkit.getOnlinePlayers().size() + "</aqua>");
        if (mysql != null) {
            StorageHealthSnapshot health = mysql.healthSnapshot();
            context.replyRich("<gray>- mysql:</gray> <aqua>available=" + health.available() + " active=" + health.activeConnections() + " idle=" + health.idleConnections() + " total=" + health.totalConnections() + " waiting=" + health.threadsAwaitingConnection() + "</aqua>");
        }
        if (redis != null) {
            RedisHealthSnapshot health = redis.healthSnapshot();
            context.replyRich("<gray>- redis:</gray> <aqua>enabled=" + health.enabled() + " closed=" + health.closed() + " commandOpen=" + health.commandConnectionOpen() + " pubSubOpen=" + health.pubSubConnectionOpen() + " handlers=" + health.registeredChannelHandlers() + "</aqua>");
        }
        context.replyRich("<gray>- debug:</gray> <aqua>buffered=" + debug.bufferedEventCount() + " watchDefaultMinutes=" + debug.config().watch().defaultMinutes() + "</aqua>");
        return Command.SINGLE_SUCCESS;
    }

    private static int debugPlayer(PaperCommandContext context) {
        String playerName = context.argument("player", String.class);
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
            context.replyRich("<red>Jugador no encontrado o no está online:</red> <gray>" + playerName + "</gray>");
            return 0;
        }

        DebugController debug = context.service(DebugController.class);
        PlayerStateService playerStateService = context.findService(PlayerStateService.class);
        PlayerId playerId = new PlayerId(player.getUniqueId());
        Optional<PlayerState> state = playerStateService == null ? Optional.empty() : playerStateService.findCurrentState(playerId);
        Optional<PracticePresence> presence = playerStateService == null ? Optional.empty() : playerStateService.findPresence(playerId);
        List<DebugEvent> recent = debug.recent(8, event -> matchesPlayer(event, player));

        context.replyRich("<gold>Player debug</gold> <gray>" + player.getName() + "</gray>");
        context.replyRich("<gray>- uuid=</gray><aqua>" + player.getUniqueId() + "</aqua> <gray>ping=</gray><aqua>" + player.getPing() + "</aqua> <gray>world=</gray><aqua>" + player.getWorld().getName() + "</aqua>");
        if (state.isPresent()) {
            context.replyRich("<gray>- state=</gray><aqua>" + state.get().status() + "</aqua> <gray>sub=</gray><aqua>" + state.get().subStatus() + "</aqua> <gray>updated=</gray><aqua>" + state.get().updatedAt() + "</aqua>");
        }
        if (presence.isPresent()) {
            PracticePresence snapshot = presence.get();
            context.replyRich("<gray>- presence=</gray><aqua>online=" + snapshot.online() + " runtime=" + snapshot.runtimeType() + " server=" + snapshot.serverId() + " updatedAt=" + snapshot.updatedAt() + "</aqua>");
        }
        if (recent.isEmpty()) {
            context.replyRich("<gray>- recent:</gray> <dark_gray>sin eventos recientes en buffer</dark_gray>");
            return Command.SINGLE_SUCCESS;
        }
        context.replyRich("<gray>- recent:</gray>");
        for (DebugEvent event : recent) {
            context.replyRich("  <gray>* [" + TIME_FORMATTER.format(event.timestamp()) + "]</gray> <aqua>" + event.category() + "</aqua> <gray>" + event.name() + "</gray> <dark_gray>-</dark_gray> <white>" + event.message() + "</white>");
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int debugRecent(PaperCommandContext context, int limit) {
        DebugController debug = context.service(DebugController.class);
        List<DebugEvent> events = debug.recent(limit);
        if (events.isEmpty()) {
            context.replyRich("<gray>No hay eventos de debug en buffer.</gray>");
            return Command.SINGLE_SUCCESS;
        }

        context.replyRich("<gold>Recent debug events</gold> <gray>limit=" + limit + "</gray>");
        for (DebugEvent event : events) {
            context.replyRich(formatEvent(event));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int activateWatch(PaperCommandContext context, String type, String value, Integer minutes) {
        DebugController debug = context.service(DebugController.class);
        int resolvedMinutes = resolveMinutes(debug, minutes);
        Duration duration = Duration.ofMinutes(resolvedMinutes);
        DebugWatch watch = switch (type) {
            case "player" -> debug.watchPlayer(value, DebugDetailLevel.TRACE, duration);
            case "match" -> debug.watchMatch(value, DebugDetailLevel.TRACE, duration);
            case "trace" -> debug.watchTrace(value, DebugDetailLevel.TRACE, duration);
            case "category" -> debug.watchCategory(value, DebugDetailLevel.TRACE, duration);
            default -> throw new IllegalArgumentException("Unknown watch type: " + type);
        };
        context.replyRich("<green>Watch activado.</green> <gray>type=" + watch.type() + ", subject=" + watch.subject() + ", level=" + lower(watch.level().name()) + ", until=" + TIME_FORMATTER.format(watch.expiresAt()) + "</gray>");
        return Command.SINGLE_SUCCESS;
    }

    private static int setCategoryLevel(PaperCommandContext context) {
        DebugController debug = context.service(DebugController.class);
        String category = context.argument("category", String.class);
        String level = context.argument("level", String.class);
        try {
            DebugDetailLevel resolved = DebugDetailLevel.parse(level);
            debug.setRuntimeCategoryLevel(category, resolved);
            context.replyRich("<green>Nivel runtime aplicado.</green> <gray>" + DebugCategories.normalize(category) + "=" + lower(resolved.name()) + "</gray>");
            return Command.SINGLE_SUCCESS;
        } catch (IllegalArgumentException exception) {
            context.replyRich("<red>Nivel inválido:</red> <gray>" + level + "</gray>");
            return 0;
        }
    }

    private static int resolveMinutes(DebugController debug, Integer requested) {
        int defaultMinutes = debug.config().watch().defaultMinutes();
        int maxMinutes = debug.config().watch().maxMinutes();
        if (requested == null) {
            return defaultMinutes;
        }
        return Math.min(requested, maxMinutes);
    }

    private static boolean matchesPlayer(DebugEvent event, Player player) {
        String eventPlayerId = event.fields().get("playerId");
        String eventPlayerName = event.fields().get("playerName");
        return player.getUniqueId().toString().equalsIgnoreCase(eventPlayerId)
                || player.getName().equalsIgnoreCase(eventPlayerName);
    }

    private static String formatEvent(DebugEvent event) {
        StringBuilder message = new StringBuilder("<gray>[" + TIME_FORMATTER.format(event.timestamp()) + "]</gray> ")
                .append("<aqua>").append(event.category()).append("</aqua> ")
                .append("<gray>").append(event.name()).append("</gray> ")
                .append("<dark_gray>(").append(lower(event.severity().name())).append('/').append(lower(event.detailLevel().name())).append(")</dark_gray> ")
                .append("<white>").append(escape(event.message())).append("</white>");
        List<String> details = new ArrayList<>();
        if (event.fields().containsKey("playerName")) {
            details.add("player=" + event.fields().get("playerName"));
        }
        if (event.fields().containsKey("matchId")) {
            details.add("match=" + event.fields().get("matchId"));
        }
        if (event.fields().containsKey("traceId")) {
            details.add("trace=" + event.fields().get("traceId"));
        }
        if (!details.isEmpty()) {
            message.append(" <dark_gray>|</dark_gray> <gray>").append(escape(String.join(", ", details))).append("</gray>");
        }
        return message.toString();
    }

    private static String lower(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private static String escape(String value) {
        return value.replace("<", "&lt;").replace(">", "&gt;");
    }
}

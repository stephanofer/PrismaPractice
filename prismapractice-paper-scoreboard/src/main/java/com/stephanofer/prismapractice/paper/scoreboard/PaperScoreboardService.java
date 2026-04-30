package com.stephanofer.prismapractice.paper.scoreboard;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import net.megavex.scoreboardlibrary.api.sidebar.component.ComponentSidebarLayout;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PaperScoreboardService {

    private final Plugin plugin;
    private final ScoreboardLibrary scoreboardLibrary;
    private final PaperScoreboardConfig config;
    private final ScoreboardContextProvider contextProvider;
    private final ScoreboardPlaceholderResolver placeholderResolver;
    private final MiniMessage miniMessage;
    private final Map<UUID, Session> sessions;
    private final BukkitTask tickerTask;
    private long tickCounter;

    public PaperScoreboardService(
            Plugin plugin,
            ScoreboardLibrary scoreboardLibrary,
            PaperScoreboardConfig config,
            ScoreboardContextProvider contextProvider,
            ScoreboardPlaceholderResolver placeholderResolver
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.scoreboardLibrary = Objects.requireNonNull(scoreboardLibrary, "scoreboardLibrary");
        this.config = Objects.requireNonNull(config, "config");
        this.contextProvider = Objects.requireNonNull(contextProvider, "contextProvider");
        this.placeholderResolver = Objects.requireNonNull(placeholderResolver, "placeholderResolver");
        this.miniMessage = MiniMessage.miniMessage();
        this.sessions = new ConcurrentHashMap<>();
        this.tickerTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, config.settings().tickInterval(), config.settings().tickInterval());
    }

    public void refresh(Player player, boolean force) {
        Objects.requireNonNull(player, "player");
        if (!player.isOnline()) {
            clear(player);
            return;
        }

        try {
            ScoreboardContextSnapshot snapshot = contextProvider.snapshot(player);
            if (config.settings().hideWhenDisabledInSettings() && !snapshot.showScoreboard()) {
                clear(player);
                return;
            }

            Optional<ScoreboardSceneConfig> scene = resolveScene(snapshot);
            if (scene.isEmpty()) {
                clear(player);
                return;
            }

            Session session = sessions.computeIfAbsent(player.getUniqueId(), ignored -> Session.create(scoreboardLibrary, player));
            RenderedScene rendered = render(player, snapshot, scene.get());
            if (force || !rendered.fingerprint().equals(session.fingerprint)) {
                rendered.layout().apply(session.sidebar);
                session.fingerprint = rendered.fingerprint();
                session.sceneKey = scene.get().key();
            }
            session.nextRefreshTick = tickCounter + scene.get().refreshIntervalTicks();
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Failed to refresh scoreboard for " + player.getName() + ": " + exception.getMessage());
        }
    }

    public void refresh(Player player) {
        refresh(player, false);
    }

    public void clear(Player player) {
        Session session = sessions.remove(player.getUniqueId());
        if (session != null) {
            session.close();
        }
    }

    public void close() {
        tickerTask.cancel();
        for (Session session : sessions.values()) {
            session.close();
        }
        sessions.clear();
        scoreboardLibrary.close();
    }

    private void tick() {
        tickCounter += config.settings().tickInterval();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Session session = sessions.get(player.getUniqueId());
            if (session == null || session.nextRefreshTick <= tickCounter) {
                refresh(player, false);
            }
        }
    }

    private Optional<ScoreboardSceneConfig> resolveScene(ScoreboardContextSnapshot snapshot) {
        for (ScoreboardSceneConfig scene : config.scenes()) {
            if (scene.conditions().matches(snapshot)) {
                return Optional.of(scene);
            }
        }
        return Optional.empty();
    }

    private RenderedScene render(Player player, ScoreboardContextSnapshot snapshot, ScoreboardSceneConfig scene) {
        Map<String, String> placeholders = placeholderResolver.resolve(player, snapshot);
        Component title = deserialize(player, scene.title(), placeholders);
        List<SidebarComponent> lineComponents = new ArrayList<>(scene.lines().size());
        StringBuilder fingerprintBuilder = new StringBuilder(scene.key()).append('|').append(title);
        for (String line : scene.lines()) {
            Component renderedLine = deserialize(player, line, placeholders);
            lineComponents.add(SidebarComponent.staticLine(renderedLine));
            fingerprintBuilder.append('|').append(renderedLine);
        }
        var linesBuilder = SidebarComponent.builder();
        for (SidebarComponent lineComponent : lineComponents) {
            linesBuilder.addComponent(lineComponent);
        }
        return new RenderedScene(
                new ComponentSidebarLayout(SidebarComponent.staticLine(title), linesBuilder.build()),
                fingerprintBuilder.toString()
        );
    }

    private Component deserialize(Player player, String template, Map<String, String> placeholders) {
        String prepared = template;
        if (config.settings().allowPlaceholderApi() && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            prepared = PlaceholderAPI.setPlaceholders(player, prepared);
        }
        TagResolver[] resolvers = placeholders.entrySet().stream()
                .map(entry -> Placeholder.unparsed(entry.getKey(), entry.getValue()))
                .toArray(TagResolver[]::new);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            prepared = prepared.replace('%' + entry.getKey() + '%', entry.getValue());
        }
        return miniMessage.deserialize(prepared, resolvers);
    }

    private static final class Session {
        private final Sidebar sidebar;
        private String sceneKey = "";
        private String fingerprint = "";
        private long nextRefreshTick;

        private Session(Sidebar sidebar) {
            this.sidebar = sidebar;
        }

        private static Session create(ScoreboardLibrary scoreboardLibrary, Player player) {
            Sidebar sidebar = scoreboardLibrary.createSidebar();
            sidebar.addPlayer(player);
            return new Session(sidebar);
        }

        private void close() {
            sidebar.close();
        }
    }

    private record RenderedScene(ComponentSidebarLayout layout, String fingerprint) {
    }
}

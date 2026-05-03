package com.stephanofer.prismapractice.paper.feedback;

import com.stephanofer.prismapractice.feedback.ActionBarFeedbackDelivery;
import com.stephanofer.prismapractice.feedback.BossBarFeedbackDelivery;
import com.stephanofer.prismapractice.feedback.ChatFeedbackDelivery;
import com.stephanofer.prismapractice.feedback.FeedbackBossBarColor;
import com.stephanofer.prismapractice.feedback.FeedbackBossBarFlag;
import com.stephanofer.prismapractice.feedback.FeedbackBossBarOverlay;
import com.stephanofer.prismapractice.feedback.FeedbackConfig;
import com.stephanofer.prismapractice.feedback.FeedbackDelivery;
import com.stephanofer.prismapractice.feedback.FeedbackDeliveryMode;
import com.stephanofer.prismapractice.feedback.FeedbackPersistence;
import com.stephanofer.prismapractice.feedback.FeedbackSoundSource;
import com.stephanofer.prismapractice.feedback.FeedbackTemplate;
import com.stephanofer.prismapractice.feedback.SoundFeedbackDelivery;
import com.stephanofer.prismapractice.feedback.TitleFeedbackDelivery;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PaperFeedbackService {

    private final Plugin plugin;
    private final MiniMessage miniMessage;
    private final Map<UUID, PlayerFeedbackState> playerStates;
    private final BukkitTask tickerTask;
    private volatile FeedbackConfig config;
    private long tickCounter;

    public PaperFeedbackService(Plugin plugin, FeedbackConfig config) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.config = Objects.requireNonNull(config, "config");
        this.miniMessage = MiniMessage.miniMessage();
        this.playerStates = new ConcurrentHashMap<>();
        this.tickerTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void send(Audience audience, String templateKey, Map<String, String> placeholders) {
        Objects.requireNonNull(audience, "audience");
        Objects.requireNonNull(templateKey, "templateKey");
        Objects.requireNonNull(placeholders, "placeholders");

        if (audience instanceof Player player) {
            send(player, templateKey, placeholders);
            return;
        }

        FeedbackTemplate template = config.template(templateKey);
        ensureNoPersistentDeliveries(templateKey, template);
        for (FeedbackDelivery delivery : template.deliveries()) {
            dispatchOneShot(audience, delivery, placeholders);
        }
    }

    public void send(Player player, String templateKey, Map<String, String> placeholders) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(templateKey, "templateKey");
        Objects.requireNonNull(placeholders, "placeholders");

        FeedbackTemplate template = config.template(templateKey);
        for (FeedbackDelivery delivery : template.deliveries()) {
            dispatchPlayer(player, delivery, placeholders);
        }
    }

    public void clearSlot(Player player, String slot) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(slot, "slot");
        PlayerFeedbackState state = playerStates.get(player.getUniqueId());
        if (state == null) {
            return;
        }
        boolean clearedActionBar = state.actionBarRegistry.clear(slot);
        BossBar bossBar = state.persistentBossBars.remove(slot);
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
        if (clearedActionBar && state.actionBarRegistry.isEmpty()) {
            player.sendActionBar(Component.empty());
        }
        if (state.isEmpty()) {
            playerStates.remove(player.getUniqueId());
        }
    }

    public void clear(Player player) {
        Objects.requireNonNull(player, "player");
        PlayerFeedbackState state = playerStates.remove(player.getUniqueId());
        if (state == null) {
            return;
        }
        state.actionBarRegistry.clear();
        player.sendActionBar(Component.empty());
        for (BossBar bossBar : state.persistentBossBars.values()) {
            player.hideBossBar(bossBar);
        }
        for (BossBarSnapshot snapshot : state.transientBossBars) {
            player.hideBossBar(snapshot.bar);
        }
    }

    public void clearPersistentSlots(Player player, String templateKey) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(templateKey, "templateKey");
        FeedbackTemplate template = config.template(templateKey);
        for (FeedbackDelivery delivery : template.deliveries()) {
            if (delivery instanceof ActionBarFeedbackDelivery actionBar
                    && actionBar.mode() == FeedbackDeliveryMode.PERSISTENT
                    && actionBar.persistence() != null) {
                clearSlot(player, actionBar.persistence().slot());
            }
            if (delivery instanceof BossBarFeedbackDelivery bossBar
                    && bossBar.mode() == FeedbackDeliveryMode.PERSISTENT
                    && bossBar.persistence() != null) {
                clearSlot(player, bossBar.persistence().slot());
            }
        }
    }

    public void reload(FeedbackConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        // Reload is intentionally limited to operational templates. We clear live persistent feedback
        // so the runtime does not keep showing stale bars created from an older config snapshot.
        for (Player player : Bukkit.getOnlinePlayers()) {
            clear(player);
        }
    }

    public void close() {
        tickerTask.cancel();
        for (Player player : Bukkit.getOnlinePlayers()) {
            clear(player);
        }
        playerStates.clear();
    }

    private void dispatchPlayer(Player player, FeedbackDelivery delivery, Map<String, String> placeholders) {
        switch (delivery) {
            case ChatFeedbackDelivery chat -> dispatchOneShot(player, chat, placeholders);
            case ActionBarFeedbackDelivery actionBar -> dispatchActionBar(player, actionBar, placeholders);
            case TitleFeedbackDelivery title -> dispatchOneShot(player, title, placeholders);
            case SoundFeedbackDelivery sound -> dispatchOneShot(player, sound, placeholders);
            case BossBarFeedbackDelivery bossBar -> dispatchBossBar(player, bossBar, placeholders);
        }
    }

    private void dispatchOneShot(Audience audience, FeedbackDelivery delivery, Map<String, String> placeholders) {
        switch (delivery) {
            case ChatFeedbackDelivery chat -> {
                for (String line : chat.lines()) {
                    audience.sendMessage(render(line, placeholders));
                }
            }
            case ActionBarFeedbackDelivery actionBar -> audience.sendActionBar(render(actionBar.message(), placeholders));
            case TitleFeedbackDelivery title -> audience.showTitle(Title.title(
                    render(title.title(), placeholders),
                    render(title.subtitle(), placeholders),
                    Title.Times.times(
                            toDuration(title.times().fadeInTicks()),
                            toDuration(title.times().stayTicks()),
                            toDuration(title.times().fadeOutTicks())
                    )
            ));
            case SoundFeedbackDelivery sound -> audience.playSound(mapSound(sound));
            case BossBarFeedbackDelivery bossBar -> {
                BossBar rendered = mapBossBar(bossBar, placeholders);
                audience.showBossBar(rendered);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> audience.hideBossBar(rendered), bossBar.durationTicks());
            }
        }
    }

    private void dispatchActionBar(Player player, ActionBarFeedbackDelivery delivery, Map<String, String> placeholders) {
        Component component = render(delivery.message(), placeholders);
        if (delivery.mode() == FeedbackDeliveryMode.ONE_SHOT) {
            player.sendActionBar(component);
            return;
        }
        FeedbackPersistence persistence = Objects.requireNonNull(delivery.persistence(), "persistence");
        playerState(player.getUniqueId()).actionBarRegistry.upsert(
                persistence.slot(),
                component,
                persistence.intervalTicks(),
                persistence.priority()
        );
    }

    private void dispatchBossBar(Player player, BossBarFeedbackDelivery delivery, Map<String, String> placeholders) {
        BossBar bossBar = mapBossBar(delivery, placeholders);
        if (delivery.mode() == FeedbackDeliveryMode.ONE_SHOT) {
            player.showBossBar(bossBar);
            playerState(player.getUniqueId()).transientBossBars.add(new BossBarSnapshot(bossBar, tickCounter + delivery.durationTicks()));
            return;
        }
        FeedbackPersistence persistence = Objects.requireNonNull(delivery.persistence(), "persistence");
        PlayerFeedbackState state = playerState(player.getUniqueId());
        BossBar previous = state.persistentBossBars.put(persistence.slot(), bossBar);
        if (previous != null) {
            player.hideBossBar(previous);
        }
        player.showBossBar(bossBar);
    }

    private void ensureNoPersistentDeliveries(String templateKey, FeedbackTemplate template) {
        for (FeedbackDelivery delivery : template.deliveries()) {
            if (delivery instanceof ActionBarFeedbackDelivery actionBar && actionBar.mode() == FeedbackDeliveryMode.PERSISTENT) {
                throw new IllegalArgumentException("Template '" + templateKey + "' contains persistent ACTION_BAR delivery and requires a Player target");
            }
            if (delivery instanceof BossBarFeedbackDelivery bossBar && bossBar.mode() == FeedbackDeliveryMode.PERSISTENT) {
                throw new IllegalArgumentException("Template '" + templateKey + "' contains persistent BOSSBAR delivery and requires a Player target");
            }
        }
    }

    private Component render(String template, Map<String, String> placeholders) {
        String prepared = template;
        TagResolver[] resolvers = placeholders.entrySet().stream()
                .map(entry -> Placeholder.unparsed(entry.getKey(), entry.getValue()))
                .toArray(TagResolver[]::new);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            prepared = prepared.replace('%' + entry.getKey() + '%', entry.getValue());
        }
        return miniMessage.deserialize(prepared, resolvers);
    }

    private Sound mapSound(SoundFeedbackDelivery delivery) {
        return Sound.sound(
                Key.key(delivery.sound().toLowerCase(Locale.ROOT)),
                switch (delivery.source()) {
                    case MASTER -> Sound.Source.MASTER;
                    case MUSIC -> Sound.Source.MUSIC;
                    case RECORD -> Sound.Source.RECORD;
                    case WEATHER -> Sound.Source.WEATHER;
                    case BLOCK -> Sound.Source.BLOCK;
                    case HOSTILE -> Sound.Source.HOSTILE;
                    case NEUTRAL -> Sound.Source.NEUTRAL;
                    case PLAYER -> Sound.Source.PLAYER;
                    case AMBIENT -> Sound.Source.AMBIENT;
                    case VOICE -> Sound.Source.VOICE;
                },
                delivery.volume(),
                delivery.pitch()
        );
    }

    private BossBar mapBossBar(BossBarFeedbackDelivery delivery, Map<String, String> placeholders) {
        BossBar bossBar = BossBar.bossBar(
                render(delivery.message(), placeholders),
                delivery.progress(),
                switch (delivery.color()) {
                    case PINK -> BossBar.Color.PINK;
                    case BLUE -> BossBar.Color.BLUE;
                    case RED -> BossBar.Color.RED;
                    case GREEN -> BossBar.Color.GREEN;
                    case YELLOW -> BossBar.Color.YELLOW;
                    case PURPLE -> BossBar.Color.PURPLE;
                    case WHITE -> BossBar.Color.WHITE;
                },
                switch (delivery.overlay()) {
                    case PROGRESS -> BossBar.Overlay.PROGRESS;
                    case NOTCHED_6 -> BossBar.Overlay.NOTCHED_6;
                    case NOTCHED_10 -> BossBar.Overlay.NOTCHED_10;
                    case NOTCHED_12 -> BossBar.Overlay.NOTCHED_12;
                    case NOTCHED_20 -> BossBar.Overlay.NOTCHED_20;
                }
        );
        for (FeedbackBossBarFlag flag : delivery.flags()) {
            bossBar.addFlag(switch (flag) {
                case DARKEN_SKY -> BossBar.Flag.DARKEN_SCREEN;
                case PLAY_BOSS_MUSIC -> BossBar.Flag.PLAY_BOSS_MUSIC;
                case CREATE_WORLD_FOG -> BossBar.Flag.CREATE_WORLD_FOG;
            });
        }
        return bossBar;
    }

    private Duration toDuration(int ticks) {
        return Duration.ofMillis(ticks * 50L);
    }

    private PlayerFeedbackState playerState(UUID playerId) {
        return playerStates.computeIfAbsent(playerId, ignored -> new PlayerFeedbackState());
    }

    private void tick() {
        tickCounter++;
        if (playerStates.isEmpty()) {
            return;
        }

        for (Map.Entry<UUID, PlayerFeedbackState> entry : new ArrayList<>(playerStates.entrySet())) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                playerStates.remove(entry.getKey());
                continue;
            }

            PlayerFeedbackState state = entry.getValue();
            PersistentActionBarRegistry.TickDecision decision = state.actionBarRegistry.tick(tickCounter);
            if (decision.shouldRender()) {
                player.sendActionBar(decision.component());
            }

            if (!state.transientBossBars.isEmpty()) {
                state.transientBossBars.removeIf(snapshot -> {
                    if (snapshot.expiresAtTick > tickCounter) {
                        return false;
                    }
                    player.hideBossBar(snapshot.bar);
                    return true;
                });
            }

            if (state.isEmpty()) {
                playerStates.remove(entry.getKey());
            }
        }
    }

    private static final class PlayerFeedbackState {
        private final PersistentActionBarRegistry actionBarRegistry = new PersistentActionBarRegistry();
        private final Map<String, BossBar> persistentBossBars = new LinkedHashMap<>();
        private final List<BossBarSnapshot> transientBossBars = new ArrayList<>();

        private boolean isEmpty() {
            return actionBarRegistry.isEmpty() && persistentBossBars.isEmpty() && transientBossBars.isEmpty();
        }
    }

    private record BossBarSnapshot(BossBar bar, long expiresAtTick) {
    }
}

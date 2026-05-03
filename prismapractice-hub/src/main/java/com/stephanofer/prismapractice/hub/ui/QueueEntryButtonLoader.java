package com.stephanofer.prismapractice.hub.ui;

import com.stephanofer.prismapractice.api.common.QueueId;
import com.stephanofer.prismapractice.hub.HubQueueFeedbackCoordinator;
import com.stephanofer.prismapractice.hub.HubScoreboardModule;
import com.stephanofer.prismapractice.hub.hotbar.HubHotbarService;
import com.stephanofer.prismapractice.paper.feedback.PaperFeedbackService;
import com.stephanofer.prismapractice.paper.ui.menu.ZMenuUiService;
import fr.maxlego08.menu.api.MenuItemStack;
import fr.maxlego08.menu.api.MenuPlugin;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.button.DefaultButtonValue;
import fr.maxlego08.menu.api.loader.ButtonLoader;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Objects;
import java.util.function.Supplier;

final class QueueEntryButtonLoader extends ButtonLoader {

    private final HubQueueMenuService queueMenuService;
    private final HubScoreboardModule scoreboardModule;
    private final PaperFeedbackService feedbackService;
    private final HubQueueFeedbackCoordinator queueFeedbackCoordinator;
    private final Supplier<HubHotbarService> hotbarServiceSupplier;
    private final Supplier<ZMenuUiService> menuUiServiceSupplier;

    QueueEntryButtonLoader(
            Plugin plugin,
            HubQueueMenuService queueMenuService,
            HubScoreboardModule scoreboardModule,
            PaperFeedbackService feedbackService,
            HubQueueFeedbackCoordinator queueFeedbackCoordinator,
            Supplier<HubHotbarService> hotbarServiceSupplier,
            Supplier<ZMenuUiService> menuUiServiceSupplier
    ) {
        super(plugin, "PP_QUEUE_ENTRY");
        this.queueMenuService = Objects.requireNonNull(queueMenuService, "queueMenuService");
        this.scoreboardModule = Objects.requireNonNull(scoreboardModule, "scoreboardModule");
        this.feedbackService = Objects.requireNonNull(feedbackService, "feedbackService");
        this.queueFeedbackCoordinator = Objects.requireNonNull(queueFeedbackCoordinator, "queueFeedbackCoordinator");
        this.hotbarServiceSupplier = Objects.requireNonNull(hotbarServiceSupplier, "hotbarServiceSupplier");
        this.menuUiServiceSupplier = Objects.requireNonNull(menuUiServiceSupplier, "menuUiServiceSupplier");
    }

    @Override
    public Button load(YamlConfiguration configuration, String path, DefaultButtonValue defaultButtonValue) {
        String rawQueueId = configuration.getString(path + "queue-id", "").trim();
        if (rawQueueId.isBlank()) {
            throw new IllegalStateException("PP_QUEUE_ENTRY requires 'queue-id' at path '" + path + "'");
        }
        return new QueueEntryButton(
                queueMenuService,
                scoreboardModule,
                feedbackService,
                queueFeedbackCoordinator,
                hotbarServiceSupplier,
                menuUiServiceSupplier,
                new QueueId(rawQueueId),
                configuration.getBoolean(path + "close-on-success", false),
                configuration.getBoolean(path + "refresh-menu-on-click", true),
                loadOptionalItem(configuration, path + "current-queue-item", defaultButtonValue),
                loadOptionalItem(configuration, path + "other-queue-item", defaultButtonValue),
                loadOptionalItem(configuration, path + "disabled-item", defaultButtonValue),
                loadOptionalItem(configuration, path + "missing-item", defaultButtonValue)
        );
    }

    private MenuItemStack loadOptionalItem(YamlConfiguration configuration, String path, DefaultButtonValue defaultButtonValue) {
        if (!configuration.isConfigurationSection(path)) {
            return null;
        }
        File file = Objects.requireNonNull(defaultButtonValue.getFile(), "defaultButtonValue.file");
        Plugin zMenu = Bukkit.getPluginManager().getPlugin("zMenu");
        if (!(zMenu instanceof MenuPlugin menuPlugin)) {
            throw new IllegalStateException("zMenu must be available to load PP_QUEUE_ENTRY item overrides");
        }
        return menuPlugin.loadItemStack(configuration, path, file);
    }
}

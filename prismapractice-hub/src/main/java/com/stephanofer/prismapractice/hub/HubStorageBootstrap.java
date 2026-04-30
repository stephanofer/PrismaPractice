package com.stephanofer.prismapractice.hub;

import com.stephanofer.prismapractice.data.mysql.MySqlStorageBootstrap;
import com.stephanofer.prismapractice.feedback.FeedbackConfigDescriptorFactory;
import com.stephanofer.prismapractice.data.mysql.StorageRuntime;
import com.stephanofer.prismapractice.hub.hotbar.HubHotbarConfigDescriptorFactory;
import com.stephanofer.prismapractice.paper.scoreboard.PaperScoreboardConfigDescriptorFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

final class HubStorageBootstrap {

    private HubStorageBootstrap() {
    }

    static StorageRuntime bootstrap(Path dataDirectory, ClassLoader classLoader, Consumer<String> logger) {
        return bootstrap(dataDirectory, classLoader, logger, new MySqlStorageBootstrap());
    }

    static StorageRuntime bootstrap(Path dataDirectory, ClassLoader classLoader, Consumer<String> logger, MySqlStorageBootstrap bootstrap) {
        return bootstrap.bootstrapRuntime(
                dataDirectory,
                classLoader,
                logger,
                "hub",
                List.of(
                        FeedbackConfigDescriptorFactory.descriptor("hub-feedback", "feedback.yml", "defaults/feedback.yml"),
                        HubHotbarConfigDescriptorFactory.descriptor(),
                        PaperScoreboardConfigDescriptorFactory.descriptor("hub-scoreboards", "scoreboards.yml", "defaults/scoreboards.yml")
                )
        );
    }
}

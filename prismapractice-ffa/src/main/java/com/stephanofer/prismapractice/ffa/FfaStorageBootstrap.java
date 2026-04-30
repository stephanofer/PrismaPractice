package com.stephanofer.prismapractice.ffa;

import com.stephanofer.prismapractice.data.mysql.MySqlStorageBootstrap;
import com.stephanofer.prismapractice.data.mysql.StorageRuntime;
import com.stephanofer.prismapractice.paper.scoreboard.PaperScoreboardConfigDescriptorFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

final class FfaStorageBootstrap {

    private FfaStorageBootstrap() {
    }

    static StorageRuntime bootstrap(Path dataDirectory, ClassLoader classLoader, Consumer<String> logger) {
        return bootstrap(dataDirectory, classLoader, logger, new MySqlStorageBootstrap());
    }

    static StorageRuntime bootstrap(Path dataDirectory, ClassLoader classLoader, Consumer<String> logger, MySqlStorageBootstrap bootstrap) {
        return bootstrap.bootstrapRuntime(
                dataDirectory,
                classLoader,
                logger,
                "ffa",
                List.of(PaperScoreboardConfigDescriptorFactory.descriptor("ffa-scoreboards", "scoreboards.yml", "defaults/scoreboards.yml"))
        );
    }
}

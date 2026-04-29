package com.stephanofer.prismapractice.ffa;

import com.stephanofer.prismapractice.data.mysql.MySqlStorageBootstrap;
import com.stephanofer.prismapractice.data.mysql.StorageRuntime;

import java.nio.file.Path;
import java.util.function.Consumer;

final class FfaStorageBootstrap {

    private FfaStorageBootstrap() {
    }

    static StorageRuntime bootstrap(Path dataDirectory, ClassLoader classLoader, Consumer<String> logger) {
        return bootstrap(dataDirectory, classLoader, logger, new MySqlStorageBootstrap());
    }

    static StorageRuntime bootstrap(Path dataDirectory, ClassLoader classLoader, Consumer<String> logger, MySqlStorageBootstrap bootstrap) {
        return bootstrap.bootstrapRuntime(dataDirectory, classLoader, logger, "ffa");
    }
}

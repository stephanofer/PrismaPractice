package com.stephanofer.prismapractice.data.mysql;

import com.zaxxer.hikari.HikariDataSource;
import com.stephanofer.prismapractice.config.ConfigDescriptor;
import com.stephanofer.prismapractice.config.ConfigBootstrapResult;
import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.config.ConfigPlatforms;
import com.stephanofer.prismapractice.data.redis.RedisStorage;
import com.stephanofer.prismapractice.data.redis.RedisStorageBootstrap;
import com.stephanofer.prismapractice.debug.DebugCategories;
import com.stephanofer.prismapractice.debug.DebugConfig;
import com.stephanofer.prismapractice.debug.DebugConsoleSink;
import com.stephanofer.prismapractice.debug.DebugController;
import com.stephanofer.prismapractice.debug.DebugDetailLevel;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class MySqlStorageBootstrap {

    private final MySqlPoolFactory poolFactory;
    private final MySqlConnectionVerifier connectionVerifier;
    private final FlywayMigrationService migrationService;
    private final RedisStorageBootstrap redisBootstrap;

    public MySqlStorageBootstrap() {
        this(new MySqlPoolFactory(), new MySqlConnectionVerifier(), new FlywayMigrationService(), new RedisStorageBootstrap());
    }

    MySqlStorageBootstrap(MySqlPoolFactory poolFactory, MySqlConnectionVerifier connectionVerifier, FlywayMigrationService migrationService, RedisStorageBootstrap redisBootstrap) {
        this.poolFactory = Objects.requireNonNull(poolFactory, "poolFactory");
        this.connectionVerifier = Objects.requireNonNull(connectionVerifier, "connectionVerifier");
        this.migrationService = Objects.requireNonNull(migrationService, "migrationService");
        this.redisBootstrap = Objects.requireNonNull(redisBootstrap, "redisBootstrap");
    }

    public static StorageRuntime bootstrap(Path dataDirectory, ClassLoader classLoader, Consumer<String> logger, String runtimeName) {
        return new MySqlStorageBootstrap().bootstrapRuntime(dataDirectory, classLoader, logger, runtimeName);
    }

    public StorageRuntime bootstrapRuntime(Path dataDirectory, ClassLoader classLoader, Consumer<String> logger, String runtimeName) {
        return bootstrapRuntime(dataDirectory, classLoader, logger, runtimeName, List.of());
    }

    public StorageRuntime bootstrapRuntime(
            Path dataDirectory,
            ClassLoader classLoader,
            Consumer<String> logger,
            String runtimeName,
            Collection<ConfigDescriptor<?>> additionalDescriptors
    ) {
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        Objects.requireNonNull(classLoader, "classLoader");
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(runtimeName, "runtimeName");
        Objects.requireNonNull(additionalDescriptors, "additionalDescriptors");

        List<ConfigDescriptor<?>> descriptors = new ArrayList<>();
        descriptors.add(StorageConfigDescriptorFactory.storageDescriptor());
        descriptors.addAll(additionalDescriptors);

        ConfigManager configManager = new ConfigManager(
                ConfigPlatforms.fromClassLoader(dataDirectory, classLoader, logger::accept),
                descriptors
        );

        ConfigBootstrapResult bootstrapResult = configManager.loadAll();
        DebugController debug = new DebugController(
                runtimeName,
                configManager.findManaged("runtime-debug")
                        .map(managed -> (DebugConfig) managed.value())
                        .orElse(DebugConfig.defaults()),
                DebugConsoleSink.consumer(logger)
        );
        debug.info(
                DebugCategories.BOOTSTRAP,
                DebugDetailLevel.BASIC,
                "bootstrap.config.loaded",
                "Runtime configuration loaded",
                debug.context()
                        .field("created", bootstrapResult.createdFiles())
                        .field("updated", bootstrapResult.updatedFiles())
                        .field("migrated", bootstrapResult.migratedFiles())
                        .field("recovered", bootstrapResult.recoveredFiles())
                        .field("warnings", bootstrapResult.warnings())
                        .build()
        );

        MySqlStorageConfig config = configManager.get("storage", MySqlStorageConfig.class);
        HikariDataSource dataSource = null;
        RedisStorage redisStorage = null;
        try {
            dataSource = poolFactory.create(config, "prismapractice-" + runtimeName + "-mysql");
            connectionVerifier.verify(dataSource, config.startup().testQuery());
            FlywayMigrationSummary migrationSummary = migrationService.migrate(classLoader, dataSource, config, "prismapractice-" + runtimeName);
            redisStorage = redisBootstrap.bootstrapRuntime(dataDirectory, classLoader, logger, runtimeName, debug);
            debug.info(
                    DebugCategories.STORAGE_MYSQL,
                    DebugDetailLevel.BASIC,
                    "mysql.bootstrap.completed",
                    "MySQL storage bootstrapped",
                    debug.context()
                            .field("jdbcUrl", config.safeJdbcUrl())
                            .field("poolMax", config.pool().maximumPoolSize())
                            .field("migrationsExecuted", migrationSummary.migrationsExecuted())
                            .field("targetVersion", migrationSummary.targetVersion())
                            .field("migrationWarnings", migrationSummary.warnings())
                            .build()
            );
            return new StorageRuntime(configManager, new MySqlStorage(runtimeName, config, dataSource, migrationSummary, debug), redisStorage, debug);
        } catch (RuntimeException exception) {
            closeQuietly(redisStorage);
            closeQuietly(dataSource);
            debug.error(DebugCategories.STORAGE_MYSQL, "mysql.bootstrap.failed", "Failed to bootstrap MySQL storage", debug.context().build(), exception);
            throw new StorageBootstrapException("Failed to bootstrap MySQL storage for runtime '" + runtimeName + "'", exception);
        }
    }

    private void closeQuietly(RedisStorage redisStorage) {
        if (redisStorage == null) {
            return;
        }
        try {
            redisStorage.close();
        } catch (RuntimeException ignored) {
        }
    }

    private void closeQuietly(HikariDataSource dataSource) {
        if (dataSource == null) {
            return;
        }
        try {
            dataSource.close();
        } catch (RuntimeException ignored) {
        }
    }
}

package com.stephanofer.prismapractice.hub;

import com.stephanofer.prismapractice.config.ConfigBootstrapResult;
import com.stephanofer.prismapractice.config.ConfigDescriptor;
import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.config.ConfigPlatforms;
import com.stephanofer.prismapractice.config.YamlConfigHelper;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

final class HubDemoConfigBootstrap {

    private HubDemoConfigBootstrap() {
    }

    static ConfigManager bootstrap(Path dataDirectory, ClassLoader classLoader, Consumer<String> logger) {
        ConfigManager manager = new ConfigManager(
                ConfigPlatforms.fromClassLoader(dataDirectory, classLoader, logger::accept),
                List.of(
                        demoConfigDescriptor(),
                        demoMessagesDescriptor()
                )
        );

        ConfigBootstrapResult bootstrapResult = manager.loadAll();
        DemoConfig demoConfig = manager.get("demo-config", DemoConfig.class);
        DemoMessages demoMessages = manager.get("demo-messages", DemoMessages.class);

        logger.accept("[demo-config] plugin=hub, sample-text=" + demoConfig.sampleText() + ", sample-number=" + demoConfig.sampleNumber() + ", auto-flag=" + demoConfig.autoFlag());
        logger.accept("[demo-messages] plugin=hub, greeting=" + demoMessages.greeting() + ", footer=" + demoMessages.footer());
        logger.accept("[demo-summary] created=" + bootstrapResult.createdFiles() + ", updated=" + bootstrapResult.updatedFiles() + ", migrated=" + bootstrapResult.migratedFiles() + ", recovered=" + bootstrapResult.recoveredFiles() + ", warnings=" + bootstrapResult.warnings());
        return manager;
    }

    private static ConfigDescriptor<DemoConfig> demoConfigDescriptor() {
        return ConfigDescriptor.builder("demo-config", DemoConfig.class)
                .filePath("config.yml")
                .bundledResourcePath("defaults/config.yml")
                .schemaVersion(2)
                .migration(1, root -> YamlConfigHelper.move(root, "sample.old-number", "sample.number"))
                .mapper(root -> new DemoConfig(
                        YamlConfigHelper.string(root, "sample-text"),
                        YamlConfigHelper.integer(YamlConfigHelper.section(root, "sample"), "number"),
                        YamlConfigHelper.bool(YamlConfigHelper.section(root, "toggles"), "auto-flag")
                ))
                .build();
    }

    private static ConfigDescriptor<DemoMessages> demoMessagesDescriptor() {
        return ConfigDescriptor.builder("demo-messages", DemoMessages.class)
                .filePath("messages/demo.yml")
                .bundledResourcePath("defaults/messages/demo.yml")
                .schemaVersion(1)
                .mapper(root -> new DemoMessages(
                        YamlConfigHelper.string(root, "greeting"),
                        YamlConfigHelper.string(root, "footer")
                ))
                .build();
    }

    private record DemoConfig(String sampleText, int sampleNumber, boolean autoFlag) {
    }

    private record DemoMessages(String greeting, String footer) {
    }
}

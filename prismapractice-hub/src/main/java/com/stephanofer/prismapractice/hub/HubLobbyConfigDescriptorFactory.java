package com.stephanofer.prismapractice.hub;

import com.stephanofer.prismapractice.config.ConfigDescriptor;
import com.stephanofer.prismapractice.config.ConfigException;
import com.stephanofer.prismapractice.config.YamlConfigHelper;

import java.util.Map;

public final class HubLobbyConfigDescriptorFactory {

    private HubLobbyConfigDescriptorFactory() {
    }

    public static ConfigDescriptor<HubLobbyConfig> descriptor() {
        return ConfigDescriptor.builder("hub-lobby", HubLobbyConfig.class)
                .filePath("hub-lobby.yml")
                .bundledResourcePath("defaults/hub-lobby.yml")
                .schemaVersion(1)
                .mapper(HubLobbyConfigDescriptorFactory::map)
                .validator(HubLobbyConfigDescriptorFactory::validate)
                .build();
    }

    private static HubLobbyConfig map(Map<String, Object> root) {
        Map<String, Object> lobby = YamlConfigHelper.section(root, "lobby");
        if (lobby.isEmpty() || !YamlConfigHelper.bool(lobby, "set")) {
            return new HubLobbyConfig(null);
        }
        return new HubLobbyConfig(new HubLobbyPoint(
                YamlConfigHelper.string(lobby, "world"),
                number(lobby, "x"),
                number(lobby, "y"),
                number(lobby, "z"),
                (float) number(lobby, "yaw"),
                (float) number(lobby, "pitch")
        ));
    }

    private static void validate(HubLobbyConfig config) {
        if (config.lobby() == null) {
            return;
        }
        if (config.lobby().world() == null || config.lobby().world().isBlank()) {
            throw new ConfigException("lobby.world must not be blank when lobby.set is true");
        }
    }

    private static double number(Map<String, Object> root, String key) {
        Object value = root.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new ConfigException("Expected number at key '" + key + "'");
    }
}

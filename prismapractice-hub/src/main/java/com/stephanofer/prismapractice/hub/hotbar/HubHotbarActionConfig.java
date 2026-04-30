package com.stephanofer.prismapractice.hub.hotbar;

import java.util.List;
import java.util.Objects;

record HubHotbarActionConfig(
        HubHotbarActionType type,
        HubHotbarActionTrigger trigger,
        String target,
        String command,
        String pluginName,
        int page,
        List<String> arguments,
        String customKey
) {

    HubHotbarActionConfig {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(trigger, "trigger");
        Objects.requireNonNull(arguments, "arguments");
        target = target == null ? "" : target;
        command = command == null ? "" : command;
        pluginName = pluginName == null ? "" : pluginName;
        customKey = customKey == null ? "" : customKey;
        arguments = List.copyOf(arguments);
    }
}

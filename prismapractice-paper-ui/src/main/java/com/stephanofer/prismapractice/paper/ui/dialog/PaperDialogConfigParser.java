package com.stephanofer.prismapractice.paper.ui.dialog;

import com.stephanofer.prismapractice.config.ConfigException;
import com.stephanofer.prismapractice.config.YamlConfigHelper;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogConfigModel.ActionButtonDefinition;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogConfigModel.ActionDefinition;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogConfigModel.ActionTypeDef;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogConfigModel.BodyDefinition;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogConfigModel.BodyTypeDef;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogConfigModel.BooleanInputSpec;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogConfigModel.ConfirmationSettings;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogConfigModel.Definition;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogConfigModel.DialogTypeDef;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogConfigModel.InputDefinition;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogConfigModel.InputTypeDef;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogConfigModel.ItemBodyDefinition;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogConfigModel.ItemDefinition;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogConfigModel.NoticeSettings;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogConfigModel.NumberRangeInputSpec;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogConfigModel.SingleOptionEntry;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogConfigModel.SingleOptionInputSpec;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogConfigModel.TextInputSpec;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class PaperDialogConfigParser {

    public Definition parse(String dialogId, Map<String, Object> root) {
        Objects.requireNonNull(dialogId, "dialogId");
        Objects.requireNonNull(root, "root");

        DialogTypeDef type = parseDialogType(string(root, "type", true));
        List<BodyDefinition> bodies = parseBodies(optionalSection(root, "body"));
        List<InputDefinition> inputs = parseInputs(optionalSection(root, "inputs"));
        NoticeSettings notice = type == DialogTypeDef.NOTICE ? parseNotice(optionalSection(root, "notice")) : null;
        ConfirmationSettings confirmation = type == DialogTypeDef.CONFIRMATION ? parseConfirmation(root) : null;
        List<ActionButtonDefinition> multiActions = type == DialogTypeDef.MULTI_ACTION ? parseMultiActions(optionalSection(root, "multi-actions")) : List.of();

        return new Definition(
                dialogId,
                string(root, "name", false, dialogId),
                stringAny(root, List.of("external-title", "external_title"), ""),
                type,
                bool(root, "can-close-with-escape", true),
                bool(root, "pause", false),
                stringAny(root, List.of("after-action", "after_action"), "CLOSE").trim().toUpperCase(Locale.ROOT),
                List.copyOf(bodies),
                List.copyOf(inputs),
                notice,
                confirmation,
                List.copyOf(multiActions)
        );
    }

    private List<BodyDefinition> parseBodies(Map<String, Object> root) {
        if (root.isEmpty()) {
            return List.of();
        }
        List<BodyDefinition> result = new ArrayList<>();
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> rawBody)) {
                throw new ConfigException("Dialog body '" + entry.getKey() + "' must be a map");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> section = (Map<String, Object>) rawBody;
            BodyTypeDef type = parseBodyType(string(section, "type", true));
            if (type == BodyTypeDef.PLAIN_MESSAGE) {
                result.add(new BodyDefinition(
                        entry.getKey(),
                        type,
                        stringList(section, "messages"),
                        integer(section, "width", 400),
                        null
                ));
                continue;
            }

            Map<String, Object> itemSection = YamlConfigHelper.section(section, "item");
            Map<String, Object> descriptionSection = optionalSection(section, "description");
            result.add(new BodyDefinition(
                    entry.getKey(),
                    type,
                    List.of(),
                    0,
                    new ItemBodyDefinition(
                            parseItem(itemSection),
                            stringList(descriptionSection, "messages"),
                            integer(descriptionSection, "width", 300),
                            bool(section, "show-decoration", true),
                            bool(section, "show-tooltip", true),
                            integer(section, "width", 180),
                            integer(section, "height", 120)
                    )
            ));
        }
        return result;
    }

    private List<InputDefinition> parseInputs(Map<String, Object> root) {
        if (root.isEmpty()) {
            return List.of();
        }
        List<InputDefinition> result = new ArrayList<>();
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> rawInput)) {
                throw new ConfigException("Dialog input '" + entry.getKey() + "' must be a map");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> section = (Map<String, Object>) rawInput;
            InputTypeDef type = parseInputType(string(section, "type", true));
            TextInputSpec textSpec = null;
            BooleanInputSpec booleanSpec = null;
            SingleOptionInputSpec singleOptionSpec = null;
            NumberRangeInputSpec numberRangeSpec = null;
            switch (type) {
                case TEXT -> {
                    Map<String, Object> multiline = optionalSection(section, "multiline");
                    textSpec = new TextInputSpec(
                            string(section, "initial-value", false, ""),
                            integer(section, "max-length", 100),
                            integer(multiline, "max-lines", 0),
                            integer(multiline, "height", 0)
                    );
                }
                case BOOLEAN -> booleanSpec = new BooleanInputSpec(
                        bool(section, "initial-value", false),
                        string(section, "text-true", false, "True"),
                        string(section, "text-false", false, "False")
                );
                case SINGLE_OPTION -> {
                    Map<String, Object> optionsSection = YamlConfigHelper.section(section, "options");
                    List<SingleOptionEntry> options = new ArrayList<>();
                    for (Map.Entry<String, Object> optionEntry : optionsSection.entrySet()) {
                        if (!(optionEntry.getValue() instanceof Map<?, ?> rawOption)) {
                            throw new ConfigException("Dialog input option '" + optionEntry.getKey() + "' must be a map");
                        }
                        @SuppressWarnings("unchecked")
                        Map<String, Object> optionSection = (Map<String, Object>) rawOption;
                        options.add(new SingleOptionEntry(
                                string(optionSection, "id", true),
                                string(optionSection, "display", true),
                                bool(optionSection, "initial", false)
                        ));
                    }
                    singleOptionSpec = new SingleOptionInputSpec(List.copyOf(options));
                }
                case NUMBER_RANGE -> numberRangeSpec = new NumberRangeInputSpec(
                        floatNumber(section, "start", 0f),
                        floatNumber(section, "end", 100f),
                        floatNumber(section, "step", 1f),
                        floatNumber(section, "initial-value", 0f),
                        string(section, "label-format", false, "")
                );
            }
            result.add(new InputDefinition(
                    entry.getKey(),
                    type,
                    string(section, "label", false, entry.getKey()),
                    integer(section, "width", 300),
                    bool(section, "label-visible", true),
                    textSpec,
                    booleanSpec,
                    singleOptionSpec,
                    numberRangeSpec
            ));
        }
        return result;
    }

    private NoticeSettings parseNotice(Map<String, Object> root) {
        return new NoticeSettings(
                string(root, "text", false, "OK"),
                string(root, "tooltip", false, ""),
                integer(root, "width", 180),
                parseActions(optionalSection(root, "actions"))
        );
    }

    private ConfirmationSettings parseConfirmation(Map<String, Object> root) {
        Map<String, Object> confirmation = optionalSection(root, "confirmation");
        return new ConfirmationSettings(
                string(confirmation, "yes-text", false, "Confirm"),
                string(confirmation, "yes-tooltip", false, ""),
                integer(confirmation, "yes-width", 140),
                parseActions(optionalSection(root, "yes-actions")),
                string(confirmation, "no-text", false, "Cancel"),
                string(confirmation, "no-tooltip", false, ""),
                integer(confirmation, "no-width", 140),
                parseActions(optionalSection(root, "no-actions"))
        );
    }

    private List<ActionButtonDefinition> parseMultiActions(Map<String, Object> root) {
        if (root.isEmpty()) {
            return List.of();
        }
        List<ActionButtonDefinition> result = new ArrayList<>();
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> rawButton)) {
                throw new ConfigException("Dialog action button '" + entry.getKey() + "' must be a map");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> section = (Map<String, Object>) rawButton;
            result.add(new ActionButtonDefinition(
                    string(section, "text", false, entry.getKey()),
                    string(section, "tooltip", false, ""),
                    integer(section, "width", 180),
                    parseActions(optionalSection(section, "actions"))
            ));
        }
        return result;
    }

    private List<ActionDefinition> parseActions(Map<String, Object> root) {
        if (root.isEmpty()) {
            return List.of();
        }
        List<ActionDefinition> result = new ArrayList<>();
        for (Object value : root.values()) {
            if (!(value instanceof Map<?, ?> rawGroup)) {
                throw new ConfigException("Dialog action group must be a map");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> group = (Map<String, Object>) rawGroup;
            Object success = group.get("success");
            if (!(success instanceof List<?> list)) {
                continue;
            }
            for (Object element : list) {
                if (!(element instanceof Map<?, ?> rawAction)) {
                    throw new ConfigException("Dialog action entry must be a map");
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> action = (Map<String, Object>) rawAction;
                ActionTypeDef type = parseActionType(string(action, "type", true));
                String valueText = switch (type) {
                    case MESSAGE -> {
                        List<String> messages = stringList(action, "messages");
                        yield String.join("\n", messages);
                    }
                    case OPEN_DIALOG -> stringAny(action, List.of("dialog", "target", "value"), "");
                    case RUN_PLAYER_COMMAND, RUN_CONSOLE_COMMAND -> stringAny(action, List.of("command", "value"), "");
                    case BACK, CLOSE -> "";
                };
                result.add(new ActionDefinition(type, valueText));
            }
        }
        return result;
    }

    private ItemDefinition parseItem(Map<String, Object> root) {
        Material material = Material.matchMaterial(string(root, "material", true));
        if (material == null) {
            throw new ConfigException("Unknown material '" + root.get("material") + "'");
        }
        return new ItemDefinition(
                material,
                integer(root, "amount", 1),
                string(root, "name", false, ""),
                stringList(root, "lore"),
                bool(root, "glowing", false),
                bool(root, "hide-attributes", true),
                optionalInteger(root, "custom-model-data")
        );
    }

    private DialogTypeDef parseDialogType(String value) {
        return switch (normalize(value)) {
            case "NOTICE" -> DialogTypeDef.NOTICE;
            case "CONFIRMATION" -> DialogTypeDef.CONFIRMATION;
            case "MULTI_ACTION" -> DialogTypeDef.MULTI_ACTION;
            default -> throw new ConfigException("Unsupported dialog type '" + value + "'");
        };
    }

    private BodyTypeDef parseBodyType(String value) {
        return switch (normalize(value)) {
            case "PLAIN_MESSAGE" -> BodyTypeDef.PLAIN_MESSAGE;
            case "ITEM" -> BodyTypeDef.ITEM;
            default -> throw new ConfigException("Unsupported dialog body type '" + value + "'");
        };
    }

    private InputTypeDef parseInputType(String value) {
        return switch (normalize(value)) {
            case "DIALOG_TEXT", "TEXT" -> InputTypeDef.TEXT;
            case "DIALOG_BOOLEAN", "BOOLEAN" -> InputTypeDef.BOOLEAN;
            case "DIALOG_SINGLE_OPTION", "SINGLE_OPTION" -> InputTypeDef.SINGLE_OPTION;
            case "DIALOG_NUMBER_RANGE", "NUMBER_RANGE" -> InputTypeDef.NUMBER_RANGE;
            default -> throw new ConfigException("Unsupported dialog input type '" + value + "'");
        };
    }

    private ActionTypeDef parseActionType(String value) {
        return switch (normalize(value)) {
            case "MESSAGE" -> ActionTypeDef.MESSAGE;
            case "OPEN_DIALOG" -> ActionTypeDef.OPEN_DIALOG;
            case "BACK" -> ActionTypeDef.BACK;
            case "CLOSE" -> ActionTypeDef.CLOSE;
            case "RUN_PLAYER_COMMAND" -> ActionTypeDef.RUN_PLAYER_COMMAND;
            case "RUN_CONSOLE_COMMAND" -> ActionTypeDef.RUN_CONSOLE_COMMAND;
            default -> throw new ConfigException("Unsupported dialog action type '" + value + "'");
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> optionalSection(Map<String, Object> root, String key) {
        Object value = root.get(key);
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> map)) {
            throw new ConfigException("Expected map at key '" + key + "'");
        }
        return new LinkedHashMap<>((Map<String, Object>) map);
    }

    private String stringAny(Map<String, Object> root, List<String> keys, String defaultValue) {
        for (String key : keys) {
            Object value = root.get(key);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return defaultValue;
    }

    private String string(Map<String, Object> root, String key, boolean required) {
        return string(root, key, required, null);
    }

    private String string(Map<String, Object> root, String key, boolean required, String defaultValue) {
        Object value = root.get(key);
        if (value == null) {
            if (required) {
                throw new ConfigException("Missing required key '" + key + "'");
            }
            return defaultValue;
        }
        return String.valueOf(value);
    }

    private int integer(Map<String, Object> root, String key, int defaultValue) {
        Object value = root.get(key);
        return value instanceof Number number ? number.intValue() : defaultValue;
    }

    private Integer optionalInteger(Map<String, Object> root, String key) {
        Object value = root.get(key);
        return value instanceof Number number ? number.intValue() : null;
    }

    private float floatNumber(Map<String, Object> root, String key, float defaultValue) {
        Object value = root.get(key);
        return value instanceof Number number ? number.floatValue() : defaultValue;
    }

    private boolean bool(Map<String, Object> root, String key, boolean defaultValue) {
        Object value = root.get(key);
        return value instanceof Boolean booleanValue ? booleanValue : defaultValue;
    }

    @SuppressWarnings("unchecked")
    private List<String> stringList(Map<String, Object> root, String key) {
        Object value = root.get(key);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new ConfigException("Expected list at key '" + key + "'");
        }
        List<String> result = new ArrayList<>();
        for (Object element : list) {
            if (!(element instanceof String stringValue)) {
                throw new ConfigException("Expected string list at key '" + key + "'");
            }
            result.add(stringValue);
        }
        return List.copyOf(result);
    }

    private String normalize(String value) {
        return value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    }
}

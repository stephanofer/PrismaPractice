package com.stephanofer.prismapractice.paper.ui.dialog;

import com.stephanofer.prismapractice.paper.ui.UiResourceInstaller;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogConfigModel.ActionButtonDefinition;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogConfigModel.ActionDefinition;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogConfigModel.ActionTypeDef;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogConfigModel.BodyDefinition;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogConfigModel.BodyTypeDef;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogConfigModel.Definition;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogConfigModel.InputDefinition;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogConfigModel.InputTypeDef;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody;
import io.papermc.paper.registry.data.dialog.input.BooleanDialogInput;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.NumberRangeDialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public final class PaperDialogService implements Listener {

    private final JavaPlugin plugin;
    private final Path dialogsDirectory;
    private final Collection<String> bundledResources;
    private final PaperDialogConfigParser parser;
    private final DialogSessionStore sessionStore;
    private final MiniMessage miniMessage;
    private final Yaml yaml;
    private final Map<String, Definition> definitions;

    public PaperDialogService(JavaPlugin plugin, Path dialogsDirectory, Collection<String> bundledResources) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.dialogsDirectory = Objects.requireNonNull(dialogsDirectory, "dialogsDirectory");
        this.bundledResources = List.copyOf(Objects.requireNonNull(bundledResources, "bundledResources"));
        this.parser = new PaperDialogConfigParser();
        this.sessionStore = new DialogSessionStore(Duration.ofMinutes(10));
        this.miniMessage = MiniMessage.miniMessage();
        this.yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        this.definitions = new LinkedHashMap<>();
    }

    public void initialize() {
        UiResourceInstaller.install(plugin, bundledResources);
        reload();
    }

    public void reload() {
        definitions.clear();
        try (Stream<Path> stream = Files.walk(dialogsDirectory)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".yml"))
                    .forEach(this::loadDefinition);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot scan dialog directory '" + dialogsDirectory + "'", exception);
        }
    }

    public Optional<Definition> findDefinition(String dialogId) {
        return Optional.ofNullable(definitions.get(dialogId));
    }

    public Collection<String> dialogIds() {
        return List.copyOf(definitions.keySet());
    }

    public DialogSessionStore sessionStore() {
        return sessionStore;
    }

    public boolean open(Player player, String dialogId) {
        Objects.requireNonNull(player, "player");
        if (dialogId == null || dialogId.isBlank()) {
            return false;
        }

        Definition definition = definitions.get(dialogId);
        if (definition == null) {
            plugin.getLogger().warning("Unknown native dialog '" + dialogId + "'.");
            return false;
        }

        DialogSession session = sessionStore.session(player.getUniqueId().toString());
        session.markCurrent(dialogId);
        io.papermc.paper.dialog.Dialog dialog = buildDialog(player, definition, session);
        player.showDialog(dialog);
        return true;
    }

    public void close() {
        sessionStore.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessionStore.remove(event.getPlayer().getUniqueId().toString());
    }

    private void loadDefinition(Path path) {
        try {
            String yamlText = Files.readString(path, StandardCharsets.UTF_8);
            Object loaded = yaml.load(yamlText);
            if (!(loaded instanceof Map<?, ?> rawMap)) {
                throw new IllegalStateException("Dialog file '" + path + "' must have a map root");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) rawMap;
            String fileName = path.getFileName().toString();
            String dialogId = fileName.substring(0, fileName.length() - 4);
            definitions.put(dialogId, parser.parse(dialogId, map));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read dialog file '" + path + "'", exception);
        }
    }

    private io.papermc.paper.dialog.Dialog buildDialog(Player player, Definition definition, DialogSession session) {
        List<DialogBody> bodies = buildBodies(player, definition, session);
        List<DialogInput> inputs = buildInputs(player, definition, session);
        DialogBase.Builder base = DialogBase.builder(component(resolve(definition.name(), player, session)))
                .externalTitle(component(resolve(definition.externalTitle(), player, session)))
                .canCloseWithEscape(definition.canCloseWithEscape())
                .pause(definition.pause())
                .afterAction(DialogBase.DialogAfterAction.valueOf(definition.afterAction()))
                .body(bodies)
                .inputs(inputs);

        return switch (definition.type()) {
            case NOTICE -> io.papermc.paper.dialog.Dialog.create(builder -> builder.empty()
                    .base(base.build())
                    .type(io.papermc.paper.registry.data.dialog.type.DialogType.notice(createNoticeButton(player, definition, inputs, session))));
            case CONFIRMATION -> io.papermc.paper.dialog.Dialog.create(builder -> builder.empty()
                    .base(base.build())
                    .type(io.papermc.paper.registry.data.dialog.type.DialogType.confirmation(
                            createConfirmationButton(player, definition, inputs, session, true),
                            createConfirmationButton(player, definition, inputs, session, false)
                    )));
            case MULTI_ACTION -> io.papermc.paper.dialog.Dialog.create(builder -> builder.empty()
                    .base(base.build())
                    .type(io.papermc.paper.registry.data.dialog.type.DialogType.multiAction(createMultiActionButtons(player, definition, inputs, session)).build()));
        };
    }

    private ActionButton createNoticeButton(Player player, Definition definition, List<DialogInput> inputs, DialogSession session) {
        return createActionButton(
                player,
                definition.notice().text(),
                definition.notice().tooltip(),
                definition.notice().width(),
                definition.notice().actions(),
                definition.id(),
                inputs,
                session
        );
    }

    private ActionButton createConfirmationButton(Player player, Definition definition, List<DialogInput> inputs, DialogSession session, boolean yes) {
        var config = definition.confirmation();
        return createActionButton(
                player,
                yes ? config.yesText() : config.noText(),
                yes ? config.yesTooltip() : config.noTooltip(),
                yes ? config.yesWidth() : config.noWidth(),
                yes ? config.yesActions() : config.noActions(),
                definition.id(),
                inputs,
                session
        );
    }

    private List<ActionButton> createMultiActionButtons(Player player, Definition definition, List<DialogInput> inputs, DialogSession session) {
        List<ActionButton> buttons = new ArrayList<>();
        for (ActionButtonDefinition button : definition.multiActions()) {
            buttons.add(createActionButton(
                    player,
                    button.text(),
                    button.tooltip(),
                    button.width(),
                    button.actions(),
                    definition.id(),
                    inputs,
                    session
            ));
        }
        return List.copyOf(buttons);
    }

    private ActionButton createActionButton(
            Player player,
            String text,
            String tooltip,
            int width,
            List<ActionDefinition> actions,
            String currentDialogId,
            List<DialogInput> inputs,
            DialogSession session
    ) {
        DialogAction action = actions.isEmpty() ? null : DialogAction.customClick(
                (view, audience) -> {
                    if (!(audience instanceof Player clickedPlayer)) {
                        return;
                    }
                    handleActions(clickedPlayer, view, session, currentDialogId, inputs, actions);
                },
                ClickCallback.Options.builder().uses(1).build()
        );
        return ActionButton.create(
                component(resolve(text, player, session)),
                component(resolve(tooltip, player, session)),
                width,
                action
        );
    }

    private void handleActions(
            Player player,
            DialogResponseView view,
            DialogSession session,
            String currentDialogId,
            List<DialogInput> inputs,
            List<ActionDefinition> actions
    ) {
        Map<String, String> capturedInputs = captureInputs(view, inputs);
        session.putAll(capturedInputs);
        session.markCurrent(currentDialogId);
        for (ActionDefinition action : actions) {
            switch (action.type()) {
                case MESSAGE -> player.sendRichMessage(resolve(action.value(), player, session));
                case RUN_PLAYER_COMMAND -> player.performCommand(stripSlash(resolve(action.value(), player, session)));
                case RUN_CONSOLE_COMMAND -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stripSlash(resolve(action.value(), player, session)));
                case OPEN_DIALOG -> {
                    String target = resolve(action.value(), player, session);
                    if (!target.isBlank() && !target.equals(currentDialogId)) {
                        session.pushHistory(currentDialogId);
                    }
                    open(player, target);
                    return;
                }
                case BACK -> {
                    String previous = session.popHistory();
                    if (previous != null && !previous.isBlank()) {
                        open(player, previous);
                    } else {
                        player.closeDialog();
                    }
                    return;
                }
                case CLOSE -> {
                    player.closeDialog();
                    return;
                }
            }
        }
    }

    private Map<String, String> captureInputs(DialogResponseView view, List<DialogInput> inputs) {
        Map<String, String> values = new LinkedHashMap<>();
        for (DialogInput input : inputs) {
            String key = input.key();
            switch (input) {
                case BooleanDialogInput boolInput -> {
                    boolean value = view.getBoolean(key);
                    values.put(key, String.valueOf(value));
                    values.put(key + "_text", value ? boolInput.onTrue() : boolInput.onFalse());
                }
                case NumberRangeDialogInput ignored -> values.put(key, String.valueOf(view.getFloat(key)));
                case TextDialogInput ignored -> values.put(key, String.valueOf(view.getText(key)));
                case SingleOptionDialogInput ignored -> values.put(key, String.valueOf(view.getText(key)));
                default -> {
                }
            }
        }
        return values;
    }

    private List<DialogBody> buildBodies(Player player, Definition definition, DialogSession session) {
        List<DialogBody> result = new ArrayList<>();
        for (BodyDefinition body : definition.bodies()) {
            if (body.type() == BodyTypeDef.PLAIN_MESSAGE) {
                List<Component> components = body.messages().stream()
                        .map(line -> component(resolve(line, player, session)))
                        .toList();
                Component finalComponent = components.size() == 1 ? components.getFirst() : Component.join(Component.newline(), components);
                result.add(DialogBody.plainMessage(finalComponent, body.width()));
                continue;
            }

            ItemStack itemStack = buildItem(body.itemBody().item(), player, session);
            PlainMessageDialogBody description = null;
            if (!body.itemBody().description().isEmpty()) {
                List<Component> descriptionLines = body.itemBody().description().stream()
                        .map(line -> component(resolve(line, player, session)))
                        .toList();
                Component finalDescription = descriptionLines.size() == 1 ? descriptionLines.getFirst() : Component.join(Component.newline(), descriptionLines);
                description = DialogBody.plainMessage(finalDescription, body.itemBody().descriptionWidth());
            }
            result.add(DialogBody.item(itemStack)
                    .description(description)
                    .showDecorations(body.itemBody().showDecoration())
                    .showTooltip(body.itemBody().showTooltip())
                    .width(body.itemBody().width())
                    .height(body.itemBody().height())
                    .build());
        }
        return List.copyOf(result);
    }

    private List<DialogInput> buildInputs(Player player, Definition definition, DialogSession session) {
        List<DialogInput> result = new ArrayList<>();
        for (InputDefinition input : definition.inputs()) {
            switch (input.type()) {
                case TEXT -> {
                    var spec = input.text();
                    TextDialogInput.MultilineOptions multiline = spec.multilineMaxLines() > 0 && spec.multilineHeight() > 0
                            ? TextDialogInput.MultilineOptions.create(spec.multilineMaxLines(), spec.multilineHeight())
                            : null;
                    String initialValue = preferSessionValue(session, input.key(), spec.initialValue());
                    result.add(DialogInput.text(
                            input.key(),
                            input.width(),
                            component(resolve(input.label(), player, session)),
                            input.labelVisible(),
                            truncate(initialValue, spec.maxLength()),
                            spec.maxLength(),
                            multiline
                    ));
                }
                case BOOLEAN -> {
                    var spec = input.bool();
                    boolean initialValue = Boolean.parseBoolean(preferSessionValue(session, input.key(), String.valueOf(spec.initialValue())));
                    result.add(DialogInput.bool(
                            input.key(),
                            component(resolve(input.label(), player, session)),
                            initialValue,
                            resolve(spec.textTrue(), player, session),
                            resolve(spec.textFalse(), player, session)
                    ));
                }
                case SINGLE_OPTION -> {
                    List<SingleOptionDialogInput.OptionEntry> options = new ArrayList<>();
                    String storedValue = session.value(input.key());
                    for (var option : input.singleOption().options()) {
                        boolean selected = storedValue == null ? option.initial() : storedValue.equals(option.id());
                        options.add(SingleOptionDialogInput.OptionEntry.create(option.id(), component(resolve(option.display(), player, session)), selected));
                    }
                    result.add(DialogInput.singleOption(
                            input.key(),
                            input.width(),
                            options,
                            component(resolve(input.label(), player, session)),
                            input.labelVisible()
                    ));
                }
                case NUMBER_RANGE -> {
                    var spec = input.numberRange();
                    float initialValue = spec.initialValue();
                    String storedValue = session.value(input.key());
                    if (storedValue != null && !storedValue.isBlank()) {
                        try {
                            initialValue = Float.parseFloat(storedValue);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    if (initialValue < spec.start() || initialValue > spec.end()) {
                        initialValue = (spec.start() + spec.end()) / 2.0f;
                    }
                    result.add(DialogInput.numberRange(
                            input.key(),
                            input.width(),
                            component(resolve(input.label(), player, session)),
                            resolve(spec.labelFormat(), player, session),
                            spec.start(),
                            spec.end(),
                            initialValue,
                            spec.step()
                    ));
                }
            }
        }
        return List.copyOf(result);
    }

    private ItemStack buildItem(PaperDialogConfigModel.ItemDefinition definition, Player player, DialogSession session) {
        ItemStack itemStack = new ItemStack(Optional.ofNullable(definition.material()).orElse(Material.PAPER), Math.max(1, definition.amount()));
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            if (!definition.name().isBlank()) {
                itemMeta.displayName(component(resolve(definition.name(), player, session)));
            }
            if (!definition.lore().isEmpty()) {
                itemMeta.lore(definition.lore().stream().map(line -> component(resolve(line, player, session))).toList());
            }
            if (definition.hideAttributes()) {
                itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            }
            if (definition.customModelData() != null) {
                itemMeta.setCustomModelData(definition.customModelData());
            }
            itemStack.setItemMeta(itemMeta);
        }
        if (definition.glowing()) {
            itemStack.editMeta(meta -> meta.setEnchantmentGlintOverride(true));
        }
        return itemStack;
    }

    private String preferSessionValue(DialogSession session, String key, String fallback) {
        String value = session.value(key);
        return value == null ? fallback : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private Component component(String value) {
        return miniMessage.deserialize(value == null ? "" : value);
    }

    private String resolve(String raw, Player player, DialogSession session) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String resolved = raw
                .replace("%player_name%", player.getName())
                .replace("%player_uuid%", player.getUniqueId().toString())
                .replace("%dialog_id%", Optional.ofNullable(session.currentDialogId()).orElse(""));
        for (Map.Entry<String, String> entry : session.snapshot().entrySet()) {
            resolved = resolved.replace("%session_" + entry.getKey() + "%", entry.getValue());
        }
        return resolved;
    }

    private String stripSlash(String command) {
        return command.startsWith("/") ? command.substring(1) : command;
    }
}

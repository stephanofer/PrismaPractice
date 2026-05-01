package com.stephanofer.prismapractice.paper.ui.dialog;

import org.bukkit.Material;

import java.util.List;

public final class PaperDialogConfigModel {

    private PaperDialogConfigModel() {
    }

    public enum DialogTypeDef {
        NOTICE,
        CONFIRMATION,
        MULTI_ACTION
    }

    public enum BodyTypeDef {
        PLAIN_MESSAGE,
        ITEM
    }

    public enum InputTypeDef {
        TEXT,
        BOOLEAN,
        SINGLE_OPTION,
        NUMBER_RANGE
    }

    public enum ActionTypeDef {
        MESSAGE,
        OPEN_DIALOG,
        BACK,
        CLOSE,
        RUN_PLAYER_COMMAND,
        RUN_CONSOLE_COMMAND
    }

    public record Definition(
            String id,
            String name,
            String externalTitle,
            DialogTypeDef type,
            boolean canCloseWithEscape,
            boolean pause,
            String afterAction,
            List<BodyDefinition> bodies,
            List<InputDefinition> inputs,
            NoticeSettings notice,
            ConfirmationSettings confirmation,
            List<ActionButtonDefinition> multiActions
    ) {
    }

    public record NoticeSettings(String text, String tooltip, int width, List<ActionDefinition> actions) {
    }

    public record ConfirmationSettings(
            String yesText,
            String yesTooltip,
            int yesWidth,
            List<ActionDefinition> yesActions,
            String noText,
            String noTooltip,
            int noWidth,
            List<ActionDefinition> noActions
    ) {
    }

    public record ActionButtonDefinition(String text, String tooltip, int width, List<ActionDefinition> actions) {
    }

    public record BodyDefinition(
            String key,
            BodyTypeDef type,
            List<String> messages,
            int width,
            ItemBodyDefinition itemBody
    ) {
    }

    public record ItemBodyDefinition(
            ItemDefinition item,
            List<String> description,
            int descriptionWidth,
            boolean showDecoration,
            boolean showTooltip,
            int width,
            int height
    ) {
    }

    public record ItemDefinition(
            Material material,
            int amount,
            String name,
            List<String> lore,
            boolean glowing,
            boolean hideAttributes,
            Integer customModelData
    ) {
    }

    public record InputDefinition(
            String key,
            InputTypeDef type,
            String label,
            int width,
            boolean labelVisible,
            TextInputSpec text,
            BooleanInputSpec bool,
            SingleOptionInputSpec singleOption,
            NumberRangeInputSpec numberRange
    ) {
    }

    public record TextInputSpec(String initialValue, int maxLength, int multilineMaxLines, int multilineHeight) {
    }

    public record BooleanInputSpec(boolean initialValue, String textTrue, String textFalse) {
    }

    public record SingleOptionInputSpec(List<SingleOptionEntry> options) {
    }

    public record SingleOptionEntry(String id, String display, boolean initial) {
    }

    public record NumberRangeInputSpec(float start, float end, float step, float initialValue, String labelFormat) {
    }

    public record ActionDefinition(ActionTypeDef type, String value) {
    }
}

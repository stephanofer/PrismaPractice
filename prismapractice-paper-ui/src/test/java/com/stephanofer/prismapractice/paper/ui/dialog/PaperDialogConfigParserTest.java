package com.stephanofer.prismapractice.paper.ui.dialog;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PaperDialogConfigParserTest {

    @Test
    void shouldParseConfirmationDialogWithAllInputTypes() {
        PaperDialogConfigParser parser = new PaperDialogConfigParser();

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("name", "Demo Configurator");
        root.put("external-title", "Configurator");
        root.put("type", "confirmation");
        root.put("after-action", "CLOSE");

        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("nickname", Map.of(
                "type", "dialog_text",
                "label", "Nickname",
                "initial-value", "tester",
                "max-length", 16
        ));
        inputs.put("spectators", Map.of(
                "type", "dialog_boolean",
                "label", "Spectators",
                "initial-value", true,
                "text-true", "Yes",
                "text-false", "No"
        ));
        inputs.put("theme", Map.of(
                "type", "dialog_single_option",
                "label", "Theme",
                "options", Map.of(
                        "bronze", Map.of("id", "bronze", "display", "Bronze", "initial", true),
                        "gold", Map.of("id", "gold", "display", "Gold", "initial", false)
                )
        ));
        inputs.put("rounds", Map.of(
                "type", "dialog_number_range",
                "label", "Rounds",
                "start", 1,
                "end", 7,
                "step", 1,
                "initial-value", 3
        ));
        root.put("inputs", inputs);
        root.put("confirmation", Map.of(
                "yes-text", "Confirm",
                "no-text", "Cancel"
        ));
        root.put("yes-actions", Map.of(
                "1", Map.of("success", List.of(Map.of("type", "open_dialog", "dialog", "demo-review")))
        ));

        PaperDialogConfigModel.Definition definition = parser.parse("demo-configurator", root);

        assertEquals(PaperDialogConfigModel.DialogTypeDef.CONFIRMATION, definition.type());
        assertEquals(4, definition.inputs().size());
        assertEquals("demo-review", definition.confirmation().yesActions().getFirst().value());
        assertNotNull(definition.inputs().getFirst().text());
        assertNotNull(definition.inputs().get(1).bool());
        assertNotNull(definition.inputs().get(2).singleOption());
        assertNotNull(definition.inputs().get(3).numberRange());
    }
}

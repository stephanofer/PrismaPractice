package com.stephanofer.prismapractice.hub.ui;

import com.stephanofer.prismapractice.api.queue.QueueDefinition;
import fr.maxlego08.menu.api.utils.Placeholders;
import org.bukkit.entity.Player;

final class QueueMenuButtonSupport {

    private QueueMenuButtonSupport() {
    }

    static Placeholders placeholders(Player player, QueueMenuView view) {
        Placeholders placeholders = new Placeholders();
        QueueDefinition definition = view.definition();
        placeholders.register("pp_player_name", player.getName());
        placeholders.register("pp_queue_id", view.targetQueueId().toString());
        placeholders.register("pp_queue_name", definition == null ? view.targetQueueId().toString() : definition.displayName());
        placeholders.register("pp_queue_mode_id", definition == null ? "unknown" : definition.modeId().toString());
        placeholders.register("pp_queue_players", Integer.toString(view.playerCount()));
        placeholders.register("pp_queue_state", view.state().name().toLowerCase(java.util.Locale.ROOT));
        placeholders.register("pp_queue_status_text", statusText(view.state()));
        placeholders.register("pp_queue_action_text", actionText(view.state()));
        placeholders.register("pp_active_queue_name", view.activeQueueName().isBlank() ? "Ninguna" : view.activeQueueName());
        placeholders.register("pp_active_queue_id", view.activeEntry() == null ? "none" : view.activeEntry().queueId().toString());
        return placeholders;
    }

    static String statusText(QueueMenuState state) {
        return switch (state) {
            case AVAILABLE -> "Disponible";
            case CURRENT_QUEUE -> "Ya estás en esta cola";
            case OTHER_QUEUE -> "Ya estás en otra cola";
            case DISABLED -> "Deshabilitada";
            case MISSING -> "No disponible";
        };
    }

    static String actionText(QueueMenuState state) {
        return switch (state) {
            case AVAILABLE -> "Click para entrar a la cola.";
            case CURRENT_QUEUE -> "Click para salir de esta cola.";
            case OTHER_QUEUE -> "Salí de tu cola actual para cambiar.";
            case DISABLED -> "Esta cola no está habilitada ahora.";
            case MISSING -> "La cola configurada no existe.";
        };
    }
}

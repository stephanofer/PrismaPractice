package com.stephanofer.prismapractice.command;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.Collection;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;

public final class PaperCommands {

    private PaperCommands() {
    }

    public static void register(
        final JavaPlugin plugin,
        final PaperCommandServiceContainer services,
        final Collection<CommandSpec> commandSpecs
    ) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(services, "services");
        Objects.requireNonNull(commandSpecs, "commandSpecs");

        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands registrar = event.registrar();
            final PaperCommandCompiler compiler = new PaperCommandCompiler(services);

            for (final CommandSpec commandSpec : commandSpecs) {
                registrar.register(
                    compiler.compile(commandSpec),
                    commandSpec.description() == null ? "" : commandSpec.description(),
                    commandSpec.aliases()
                );
            }
        });
    }
}

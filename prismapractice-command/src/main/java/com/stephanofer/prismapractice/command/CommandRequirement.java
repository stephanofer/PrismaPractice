package com.stephanofer.prismapractice.command;

import io.papermc.paper.command.brigadier.CommandSourceStack;

@FunctionalInterface
public interface CommandRequirement {

    boolean test(CommandSourceStack source);
}

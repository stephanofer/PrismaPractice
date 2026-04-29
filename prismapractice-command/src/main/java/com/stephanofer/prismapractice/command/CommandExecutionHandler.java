package com.stephanofer.prismapractice.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

@FunctionalInterface
public interface CommandExecutionHandler {

    int execute(PaperCommandContext context) throws CommandSyntaxException;
}

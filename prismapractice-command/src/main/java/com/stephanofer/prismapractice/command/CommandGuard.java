package com.stephanofer.prismapractice.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

@FunctionalInterface
public interface CommandGuard {

    CommandGuardResult check(PaperCommandContext context) throws CommandSyntaxException;
}

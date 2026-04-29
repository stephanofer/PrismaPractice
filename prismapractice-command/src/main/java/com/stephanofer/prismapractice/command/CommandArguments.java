package com.stephanofer.prismapractice.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.stephanofer.prismapractice.command.argument.OnlinePlayerArgument;
import org.bukkit.entity.Player;

public final class CommandArguments {

    private CommandArguments() {
    }

    public static CommandArgumentSpec<Boolean> bool(final String name) {
        return CommandArgumentSpec.argument(name, BoolArgumentType.bool());
    }

    public static CommandArgumentSpec<Integer> integer(final String name) {
        return CommandArgumentSpec.argument(name, IntegerArgumentType.integer());
    }

    public static CommandArgumentSpec<Integer> integer(final String name, final int min, final int max) {
        return CommandArgumentSpec.argument(name, IntegerArgumentType.integer(min, max));
    }

    public static CommandArgumentSpec<Long> longValue(final String name) {
        return CommandArgumentSpec.argument(name, LongArgumentType.longArg());
    }

    public static CommandArgumentSpec<Float> floatValue(final String name, final float min, final float max) {
        return CommandArgumentSpec.argument(name, FloatArgumentType.floatArg(min, max));
    }

    public static CommandArgumentSpec<Double> doubleValue(final String name, final double min, final double max) {
        return CommandArgumentSpec.argument(name, DoubleArgumentType.doubleArg(min, max));
    }

    public static CommandArgumentSpec<String> word(final String name) {
        return CommandArgumentSpec.argument(name, StringArgumentType.word());
    }

    public static CommandArgumentSpec<String> string(final String name) {
        return CommandArgumentSpec.argument(name, StringArgumentType.string());
    }

    public static CommandArgumentSpec<String> greedyString(final String name) {
        return CommandArgumentSpec.argument(name, StringArgumentType.greedyString());
    }

    public static CommandArgumentSpec<Player> onlinePlayer(final String name) {
        return CommandArgumentSpec.argument(name, new OnlinePlayerArgument());
    }
}

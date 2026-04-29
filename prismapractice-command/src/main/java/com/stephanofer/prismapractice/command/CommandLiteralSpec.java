package com.stephanofer.prismapractice.command;

import java.util.Objects;

public class CommandLiteralSpec extends CommandNodeSpec<CommandLiteralSpec> {

    private final String literal;

    protected CommandLiteralSpec(final String literal) {
        this.literal = Objects.requireNonNull(literal, "literal");
    }

    public static CommandLiteralSpec literal(final String literal) {
        return new CommandLiteralSpec(literal);
    }

    public String literal() {
        return this.literal;
    }

    @Override
    protected CommandLiteralSpec self() {
        return this;
    }
}

package com.catthoor.TipOfTheSpearBot.commands;

import java.util.Objects;

public class CommandKey {

    private String key;
    private boolean acceptsParameters;

    public CommandKey(String key, boolean acceptsParameters) {
        this.key = key;
        this.acceptsParameters = acceptsParameters;
    }

    public String getKey() {
        return key;
    }

    public boolean isAcceptsParameters() {
        return acceptsParameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommandKey that = (CommandKey) o;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}

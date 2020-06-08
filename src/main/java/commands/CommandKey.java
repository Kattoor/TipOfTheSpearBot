package commands;

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

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isAcceptsParameters() {
        return acceptsParameters;
    }

    public void setAcceptsParameters(boolean acceptsParameters) {
        this.acceptsParameters = acceptsParameters;
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

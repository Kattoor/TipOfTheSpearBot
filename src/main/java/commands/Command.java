package commands;

import discord4j.core.object.entity.Message;

public interface Command {

    public void execute(Message message);
}

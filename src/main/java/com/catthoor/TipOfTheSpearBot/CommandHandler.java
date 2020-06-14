package com.catthoor.TipOfTheSpearBot;

import com.catthoor.TipOfTheSpearBot.commands.*;
import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Message;

import java.util.Map;
import java.util.Optional;

public class CommandHandler {

    private final Map<CommandKey, Command> router = Map.ofEntries(
            Map.entry(new CommandKey("!pc", false), new PlayerCountCommand()),
            Map.entry(new CommandKey("!announcement", true), new AnnouncementCommand()),
            Map.entry(new CommandKey("!auth", true), new AuthCommand()),
            Map.entry(new CommandKey("!server", true), new ServerCommand()),
            Map.entry(new CommandKey("!servers", false), new ServersCommand()));

    public void performOnLaunchCommandActions(DiscordClient client) {
        router.values().forEach(command -> {
            if (command instanceof OnLaunchAction)
                ((OnLaunchAction) command).onLaunch(client);
        });
    }

    private Optional<Command> getCommand(String content) {
        return router.entrySet().stream().filter(entry -> {
            CommandKey commandKey = entry.getKey();
            return content.split(" ")[0].equals(commandKey.getKey());
        }).map(Map.Entry::getValue).findFirst();
    }

    public void handle(Message message) {
        final String content = message.getContent();

        Optional<Command> optionalCommand = getCommand(content);

        optionalCommand.ifPresent((command) -> {
            message.getAuthor().ifPresent(user ->
                    System.out.println("User [" + user.getTag() + "] - " + content));

            command.execute(message);
        });
    }
}

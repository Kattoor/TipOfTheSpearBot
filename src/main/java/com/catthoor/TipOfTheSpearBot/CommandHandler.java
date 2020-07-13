package com.catthoor.TipOfTheSpearBot;

import com.catthoor.TipOfTheSpearBot.commands.*;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.gateway.GatewayClient;

import java.util.Map;
import java.util.Optional;

public class CommandHandler {

    private final Map<CommandKey, Command> router = Map.ofEntries(
            Map.entry(new CommandKey("!pc", false), new PlayerCountCommand()),
            Map.entry(new CommandKey("!announcement", true), new AnnouncementCommand()),
            Map.entry(new CommandKey("!rooms", false), new RoomsCommand()),
            Map.entry(new CommandKey("!commands", false), new HelpCommand()),
            Map.entry(new CommandKey("!liveserverlist", false), new LiveServerListCommand())
            /*Map.entry(new CommandKey("!room", true), new RoomCommand())*/);

    public void performOnLaunchCommandActions(DiscordClient client, GatewayDiscordClient gateway) {
        router.values().forEach(command -> {
            if (command instanceof OnLaunchAction)
                ((OnLaunchAction) command).onLaunch(client, gateway);
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

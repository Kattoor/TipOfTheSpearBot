package com.catthoor.TipOfTheSpearBot;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;

public class Main {

    public static void main(String[] args) {
        new Main();
    }

    private final CommandHandler commandHandler = new CommandHandler();

    private Main() {
        final String token = Config.getToken();
        final DiscordClient client = DiscordClient.create(token);

        final GatewayDiscordClient gateway = client.login().block();

        if (gateway != null) {
            gateway.on(ReadyEvent.class).subscribe(event -> {
                System.out.println("Ready");
                commandHandler.performOnLaunchCommandActions(client);
                initCommandHandler(gateway, event.getSelf());
            });

            gateway.onDisconnect().block();
        }
    }

    private void initCommandHandler(GatewayDiscordClient gateway, User self) {
        gateway.on(MessageCreateEvent.class).subscribe(event -> {
            final Message message = event.getMessage();
            final User author = message.getAuthor().orElseThrow();

            if (!author.getTag().equals(self.getTag()))
                commandHandler.handle(message);
        });
    }
}

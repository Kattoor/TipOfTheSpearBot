import commands.AnnouncementCommand;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.rest.entity.RestChannel;

import java.math.BigInteger;

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
                initCommandHandler(gateway, event.getSelf());
            });

            initAnnouncements(client);

            gateway.onDisconnect().block();
        }
    }

    // todo: make this more flexible, extract from this class
    private void initAnnouncements(DiscordClient client) {
        RestChannel generalChannel = client.getChannelById(Snowflake.of(new BigInteger("621918100949827594")));

        AnnouncementCommand.loadAnnouncements(generalChannel);
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

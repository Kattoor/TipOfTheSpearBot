package com.catthoor.TipOfTheSpearBot.commands;

import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import java.util.function.Consumer;

public class HelpCommand implements Command {

    private Consumer<EmbedCreateSpec> getEmbedBuilder() {
        return builder -> {
            builder.setColor(Color.GREEN);
            builder.setTitle("Available Commands");
            builder.setThumbnail("https://content.invisioncic.com/f299184/monthly_2020_05/Logo_Force_TSTFE.png.ce5720e9c45f10b2776bd2e38d5e7e36.png");

            builder.addField("!pc", "Amount of online players", false);
            builder.addField("!rooms", "Info about all integrated TFE rooms (integrate your server via http://185.183.182.44/#/webadmin)", false);
        };
    }

    @Override
    public void execute(Message message) {
        message.getChannel().subscribe(messageChannel ->
                messageChannel.createEmbed(getEmbedBuilder()).block());
    }
}

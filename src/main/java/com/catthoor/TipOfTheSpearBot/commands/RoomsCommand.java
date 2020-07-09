package com.catthoor.TipOfTheSpearBot.commands;

import com.catthoor.TipOfTheSpearBot.Main;
import com.catthoor.TipOfTheSpearBot.model.Room;
import com.catthoor.TipOfTheSpearBot.model.Rooms;
import com.catthoor.TipOfTheSpearBot.utilities.TfeServerCredentials;
import com.catthoor.TipOfTheSpearBot.utilities.GameModeUtil;
import com.catthoor.TipOfTheSpearBot.utilities.TfeServerCredentialsUtil;
import com.google.gson.Gson;
import discord4j.core.object.Embed;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class RoomsCommand implements Command {

    private Message sentEmbed = null;

    private Consumer<EmbedCreateSpec> getEmbedBuilder(Consumer<EmbedCreateSpec> optionalExtraBuild) {
        return builder -> {
            builder.setColor(Color.GREEN);
            builder.setTitle("Rooms");
            builder.setThumbnail("https://content.invisioncic.com/f299184/monthly_2020_05/Logo_Force_TSTFE.png.ce5720e9c45f10b2776bd2e38d5e7e36.png");

            if (optionalExtraBuild == null)
                builder.addField("No rooms", "\u200b", false);
            else
                optionalExtraBuild.accept(builder);
        };
    }


    @Override
    public void execute(Message message) {
        List<TfeServerCredentials> tfeServerCredentials = TfeServerCredentialsUtil.getDiscordCredentials();
        List<CompletableFuture<List<Room>>> roomsFutures = getRooms(tfeServerCredentials);

        List<Room> rooms = new ArrayList<>();

        message.getChannel().subscribe(messageChannel ->
                messageChannel.createEmbed(getEmbedBuilder(null)).blockOptional().ifPresent(e -> sentEmbed = e));

        roomsFutures.forEach(future ->
                future.thenAccept(roomsOfSameServer -> {
                    if (roomsOfSameServer.size() > 0) {
                        rooms.addAll(roomsOfSameServer);
                        sendEmbed(rooms);

                        removePreviousRoomsPosts(message);
                    }
                }));
    }

    private void sendEmbed(List<Room> rooms) {
        if (sentEmbed != null) {
            sentEmbed.edit(spec -> spec.setEmbed(getEmbedBuilder(builder -> {
                StringBuilder stringBuilder = new StringBuilder();

                for (Room room : rooms) {
                    int amountOfPlayers = room.getBlueTeam().size() + room.getRedTeam().size();

                    stringBuilder
                            .append("`")
                            .append(room.getRoomName())
                            .append(", ")
                            .append(amountOfPlayers)
                            .append(", ")
                            .append(GameModeUtil.getGameMode(room.getGameMode()))
                            .append(", ")
                            .append(room.getMap())
                            .append("`")
                            .append("\n");
                }

                builder.setFooter("!commands - lists all available commands", null);

                builder.addField("Name, players, mode, map", stringBuilder.toString(), false);
            }))).block();
        }
    }

    private List<CompletableFuture<List<Room>>> getRooms(List<TfeServerCredentials> credentials) {
        HttpClient client = HttpClient.newHttpClient();
        List<CompletableFuture<List<Room>>> rooms = new ArrayList<>();

        for (TfeServerCredentials credential : credentials) {
            String url = "http://185.183.182.44/api/canlogin?ip=" + credential.getIp() + "&username=" + credential.getName() + "&password=" + credential.getPassword();
            System.out.println(url);
            HttpRequest requestToken = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(3))
                    .build();

            CompletableFuture<List<Room>> listCompletableFuture = CompletableFuture.supplyAsync(() -> {
                List<Room> roomsList = new ArrayList<>();
                try {
                    String token = client.send(requestToken, HttpResponse.BodyHandlers.ofString()).body();

                    System.out.println(token);

                    HttpRequest requestRooms = HttpRequest.newBuilder()
                            .uri(URI.create("http://185.183.182.44/api/getrooms"))
                            .header("token", token)
                            .timeout(Duration.ofSeconds(3))
                            .build();

                    String body = client.send(requestRooms, HttpResponse.BodyHandlers.ofString()).body();

                    System.out.println(body);

                    roomsList = new Gson().fromJson(body, Rooms.class).getRooms();
                } catch (HttpTimeoutException e) {
                    System.out.println("Timeout for server " + credential.getIp());
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                return roomsList;
            });

            rooms.add(listCompletableFuture);
        }

        return rooms;
    }

    private void removePreviousRoomsPosts(Message message) {
        message.getChannel().subscribe(messageChannel -> {
            Optional<Message> optionalLastMessage = messageChannel.getLastMessage().blockOptional();
            if (optionalLastMessage.isEmpty())
                return;
            Message lastMessage = optionalLastMessage.get();

            Optional<List<Message>> optionalPreviousBotMessages = messageChannel.getMessagesBefore(lastMessage.getId())
                    .take(20)
                    .filter(this::isBotMessage)
                    .collectList().blockOptional();
            if (optionalPreviousBotMessages.isEmpty())
                return;
            List<Message> previousBotMessages = optionalPreviousBotMessages.get();

            previousBotMessages.forEach(previousMessage -> {
                List<Embed> embeds = previousMessage.getEmbeds();
                embeds.forEach(embed -> {
                    embed.getTitle().ifPresent(title -> {
                        if (title.equals("Rooms")) {
                            previousMessage.delete().block();
                        }
                    });
                });
            });
        });
    }

    private boolean isBotMessage(Message m) {
        if (m.getAuthor().isEmpty())
            return false;
        String author = m.getAuthor().get().getTag();
        return author.equals(Main.botTag);
    }
}

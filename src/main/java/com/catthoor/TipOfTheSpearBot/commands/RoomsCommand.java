package com.catthoor.TipOfTheSpearBot.commands;

import com.catthoor.TipOfTheSpearBot.model.Room;
import com.catthoor.TipOfTheSpearBot.model.Rooms;
import com.catthoor.TipOfTheSpearBot.utilities.GameModeUtil;
import com.catthoor.TipOfTheSpearBot.utilities.SideCarUtil;
import com.google.gson.Gson;
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
        List<IpAuthKeyAndServerName> servers = SideCarUtil.getServers(SideCarUtil.getAuthList());
        List<CompletableFuture<List<Room>>> roomsFutures = getRooms(servers);
        List<Room> rooms = new ArrayList<>();

        message.getChannel().subscribe(messageChannel ->
                messageChannel.createEmbed(getEmbedBuilder(null)).blockOptional().ifPresent(e -> sentEmbed = e));

        roomsFutures.forEach(future ->
                future.thenAccept(roomsOfSameServer -> {
                    if (roomsOfSameServer.size() > 0) {
                        rooms.addAll(roomsOfSameServer);
                        sendEmbed(rooms);
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

                builder.addField("Name, players, mode, map", stringBuilder.toString(), false);
            }))).block();
        }
    }

    private List<CompletableFuture<List<Room>>> getRooms(List<IpAuthKeyAndServerName> servers) {
        HttpClient client = HttpClient.newHttpClient();
        List<CompletableFuture<List<Room>>> rooms = new ArrayList<>();

        for (IpAuthKeyAndServerName server : servers) {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + server.getSidecarIp() + ":8080/server"))
                    .header("authKey", server.getAuthKey())
                    .timeout(Duration.ofSeconds(3))
                    .build();


            CompletableFuture<List<Room>> listCompletableFuture = CompletableFuture.supplyAsync(() -> {
                List<Room> roomsList = new ArrayList<>();
                try {
                    String body = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
                    roomsList = new Gson().fromJson(body, Rooms.class).getRooms();
                } catch (HttpTimeoutException e) {
                    System.out.println("Timeout for server " + server.getServerName());
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                return roomsList;
            });

            rooms.add(listCompletableFuture);
        }

        return rooms;
    }
}

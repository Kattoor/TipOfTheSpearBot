package com.catthoor.TipOfTheSpearBot.commands;

import com.catthoor.TipOfTheSpearBot.model.PlayerInfo;
import com.catthoor.TipOfTheSpearBot.model.Room;
import com.catthoor.TipOfTheSpearBot.model.Rooms;
import com.catthoor.TipOfTheSpearBot.utilities.GameModeUtil;
import com.catthoor.TipOfTheSpearBot.utilities.SideCarUtil;
import com.google.gson.Gson;
import discord4j.core.object.entity.Message;
import discord4j.rest.util.Color;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RoomCommand implements Command {

    @Override
    public void execute(Message message) {
        message.getChannel().subscribe(messageChannel -> {
            final String content = message.getContent();
            final String[] parts = content.split(" ");

            if (parts.length < 2) {
                messageChannel.createMessage("Usage: !server {serverName}").block();
                return;
            }

            String roomName = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));

            List<IpAuthKeyAndServerName> servers = SideCarUtil.getAliveServers(SideCarUtil.getAuthList());
            List<Room> rooms = getRooms(servers);
            Optional<Room> optionalRoom = rooms.stream().filter(r -> r.getRoomName().equalsIgnoreCase(roomName)).findFirst();

            optionalRoom.ifPresent(room -> {
                List<PlayerInfo> blueTeam = room.getBlueTeam();
                List<PlayerInfo> redTeam = room.getRedTeam();

                messageChannel.createEmbed(builder -> {
                    builder.setColor(Color.GREEN);
                    builder.setTitle("Room " + room.getRoomName());
                    builder.setThumbnail("https://content.invisioncic.com/f299184/monthly_2020_05/Logo_Force_TSTFE.png.ce5720e9c45f10b2776bd2e38d5e7e36.png");

                    if (blueTeam.size() == 0 && redTeam.size() == 0) {
                        builder.addField("Server is empty", "\u200b", false);
                    } else {
                        builder.addField(":taskforceelite: Task Force Elite", blueTeam.size() > 0 ? blueTeam.stream().map(playerInfo -> "`" + playerInfo.getDisplayName() + "`").collect(Collectors.joining("\n")) : "\u200b", true);
                        builder.addField(":redspear: Red Spear", redTeam.size() > 0 ? redTeam.stream().map(playerInfo -> "`" + playerInfo.getDisplayName() + "`").collect(Collectors.joining("\n")) : "\u200b", true);
                    }

                    builder.addField(room.getMap() + ", " + GameModeUtil.getGameMode(room.getGameMode()) + ", " + (room.getNumOfBots() == 0 ? "no" : room.getNumOfBots()) + " bots", "\u200b", false);
                }).block();
            });
        });
    }

    private List<Room> getRooms(List<IpAuthKeyAndServerName> servers) {
        HttpClient client = HttpClient.newHttpClient();
        List<Room> rooms = new ArrayList<>();

        for (IpAuthKeyAndServerName server : servers) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + server.getSidecarIp() + ":8080/server"))
                    .header("authKey", server.getAuthKey())
                    .build();

            try {
                String body = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
                rooms.addAll(new Gson().fromJson(body, Rooms.class).getRooms());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        return rooms;
    }
}

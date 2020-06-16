package com.catthoor.TipOfTheSpearBot.commands;

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
import java.util.List;

public class RoomsCommand implements Command {

    @Override
    public void execute(Message message) {
        List<IpAuthKeyAndServerName> servers = SideCarUtil.getAliveServers(SideCarUtil.getAuthList());
        List<Room> rooms = getRooms(servers);

        message.getChannel().subscribe(messageChannel -> {
            messageChannel.createEmbed(builder -> {
                builder.setColor(Color.GREEN);
                builder.setTitle("Rooms");
                builder.setThumbnail("https://content.invisioncic.com/f299184/monthly_2020_05/Logo_Force_TSTFE.png.ce5720e9c45f10b2776bd2e38d5e7e36.png");

                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < rooms.size(); i++) {
                    Room room = rooms.get(i);
                    int amountOfPlayers = room.getBlueTeam().size() + room.getRedTeam().size();

                    stringBuilder
                            .append("`")
                            .append(i)
                            .append(", ")
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

                builder.addField("Id, name, players, mode, map", stringBuilder.toString(), false);
            }).block();
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

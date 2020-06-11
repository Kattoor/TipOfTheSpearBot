package com.catthoor.TipOfTheSpearBot.commands;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class IpAndAuthKey {
    private String sidecarIp;
    private String authKey;

    public IpAndAuthKey(String sidecarIp, String authKey) {
        this.sidecarIp = sidecarIp;
        this.authKey = authKey;
    }

    public String getSidecarIp() {
        return sidecarIp;
    }

    public String getAuthKey() {
        return authKey;
    }
}

public class ServerCommand implements Command {

    private final Path filePath = Paths.get("sidecar-auth.json");
    private final JSONParser parser = new JSONParser();

    @Override
    public void execute(Message message) {
        final String content = message.getContent();

        final String[] parts = content.split(" ");

        if (parts.length != 2)
            return;

        String serverName = parts[1];

        JSONArray authList = (JSONArray) getRoot().get("authList");
        IpAndAuthKey server = findServerByName(authList, serverName);

        message.getChannel().subscribe(messageChannel -> {
            if (server == null) {
                messageChannel.createMessage("Couldn't find server: " + serverName).block();
            } else {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://" + server.getSidecarIp() + ":8080/server"))
                        .header("authKey", server.getAuthKey())
                        .build();
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenApply(HttpResponse::body)
                        .thenAccept(data -> sendEmbed(data, serverName, messageChannel));
            }
        });
    }

    private JSONObject getRoot() {
        try {
            String fileData = Files.exists(filePath)
                    ? Files.readString(filePath)
                    : "{\"authList\": []}";
            return (JSONObject) parser.parse(fileData);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            return new JSONObject(Map.of("authList", new JSONArray()));
        }
    }

    private IpAndAuthKey findServerByName(JSONArray authList, String serverName) {
        Iterator iterator = authList.iterator();

        IpAndAuthKey result = null;
        boolean shortCircuit = false;
        while (iterator.hasNext() && !shortCircuit) {
            JSONObject o = (JSONObject) iterator.next();
            if (o.get("serverName").equals(serverName)) {
                shortCircuit = true;
                result = new IpAndAuthKey((String) o.get("sidecarIp"), (String) o.get("authKey"));
            }
        }

        return result;
    }

    private void sendEmbed(String data, String serverName, MessageChannel messageChannel) {
        try {
            System.out.println(data);
            JSONArray rooms = (JSONArray) ((JSONObject) parser.parse(data)).get("rooms");
            messageChannel.createMessage("This server has " + rooms.size() + " room(s)!").block();
            rooms.forEach(room -> {
                JSONObject obj = (JSONObject) room;
                long gameMode = (long) obj.get("gameMode");
                String region = (String) obj.get("region");
                String sessionType = (String) obj.get("sessionType");
                String map = (String) obj.get("map");
                String roomName = (String) obj.get("roomName");
                List<String> blueTeam = getPlayerNames((JSONArray) obj.get("blueTeam"));
                List<String> redTeam = getPlayerNames((JSONArray) obj.get("redTeam"));
                List<String> mapRotation = getMapRotation((JSONArray) obj.get("mapRotation"));
                long maxPlayer = (long) obj.get("maxPlayer");
                long gameLength = (long) obj.get("gameLength");
                long numOfBots = (long) obj.get("numOfBots");


                messageChannel.createEmbed(builder -> {
                    builder.setColor(Color.GREEN);
                    builder.setTitle("[" + serverName + "] " + roomName);
                    builder.setThumbnail("https://content.invisioncic.com/f299184/monthly_2020_05/Logo_Force_TSTFE.png.ce5720e9c45f10b2776bd2e38d5e7e36.png");
                    builder.addField("Current map", map, false);
                    builder.addField("Amount of bots", String.valueOf(numOfBots), false);
                    builder.addField("Blue team", blueTeam.size() > 0 ? String.join(", ", blueTeam) : "empty", false);
                    builder.addField("Red team", blueTeam.size() > 0 ? String.join(", ", redTeam) : "empty", false);
                }).block();
                System.out.println("sending embed7");
            });
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private List<String> getPlayerNames(JSONArray team) {
        List<String> playerNames = new ArrayList<>();

        team.forEach(player -> {
            JSONObject obj = (JSONObject) player;
            playerNames.add((String) obj.get("displayName"));
        });

        return playerNames;
    }

    private List<String> getMapRotation(JSONArray mapRotation) {
        List<String> maps = new ArrayList<>();

        mapRotation.forEach(mapName -> maps.add((String) mapName));

        return maps;
    }
}

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
import java.util.stream.Collectors;

class IpAuthKeyAndServerName {
    private final String sidecarIp;
    private final String authKey;
    private final String serverName;

    public IpAuthKeyAndServerName(String sidecarIp, String authKey, String serverName) {
        this.sidecarIp = sidecarIp;
        this.authKey = authKey;
        this.serverName = serverName;
    }

    public String getSidecarIp() {
        return sidecarIp;
    }

    public String getAuthKey() {
        return authKey;
    }

    public String getServerName() {
        return serverName;
    }
}

public class ServerCommand implements Command {

    private final Path filePath = Paths.get("../sidecar-auth.json");
    private final JSONParser parser = new JSONParser();

    @Override
    public void execute(Message message) {
        final String content = message.getContent();

        final String[] parts = content.split(" ");

        message.getChannel().subscribe(messageChannel -> {
            if (parts.length != 2) {
                messageChannel.createMessage("Usage: ?server {serverName or index}");
                return;
            }

            String serverName = parts[1];

            JSONArray authList = (JSONArray) getRoot().get("authList");

            IpAuthKeyAndServerName server;

            if (serverName.length() < 3 && isNumeric(serverName))
                server = findServerByListId(authList, Integer.parseInt(serverName));
            else
                server = findServerByName(authList, serverName);

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
                        .thenAccept(data -> sendEmbed(data, server.getServerName(), messageChannel));
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

    private IpAuthKeyAndServerName findServerByName(JSONArray authList, String serverName) {
        Iterator iterator = authList.iterator();

        IpAuthKeyAndServerName result = null;
        boolean shortCircuit = false;
        while (iterator.hasNext() && !shortCircuit) {
            JSONObject o = (JSONObject) iterator.next();
            if (o.get("serverName").equals(serverName)) {
                shortCircuit = true;
                result = new IpAuthKeyAndServerName((String) o.get("sidecarIp"), (String) o.get("authKey"), (String) o.get("serverName"));
            }
        }

        return result;
    }

    private IpAuthKeyAndServerName findServerByListId(JSONArray authList, int listId) {
        Iterator iterator = authList.iterator();

        int i = 0;

        while (iterator.hasNext()) {
            if (i++ == listId) {
                JSONObject o = (JSONObject) iterator.next();
                return new IpAuthKeyAndServerName((String) o.get("sidecarIp"), (String) o.get("authKey"), (String) o.get("serverName"));
            }
        }

        return null;
    }

    private void sendEmbed(String data, String serverName, MessageChannel messageChannel) {
        try {
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

                    if (blueTeam.size() == 0 && redTeam.size() == 0) {
                        builder.addField("Server is empty", "\u200b", false);
                    } else {
                        builder.addField(":blue_circle: Task Force Elite", blueTeam.size() > 0 ? blueTeam.stream().map(name -> "`" + name + "`").collect(Collectors.joining("\n")) : "\u200b", true);
                        builder.addField(":red_circle: Red Spear", redTeam.size() > 0 ? redTeam.stream().map(name -> "`" + name + "`").collect(Collectors.joining("\n")) : "\u200b", true);
                    }

                    builder.addField(map + ", " + getGameMode(gameMode) + ", " + (numOfBots == 0 ? "no" : numOfBots) + " bots", "\u200b", false);
                }).block();
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

    private boolean isNumeric(String str) {
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }

    private String getGameMode(long gameMode) {
        switch ((int) gameMode) {
            case 0:
                return "Deathmatch";
            case 1:
                return "Team Deathmatch";
            case 2:
                return "Team King of the Hill";
            case 3:
                return "Capture the Flag";
            default:
                return "Unknown";
        }
    }
}

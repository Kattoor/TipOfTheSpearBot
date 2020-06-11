package com.catthoor.TipOfTheSpearBot.commands;

import discord4j.core.object.entity.Message;
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
import java.util.Iterator;
import java.util.Map;

public class AuthCommand implements Command {

    private final Path filePath = Paths.get("sidecar-auth.json");
    private final JSONParser parser = new JSONParser();

    @Override
    public void execute(Message message) {
        final String content = message.getContent();

        final String[] parts = content.split(" ");

        if (parts.length != 4)
            return;

        String sidecarIp = parts[1];
        String authKey = parts[2];
        String serverName = parts[3];

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + sidecarIp + ":8080/ping"))
                .header("authKey", authKey)
                .build();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(ok -> {
                    message.getAuthor().ifPresent(author -> addAuthRecord(author.getTag(), authKey, serverName, sidecarIp, message));
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


    private void addAuthRecord(String userTag, String authKey, String serverName, String sidecarIp, Message message) {
        message.getChannel().subscribe(messageChannel -> {
            JSONObject root = getRoot();
            JSONArray authList = (JSONArray) root.get("authList");

            deleteIfAlreadyExists(authList, userTag, message);

            authList.add(new JSONObject(
                    Map.ofEntries(
                            Map.entry("userTag", userTag),
                            Map.entry("authKey", authKey),
                            Map.entry("serverName", serverName),
                            Map.entry("sidecarIp", sidecarIp))));

            messageChannel.createMessage("Registered authKey for user " + userTag).block();

            write(root);
        });
    }

    private void deleteIfAlreadyExists(JSONArray authList, String userTag, Message message) {
        message.getChannel().subscribe(messageChannel -> {
            Iterator iterator = authList.iterator();

            int index = -1;
            int i = 0;
            boolean shortCircuit = false;
            while (iterator.hasNext() && !shortCircuit) {
                JSONObject o = (JSONObject) iterator.next();
                if (o.get("userTag").equals(userTag)) {
                    shortCircuit = true;
                    index = i;
                } else i++;
            }

            if (index >= 0) {
                authList.remove(index);
                messageChannel.createMessage("Deleted authKey for user " + userTag).block();
            }
        });
    }

    private void write(JSONObject root) {
        try {
            Files.writeString(filePath, root.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

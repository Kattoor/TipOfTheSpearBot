package com.catthoor.TipOfTheSpearBot.commands;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
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
        message.getChannel().subscribe(messageChannel -> {
            final String content = message.getContent();

            final String[] parts = content.split(" ");

            if (parts.length != 4) {
                messageChannel.createMessage("Usage: ?auth {sidecarIp} {authenticationKey} {serverName}").block();
                return;
            }

            String sidecarIp = parts[1];
            String authKey = parts[2];
            String serverName = parts[3];

            if (serverName.length() < 3) {
                messageChannel.createMessage("Server name has to be > 2 characters long").block();
                return;
            }

            message.getAuthor().ifPresent(user -> {
                if (serverNameAlreadyExists(serverName) && !serverRegisteredUnderUser(serverName, user.getTag())) {
                    messageChannel.createMessage("Server name '" + serverName + "' is already in use").block();
                } else {
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("http://" + sidecarIp + ":8080/ping"))
                            .header("authKey", authKey)
                            .build();
                    client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                            .thenApply(HttpResponse::body)
                            .thenAccept(ok -> message.getAuthor().ifPresent(author -> addAuthRecord(author.getTag(), authKey, serverName, sidecarIp, messageChannel)));
                }
            });
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


    private void addAuthRecord(String userTag, String authKey, String serverName, String sidecarIp, MessageChannel messageChannel) {
        JSONObject root = getRoot();
        JSONArray authList = (JSONArray) root.get("authList");

        deleteIfAlreadyExists(authList, userTag, messageChannel);

        authList.add(new JSONObject(
                Map.ofEntries(
                        Map.entry("userTag", userTag),
                        Map.entry("authKey", authKey),
                        Map.entry("serverName", serverName),
                        Map.entry("sidecarIp", sidecarIp))));

        messageChannel.createMessage("Registered authKey for user " + userTag).block();

        write(root);
    }

    /* Only one record per userTag */
    private void deleteIfAlreadyExists(JSONArray authList, String userTag, MessageChannel messageChannel) {
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
    }

    private boolean serverNameAlreadyExists(String serverName) {
        JSONObject root = getRoot();
        JSONArray authList = (JSONArray) root.get("authList");

        Iterator iterator = authList.iterator();
        int index = -1;
        int i = 0;

        boolean shortCircuit = false;
        while (iterator.hasNext() && !shortCircuit) {
            JSONObject o = (JSONObject) iterator.next();
            if (o.get("serverName").equals(serverName)) {
                shortCircuit = true;
                index = i;
            } else i++;
        }

        return index >= 0;
    }

    private boolean serverRegisteredUnderUser(String serverName, String userTag) {
        JSONObject root = getRoot();
        JSONArray authList = (JSONArray) root.get("authList");

        for (Object value : authList) {
            JSONObject o = (JSONObject) value;
            if (o.get("serverName").equals(serverName) && o.get("userTag").equals(userTag))
                return true;
        }

        return false;
    }

    private void write(JSONObject root) {
        try {
            Files.writeString(filePath, root.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

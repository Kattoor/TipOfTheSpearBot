package com.catthoor.TipOfTheSpearBot.commands;

import discord4j.core.object.entity.Message;
import discord4j.rest.util.Color;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ServersCommand implements Command {

    private final Path filePath = Paths.get("sidecar-auth.json");
    private final JSONParser parser = new JSONParser();

    @Override
    public void execute(Message message) {
        JSONArray authList = (JSONArray) getRoot().get("authList");
        List<String> serverNames = getServers(authList);

        message.getChannel().subscribe(messageChannel -> {
            messageChannel.createEmbed(builder -> {
                builder.setColor(Color.GREEN);
                builder.setTitle("Servers");
                builder.setThumbnail("https://content.invisioncic.com/f299184/monthly_2020_05/Logo_Force_TSTFE.png.ce5720e9c45f10b2776bd2e38d5e7e36.png");
                for (int i = 0; i < serverNames.size(); i++) {
                    String serverName = serverNames.get(i);
                    builder.addField("Id", String.valueOf(i), true);
                    builder.addField("Name", serverName, true);
                    builder.addField(String.valueOf('\u200B'), String.valueOf('\u200B'), false);
                }
            }).block();
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

    private List<String> getServers(JSONArray authList) {
        Iterator iterator = authList.iterator();

        List<String> serverNames = new ArrayList<>();

        while (iterator.hasNext()) {
            JSONObject o = (JSONObject) iterator.next();
            serverNames.add((String) o.get("serverName"));
        }

        return serverNames;
    }
}

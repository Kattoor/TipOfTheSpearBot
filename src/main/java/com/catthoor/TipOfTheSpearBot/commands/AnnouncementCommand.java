package com.catthoor.TipOfTheSpearBot.commands;

import com.catthoor.TipOfTheSpearBot.Config;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.Color;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AnnouncementCommand implements Command, OnLaunchAction {

    public final Map<Integer, ScheduledFuture<?>> scheduledFutures = new HashMap<>();

    private final Path filePath = Paths.get("announcements.json");
    private final JSONParser parser = new JSONParser();

    private void loadAnnouncements(RestChannel channel) {
        JSONArray announcementsArray = (JSONArray) getRoot().get("announcements");
        for (Object o : announcementsArray) {
            JSONObject announcement = (JSONObject) o;

            String text = (String) announcement.get("text");
            long interval = (long) announcement.get("interval");
            long epochStartInSeconds = ((long) announcement.get("startTime")) / 1000;
            long currentTimeInSeconds = System.currentTimeMillis() / 1000;
            long deltaTime = currentTimeInSeconds - epochStartInSeconds;

            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
            ScheduledFuture<?> scheduledFuture =
                    executor.scheduleAtFixedRate(
                            channel.createMessage(MessageCreateRequest.builder().content(text).build())::block,
                            interval - (deltaTime % interval), interval, TimeUnit.SECONDS);

            scheduledFutures.put(announcementsArray.size() - 1, scheduledFuture);
        }
    }

    public void execute(Message message) {
        final String content = message.getContent();

        final String[] parts = content.split(" ");

        if (parts.length < 2)
            return;

        // todo: move author limitation to com.catthoor.TipOfTheSpearBot.CommandHandler, into the router dictionary
        if (message.getAuthor().isEmpty() || !message.getAuthor().get().getTag().equals(Config.getAnnouncementsUser()))
            return;

        String action = parts[1];
        switch (action) {
            case "list":
                listAnnouncements(message);
                break;
            case "create":
                if (parts.length < 4 || !isNumeric(parts[2]))
                    return;

                int interval = Integer.parseInt(parts[2]);
                String text = String.join(" ", Arrays.copyOfRange(parts, 3, parts.length));
                createAnnouncement(interval, text, message);
                break;
            case "delete":
                if (parts.length < 3 || !isNumeric(parts[2]))
                    return;

                int id = Integer.parseInt(parts[2]);
                deleteAnnouncement(id, message);
                break;
        }
    }

    private boolean isNumeric(String str) {
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }

    private JSONObject getRoot() {
        try {
            String fileData = Files.exists(filePath)
                    ? Files.readString(filePath)
                    : "{\"announcements\": []}";
            return (JSONObject) parser.parse(fileData);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            return new JSONObject(Map.of("announcements", new JSONArray()));
        }
    }

    private void listAnnouncements(Message message) {
        message.getChannel().subscribe(messageChannel -> {

            JSONArray announcementsArray = (JSONArray) getRoot().get("announcements");
            if (announcementsArray.size() == 0) {
                messageChannel.createMessage("There are no announcements!").block();
            } else {
                messageChannel.createEmbed(builder -> {
                    builder.setColor(Color.GREEN);
                    builder.setTitle("Announcements");
                    builder.setThumbnail("https://content.invisioncic.com/f299184/monthly_2020_05/Logo_Force_TSTFE.png.ce5720e9c45f10b2776bd2e38d5e7e36.png");

                    for (Object o : announcementsArray) {
                        JSONObject obj = (JSONObject) o;
                        builder.addField("ID", String.valueOf(obj.get("id")), true);
                        builder.addField("Text", String.valueOf(obj.get("text")), true);
                        builder.addField(String.valueOf('\u200B'), String.valueOf('\u200B'), false);
                    }
                }).block();
            }
        });
    }

    private void createAnnouncement(int interval, String text, Message message) {
        message.getChannel().subscribe(messageChannel -> {
            JSONObject root = getRoot();
            JSONArray announcementsArray = (JSONArray) root.get("announcements");

            announcementsArray.add(new JSONObject(
                    Map.ofEntries(
                            Map.entry("id", announcementsArray.size()),
                            Map.entry("interval", interval),
                            Map.entry("text", text),
                            Map.entry("startTime", System.currentTimeMillis()))));

            messageChannel.createMessage("New announcement added! ID: " + (announcementsArray.size() - 1)).block();

            write(root);

            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
            ScheduledFuture<?> scheduledFuture = executor.scheduleAtFixedRate(() -> {
                messageChannel.createMessage(text).block();
            }, interval, interval, TimeUnit.SECONDS);

            scheduledFutures.put(announcementsArray.size() - 1, scheduledFuture);
        });
    }

    private void deleteAnnouncement(int id, Message message) {
        message.getChannel().subscribe(messageChannel -> {
            JSONObject root = getRoot();
            JSONArray announcementsArray = (JSONArray) root.get("announcements");
            Iterator iterator = announcementsArray.iterator();

            int index = -1;
            int i = 0;
            boolean shortCircuit = false;
            while (iterator.hasNext() && !shortCircuit) {
                JSONObject o = (JSONObject) iterator.next();
                if ((long) o.get("id") == id) {
                    shortCircuit = true;
                    index = i;
                } else i++;
            }

            if (index >= 0) {
                announcementsArray.remove(index);
                if (scheduledFutures.containsKey(index)) {
                    scheduledFutures.get(index).cancel(true);
                    scheduledFutures.remove(index);
                }
                messageChannel.createMessage("Successfully deleted announcement[ID: " + id + "]").block();
            } else {
                messageChannel.createMessage("No announcement with ID: " + id);
            }

            write(root);
        });
    }

    private void write(JSONObject root) {
        try {
            Files.writeString(filePath, root.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLaunch(DiscordClient client) {
        RestChannel generalChannel = client.getChannelById(Snowflake.of(new BigInteger(Config.getGeneralChannelId())));
        loadAnnouncements(generalChannel);
    }
}

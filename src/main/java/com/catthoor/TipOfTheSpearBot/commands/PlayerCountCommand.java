package com.catthoor.TipOfTheSpearBot.commands;

import discord4j.core.object.entity.Message;
import discord4j.rest.util.Color;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class PlayerCountCommand implements Command {

    public void execute(Message message) {
        getJson()
                .thenApply(this::extractPlayerCount)
                .thenAccept(sendToDiscord(message));
    }

    private CompletableFuture<String> getJson() {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.steampowered.com/ISteamUserStats/GetNumberOfCurrentPlayers/v1?appid=1148810"))
                .build();

        httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(completableFuture::complete)
                .join();

        return completableFuture;
    }

    private long extractPlayerCount(String json) {
        JSONParser parser = new JSONParser();
        JSONObject root;

        try {
            root = (JSONObject) parser.parse(json);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        JSONObject response = (JSONObject) root.get("response");
        return (long) response.get("player_count");
    }

    private Consumer<Long> sendToDiscord(Message message) {
        return playerCount ->
                message.getChannel().subscribe(messageChannel ->
                        messageChannel.createEmbed(builder -> {
                            builder.setColor(Color.GREEN);
                            builder.setTitle("Player Count");
                            builder.setThumbnail("https://content.invisioncic.com/f299184/monthly_2020_05/Logo_Force_TSTFE.png.ce5720e9c45f10b2776bd2e38d5e7e36.png");
                            builder.addField("Amount of players", Long.toString(playerCount), true);
                        }).block());
    }
}

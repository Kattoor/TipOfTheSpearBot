package com.catthoor.TipOfTheSpearBot.commands;

import com.catthoor.TipOfTheSpearBot.Config;
import com.catthoor.TipOfTheSpearBot.model.Room;
import com.catthoor.TipOfTheSpearBot.model.Rooms;
import com.catthoor.TipOfTheSpearBot.utilities.GameModeUtil;
import com.catthoor.TipOfTheSpearBot.utilities.TfeServerCredentials;
import com.catthoor.TipOfTheSpearBot.utilities.TfeServerCredentialsUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class LiveServerListCommand implements Command, OnLaunchAction {

    private Map<String, String> countryCodeByIpCache = new HashMap<>();

    @Override
    public void execute(Message message) {

    }

    private final String liveServerListChannelId = Config.getLiveServerListChannelId();

    private Optional<MessageChannel> getChannel(GatewayDiscordClient gateway) {
        return gateway.getChannelById(Snowflake.of(liveServerListChannelId)).cast(MessageChannel.class).blockOptional();
    }

    private void getLastMessage(MessageChannel channel, Consumer<Message> consumer) {
        channel.getLastMessage().subscribe(consumer, error -> consumer.accept(null));
    }

    @Override
    public void onLaunch(DiscordClient client, GatewayDiscordClient gateway) {
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            getPlayerCountJson()
                    .thenApply(this::extractPlayerCount)
                    .thenAccept(playerCount -> {

                        getChannel(gateway).ifPresent(channel ->
                                getLastMessage(channel, message -> {
                                    if (message == null) {
                                        sendEmptyEmbed(channel).ifPresent(m -> updateEmbed(m, playerCount));
                                    } else {
                                        updateEmbed(message, playerCount);
                                    }
                                }));
                    });
        }, 0, 1, TimeUnit.MINUTES);
    }

    private CompletableFuture<String> getPlayerCountJson() {
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

    private Optional<Message> sendEmptyEmbed(MessageChannel channel) {
        return channel.createEmbed(builder -> {
        }).blockOptional();
    }

    private void updateEmbed(Message message, long playerCount) {
        List<CompletableFuture<List<Room>>> roomsFutures = getRoomsFutures();

        List<Room> rooms = new ArrayList<>();

        roomsFutures.forEach(future ->
                future.thenAccept(roomsOfSameServer -> {
                    if (roomsOfSameServer.size() > 0) {
                        rooms.addAll(roomsOfSameServer);
                        updateEmbed(message, rooms, playerCount);
                    }
                }));
    }

    private void updateEmbed(Message embed, List<Room> rooms, long playerCount) {
        if (embed != null) {
            embed.edit(spec -> spec.setEmbed(builder -> {

                builder.setTitle(":video_game: Live Server List :video_game:");

                builder.setDescription(":bust_in_silhouette: Total Players Online: " + playerCount);

                for (Room room : rooms) {

                    int amountOfPlayers = room.getBlueTeam().size() + room.getRedTeam().size();

                    String sb = "`Game Mode: " +
                            GameModeUtil.getGameMode(room.getGameMode()) +
                            "\n" +
                            "Current Map: " +
                            room.getMap() +
                            "\n" +
                            "Players: " +
                            amountOfPlayers +
                            "\n`";
                    builder.addField(":flag_" + getCountryCode(room.getDsc()).toLowerCase() + ": " + room.getRoomName(), sb, false);
                }

                int playersInNonBotServer = (int) playerCount - rooms.stream().map(room -> room.getBlueTeam().size() + room.getRedTeam().size()).reduce(0, Integer::sum);
                builder.setFooter(playersInNonBotServer + " players in servers without bot support", null);
            })).block();
        }
    }

    private String getCountryCode(String dsc) {
        HttpClient httpClient = HttpClient.newHttpClient();

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://api.ipstack.com/" + dsc + "?access_key=59e8068796a52ac5dfcc61c403c8759a"))
                .build();

        try {
            String body = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString()).body();
            JsonObject jsonObject = new Gson().fromJson(body, JsonObject.class);

            return jsonObject.get("country_code").getAsString();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    private String getFlagUrlByIp(String dsc) throws IOException {
        HttpClient httpClient = HttpClient.newHttpClient();

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://api.ipstack.com/" + dsc + "?access_key=59e8068796a52ac5dfcc61c403c8759a"))
                .build();

        try {
            String body = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString()).body();
            JsonObject jsonObject = new Gson().fromJson(body, JsonObject.class);
            String countryCode = jsonObject.get("country_code").getAsString();

            httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.countryflags.io/" + countryCode + "/flat/64.png"))
                    .build();
            body = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString()).body();

            return body;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return "";
    }

    private List<CompletableFuture<List<Room>>> getRoomsFutures() {
        List<TfeServerCredentials> tfeServerCredentials = TfeServerCredentialsUtil.getDiscordCredentials();
        return getRooms(tfeServerCredentials);
    }

    private List<CompletableFuture<List<Room>>> getRooms(List<TfeServerCredentials> credentials) {
        HttpClient client = HttpClient.newHttpClient();
        List<CompletableFuture<List<Room>>> rooms = new ArrayList<>();

        for (TfeServerCredentials credential : credentials) {
            String url = "http://185.183.182.44/api/canlogin?ip=" + credential.getIp() + "&username=" + credential.getName() + "&password=" + credential.getPassword();
            System.out.println(url);
            HttpRequest requestToken = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(3))
                    .build();

            CompletableFuture<List<Room>> listCompletableFuture = CompletableFuture.supplyAsync(() -> {
                List<Room> roomsList = new ArrayList<>();
                try {
                    String token = client.send(requestToken, HttpResponse.BodyHandlers.ofString()).body();

                    System.out.println(token);

                    HttpRequest requestRooms = HttpRequest.newBuilder()
                            .uri(URI.create("http://185.183.182.44/api/getrooms"))
                            .header("token", token)
                            .timeout(Duration.ofSeconds(3))
                            .build();

                    String body = client.send(requestRooms, HttpResponse.BodyHandlers.ofString()).body();

                    System.out.println(body);

                    roomsList = new Gson().fromJson(body, Rooms.class).getRooms();
                } catch (HttpTimeoutException e) {
                    System.out.println("Timeout for server " + credential.getIp());
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

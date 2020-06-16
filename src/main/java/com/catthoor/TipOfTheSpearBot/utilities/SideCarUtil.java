package com.catthoor.TipOfTheSpearBot.utilities;

import com.catthoor.TipOfTheSpearBot.commands.IpAuthKeyAndServerName;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SideCarUtil {

    private static final Path filePath = Paths.get("../sidecar-auth.json");
    private static final JSONParser parser = new JSONParser();

    public static JSONObject getRoot() {
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

    public static JSONArray getAuthList() {
        return getAuthList(getRoot());
    }

    public static JSONArray getAuthList(JSONObject root) {
        return (JSONArray) root.get("authList");
    }

    public static void write(JSONObject root) {
        try {
            Files.writeString(filePath, root.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<IpAuthKeyAndServerName> getServers(JSONArray authList) {
        Iterator iterator = authList.iterator();

        List<IpAuthKeyAndServerName> servers = new ArrayList<>();

        while (iterator.hasNext()) {
            JSONObject o = (JSONObject) iterator.next();
            IpAuthKeyAndServerName server = new IpAuthKeyAndServerName(
                    (String) o.get("sidecarIp"),
                    (String) o.get("authKey"),
                    (String) o.get("serverName"));
            servers.add(server);
        }

        return servers;
    }

    public static List<IpAuthKeyAndServerName> getAliveServers(JSONArray authList) {
        HttpClient client = HttpClient.newHttpClient();

        Iterator iterator = authList.iterator();

        List<IpAuthKeyAndServerName> servers = new ArrayList<>();

        while (iterator.hasNext()) {
            JSONObject o = (JSONObject) iterator.next();
            IpAuthKeyAndServerName server = new IpAuthKeyAndServerName(
                    (String) o.get("sidecarIp"),
                    (String) o.get("authKey"),
                    (String) o.get("serverName"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + server.getSidecarIp() + ":8080/ping"))
                    .header("authKey", server.getAuthKey())
                    .timeout(Duration.ofSeconds(1))
                    .build();

            try {
                client.send(request, HttpResponse.BodyHandlers.ofString()).body();
                servers.add(server);
            } catch (HttpTimeoutException e) {
                System.out.println("Ping for " + server.getServerName() + " timed out");
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        return servers;
    }
}

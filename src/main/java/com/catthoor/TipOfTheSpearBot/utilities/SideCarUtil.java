package com.catthoor.TipOfTheSpearBot.utilities;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        return (JSONArray) getRoot().get("authList");
    }

    public static void write(JSONObject root) {
        try {
            Files.writeString(filePath, root.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package com.catthoor.TipOfTheSpearBot.utilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TfeServerCredentialsUtil {

    public static List<TfeServerCredentials> getDiscordCredentials() {
        final Path path = Paths.get("discord-credentials");
        try {
            List<TfeServerCredentials> credentials = new ArrayList<>();
            List<String> lines = Files.readAllLines(path);
            Collections.reverse(lines);
            lines.forEach(line -> {
                String[] parts = line.split(";");
                TfeServerCredentials newReadCreds = new TfeServerCredentials(parts[0], parts[1], parts[2]);
                if (credentials.stream().noneMatch(cred -> cred.getIp().equals(newReadCreds.getIp())))
                    credentials.add(newReadCreds);
            });
            return credentials;
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}

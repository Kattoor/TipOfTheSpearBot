package com.catthoor.TipOfTheSpearBot;

import com.moandjiezana.toml.Toml;

public class Config {

    private static final Toml toml = new Toml().read(Config.class.getResourceAsStream("/app.secret.toml"));
    private static final String environment = System.getenv("env");

    public static String getToken() {

        return toml.getString("discord.token." + environment);
    }

    public static String getGeneralChannelId() {
        return toml.getString("discord.channel.general.id." + environment);
    }

    public static String getAnnouncementsUser() {
        return toml.getString("commands.announcements.user." + environment);
    }
}

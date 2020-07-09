package com.catthoor.TipOfTheSpearBot.utilities;

public class TfeServerCredentials {
    private final String name;
    private final String password;
    private final String ip;

    public TfeServerCredentials(String name, String password, String ip) {
        this.name = name;
        this.password = password;
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public String getIp() {
        return ip;
    }
}

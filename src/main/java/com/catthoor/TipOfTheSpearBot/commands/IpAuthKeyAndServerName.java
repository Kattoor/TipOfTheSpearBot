package com.catthoor.TipOfTheSpearBot.commands;

public class IpAuthKeyAndServerName {
    private final String sidecarIp;
    private final String authKey;
    private final String serverName;

    public IpAuthKeyAndServerName(String sidecarIp, String authKey, String serverName) {
        this.sidecarIp = sidecarIp;
        this.authKey = authKey;
        this.serverName = serverName;
    }

    public String getSidecarIp() {
        return sidecarIp;
    }

    public String getAuthKey() {
        return authKey;
    }

    public String getServerName() {
        return serverName;
    }
}

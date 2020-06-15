package com.catthoor.TipOfTheSpearBot.model;

import java.util.List;

public class Room {
    private int gameMode;
    private String region;
    private String sessionType;
    private String map;
    private String roomName;
    private List<PlayerInfo> blueTeam;
    private List<PlayerInfo> redTeam;
    private List<String> mapRotation;
    private int maxPlayer;
    private int gameLength;
    private int numOfBots;

    public Room(int gameMode, String region, String sessionType, String map, String roomName, List<PlayerInfo> blueTeam, List<PlayerInfo> redTeam, List<String> mapRotation, int maxPlayer, int gameLength, int numOfBots) {
        this.gameMode = gameMode;
        this.region = region;
        this.sessionType = sessionType;
        this.map = map;
        this.roomName = roomName;
        this.blueTeam = blueTeam;
        this.redTeam = redTeam;
        this.mapRotation = mapRotation;
        this.maxPlayer = maxPlayer;
        this.gameLength = gameLength;
        this.numOfBots = numOfBots;
    }

    public Room() {

    }

    public int getGameMode() {
        return gameMode;
    }

    public String getRegion() {
        return region;
    }

    public String getSessionType() {
        return sessionType;
    }

    public String getMap() {
        return map;
    }

    public String getRoomName() {
        return roomName;
    }

    public List<PlayerInfo> getBlueTeam() {
        return blueTeam;
    }

    public List<PlayerInfo> getRedTeam() {
        return redTeam;
    }

    public List<String> getMapRotation() {
        return mapRotation;
    }

    public int getMaxPlayer() {
        return maxPlayer;
    }

    public int getGameLength() {
        return gameLength;
    }

    public int getNumOfBots() {
        return numOfBots;
    }
}

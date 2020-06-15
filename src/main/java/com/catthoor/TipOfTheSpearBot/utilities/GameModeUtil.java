package com.catthoor.TipOfTheSpearBot.utilities;

public class GameModeUtil {

    public static String getGameMode(long gameMode) {
        switch ((int) gameMode) {
            case 0:
                return "DM";
            case 1:
                return "TDM";
            case 2:
                return "TKOTH";
            case 3:
                return "CTF";
            default:
                return "?";
        }
    }
}

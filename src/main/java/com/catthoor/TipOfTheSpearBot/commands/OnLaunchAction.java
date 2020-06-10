package com.catthoor.TipOfTheSpearBot.commands;

import discord4j.core.DiscordClient;

public interface OnLaunchAction {
    void onLaunch(DiscordClient client);
}

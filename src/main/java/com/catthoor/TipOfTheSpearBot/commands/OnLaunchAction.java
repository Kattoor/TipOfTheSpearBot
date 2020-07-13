package com.catthoor.TipOfTheSpearBot.commands;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;

public interface OnLaunchAction {
    void onLaunch(DiscordClient client, GatewayDiscordClient gateway);
}

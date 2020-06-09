package com.catthoor.TipOfTheSpearBot;

import discord4j.core.object.entity.Message;

public interface Command {

    void execute(Message message);
}

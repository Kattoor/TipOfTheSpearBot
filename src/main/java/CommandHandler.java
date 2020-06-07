import commands.PlayerCount;
import discord4j.core.object.entity.Message;

import java.util.Map;
import java.util.function.Consumer;

public class CommandHandler {

    private Map<String, Consumer<Message>> router = Map.ofEntries(Map.entry("?pc", message -> new PlayerCount().execute(message)));

    public void handle(Message message) {
        final String content = message.getContent();

        if (router.containsKey(content)) {
            message.getAuthor().ifPresent(user ->
                    System.out.println("User [" + user.getTag() + "] - " + content));

            router.get(content).accept(message);
        }
    }
}

import commands.AnnouncementCommand;
import commands.Command;
import commands.CommandKey;
import commands.PlayerCountCommand;
import discord4j.core.object.entity.Message;

import java.util.Map;
import java.util.Optional;

public class CommandHandler {

    private final Map<CommandKey, Command> router = Map.ofEntries(
            Map.entry(new CommandKey("?pc", false), new PlayerCountCommand()),
            Map.entry(new CommandKey("?announcement", true), new AnnouncementCommand()));

    private Optional<Command> getCommand(String content) {
        return router.entrySet().stream().filter(entry -> {
            CommandKey commandKey = entry.getKey();

            if (commandKey.isAcceptsParameters() && content.startsWith(commandKey.getKey()))
                return true;
            else if (commandKey.getKey().equals(content))
                return true;

            return false;
        }).map(Map.Entry::getValue).findFirst();
    }

    public void handle(Message message) {
        final String content = message.getContent();

        Optional<Command> optionalCommand = getCommand(content);

        optionalCommand.ifPresent((command) -> {
            message.getAuthor().ifPresent(user ->
                    System.out.println("User [" + user.getTag() + "] - " + content));

            command.execute(message);
        });
    }
}

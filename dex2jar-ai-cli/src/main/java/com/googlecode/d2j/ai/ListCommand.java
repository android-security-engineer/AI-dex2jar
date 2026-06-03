package com.googlecode.d2j.ai;

import picocli.CommandLine.Command;
import java.util.concurrent.Callable;

@Command(
    name = "list",
    description = "List available commands as JSON",
    mixinStandardHelpOptions = true
)
public class ListCommand implements Callable<Integer> {
    @Override
    public Integer call() {
        CommandExecutor executor = new CommandExecutor();
        String[] commands = executor.listCommands();
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"commands\": [\n");
        for (int i = 0; i < commands.length; i++) {
            if (i > 0) sb.append(",\n");
            sb.append("    {\"name\": \"").append(commands[i]).append("\"");
            sb.append(", \"description\": \"").append(JsonWriter.escape(CommandRegistry.getDescription(commands[i]))).append("\"}");
        }
        sb.append("\n  ]\n}");
        System.out.println(sb.toString());
        return 0;
    }
}
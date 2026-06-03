package com.googlecode.d2j.ai;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import java.util.Set;
import java.util.concurrent.Callable;

@Command(
    name = "d2j-ai",
    description = "dex2jar AI CLI - structured JSON interface for dex2jar tools",
    mixinStandardHelpOptions = true,
    version = "2.x",
    subcommands = { ListCommand.class, InfoCommand.class, CompletionCommand.class, BatchCommand.class }
)
public class D2jAiMain implements Callable<Integer> {
    @Option(names = {"--pretty"}, description = "Pretty-print JSON output")
    private boolean pretty;

    @Option(names = {"--verbose"}, description = "Show verbose output including stderr")
    private boolean verbose;

    public static void main(String[] args) {
        D2jAiMain main = new D2jAiMain();
        CommandLine cmd = new CommandLine(main);

        Set<String> commands = CommandRegistry.listCommands();
        for (String name : commands) {
            String desc = CommandRegistry.getDescription(name);
            D2jCommand sub = new D2jCommand(name, desc);
            CommandLine subCmd = new CommandLine(sub);
            subCmd.setCommandName(name);
            subCmd.getCommandSpec().usageMessage().description(desc);
            subCmd.setUsageHelpAutoWidth(true);
            cmd.addSubcommand(name, subCmd);
        }

        CliContext.setMain(main);
        try {
            int exitCode = cmd.execute(args);
            System.exit(exitCode);
        } finally {
            CliContext.clear();
        }
    }

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public boolean isPretty() { return pretty; }
    public boolean isVerbose() { return verbose; }
}
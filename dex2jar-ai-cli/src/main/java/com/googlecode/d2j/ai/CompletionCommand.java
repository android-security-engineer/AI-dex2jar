package com.googlecode.d2j.ai;

import picocli.CommandLine.Command;
import java.util.concurrent.Callable;

@Command(
    name = "completion",
    description = "Generate shell completion script for bash/zsh",
    mixinStandardHelpOptions = true
)
public class CompletionCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        D2jAiMain main = CliContext.getMain();
        if (main == null) {
            System.err.println("Error: CliContext not initialized");
            return 1;
        }

        String script = picocli.AutoComplete.bash(
            "d2j-ai",
            new picocli.CommandLine(main)
        );
        System.out.println(script);
        return 0;
    }
}

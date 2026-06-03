package com.googlecode.d2j.ai;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import java.util.concurrent.Callable;

@Command(
    mixinStandardHelpOptions = true,
    descriptionHeading = "%nDescription:%n",
    parameterListHeading = "%nParameters:%n",
    optionListHeading = "%nOptions:%n"
)
public class D2jCommand implements Callable<Integer> {
    private final String commandName;
    private final String description;

    @Parameters(arity = "0..*", description = "Arguments passed to the dex2jar tool")
    private String[] args;

    @Option(names = {"--pipe"}, description = "Use previous command's output path as input to this command")
    private boolean pipe;

    public D2jCommand(String commandName, String description) {
        this.commandName = commandName;
        this.description = description;
    }

    @Override
    public Integer call() {
        String[] effectiveArgs = args;
        if (pipe) {
            String pipedInput = CliContext.getPipedInput();
            if (pipedInput != null) {
                effectiveArgs = mergeArgs(args, pipedInput);
            }
        }

        CommandExecutor executor = new CommandExecutor();
        CommandResult result = executor.execute(commandName, effectiveArgs);

        if (pipe && result.isSuccess() && result.getOutputPath() != null) {
            CliContext.setPipedInput(result.getOutputPath());
        }

        D2jAiMain main = CliContext.getMain();
        if (main != null && main.isPretty()) {
            JsonWriter.writePretty(result);
        } else {
            JsonWriter.write(result);
        }

        if (main != null && main.isVerbose() && result.getStderr() != null && !result.getStderr().isEmpty()) {
            System.err.println("[stderr] " + result.getStderr());
        }

        return result.isSuccess() ? 0 : 1;
    }

    private String[] mergeArgs(String[] original, String pipedInput) {
        if (original == null || original.length == 0) {
            return new String[]{pipedInput};
        }
        String[] merged = new String[original.length + 1];
        System.arraycopy(original, 0, merged, 0, original.length);
        merged[original.length] = pipedInput;
        return merged;
    }

    public String getCommandName() { return commandName; }
    public String getDescription() { return description; }
}

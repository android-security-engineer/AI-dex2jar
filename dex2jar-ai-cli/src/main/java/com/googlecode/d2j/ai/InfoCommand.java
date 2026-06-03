package com.googlecode.d2j.ai;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "info",
    description = "Show command details and options as JSON",
    mixinStandardHelpOptions = true
)
public class InfoCommand implements Callable<Integer> {

    @Parameters(arity = "0..1", description = "Command name to inspect (omit to show all)")
    private String commandName;

    @Override
    public Integer call() {
        D2jAiMain main = CliContext.getMain();
        boolean pretty = main != null && main.isPretty();

        if (commandName != null) {
            if (!CommandRegistry.hasCommand(commandName)) {
                System.err.println("Unknown command: " + commandName);
                return 1;
            }
            System.out.println(formatCommand(commandName, pretty));
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(pretty ? "{\n  \"commands\": [\n" : "{\"commands\":[");
            String[] names = new CommandExecutor().listCommands();
            for (int i = 0; i < names.length; i++) {
                if (i > 0) sb.append(pretty ? ",\n" : ",");
                sb.append(formatCommand(names[i], pretty));
            }
            sb.append(pretty ? "\n  ]\n}" : "]}");
            System.out.println(sb.toString());
        }
        return 0;
    }

    private String formatCommand(String name, boolean pretty) {
        String indent = pretty ? "    " : "";
        String nl = pretty ? "\n" : "";
        StringBuilder sb = new StringBuilder();
        sb.append(pretty ? "    {" : "{\"name\":\"").append(pretty ? "" : JsonWriter.escape(name)).append("\"");
        if (pretty) {
            sb.append("{\n").append("      \"name\": \"").append(JsonWriter.escape(name)).append("\",\n");
            sb.append("      \"description\": \"").append(JsonWriter.escape(CommandRegistry.getDescription(name))).append("\",\n");
        } else {
            sb.append(",\"description\":\"").append(JsonWriter.escape(CommandRegistry.getDescription(name))).append("\",");
        }
        List<CommandMeta.OptionInfo> opts = CommandMeta.getOptions(name);
        if (pretty) {
            sb.append("      \"options\": [\n");
        } else {
            sb.append("\"options\":[");
        }
        for (int i = 0; i < opts.size(); i++) {
            if (i > 0) sb.append(pretty ? ",\n" : ",");
            CommandMeta.OptionInfo o = opts.get(i);
            if (pretty) sb.append("        ");
            sb.append("{");
            sb.append(pretty ? "\"short_opt\": " : "\"short_opt\":");
            sb.append(o.getShortOpt() != null ? "\"" + JsonWriter.escape("-" + o.getShortOpt()) + "\"" : "null");
            sb.append(pretty ? ", \"long_opt\": " : ",\"long_opt\":");
            sb.append(o.getLongOpt() != null ? "\"" + JsonWriter.escape("--" + o.getLongOpt()) + "\"" : "null");
            sb.append(pretty ? ", \"has_arg\": " : ",\"has_arg\":").append(o.isHasArg());
            sb.append(pretty ? ", \"description\": \"" : ",\"description\":\"").append(JsonWriter.escape(o.getDescription())).append("\"");
            sb.append(pretty ? ", \"arg_name\": " : ",\"arg_name\":");
            sb.append(o.getArgName() != null && !o.getArgName().isEmpty() ? "\"" + JsonWriter.escape(o.getArgName()) + "\"" : "null");
            sb.append(pretty ? ", \"required\": " : ",\"required\":").append(o.isRequired());
            sb.append("}");
        }
        if (pretty) {
            sb.append("\n      ]\n    }");
        } else {
            sb.append("]}");
        }
        return sb.toString();
    }
}

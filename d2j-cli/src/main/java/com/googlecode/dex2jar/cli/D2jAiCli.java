package com.googlecode.dex2jar.cli;

import com.googlecode.dex2jar.tools.BaseCmd;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class D2jAiCli {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    public static void main(String[] args) {
        if (args.length == 0 || args[0].equals("-h") || args[0].equals("--help") || args[0].equals("help")) {
            printHelp();
            return;
        }

        String subCommand = args[0];
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, subArgs.length);

        if (subCommand.equals("list")) {
            printList();
            return;
        }

        Class<? extends BaseCmd> cmdClass = CommandRegistry.getCommand(subCommand);
        if (cmdClass == null) {
            JsonResult result = JsonResult.error(subCommand,
                    "Unknown command: " + subCommand + ". Available: " + String.join(", ", CommandRegistry.getCommandNames()));
            System.out.println(result.toJson());
            System.exit(1);
            return;
        }

        executeCommand(subCommand, cmdClass, subArgs);
    }

    private static void executeCommand(String name, Class<? extends BaseCmd> cmdClass, String[] args) {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuf = new ByteArrayOutputStream();

        PrintStream capturedOut = new PrintStream(outBuf, true);
        PrintStream capturedErr = new PrintStream(errBuf, true);

        int exitCode = 0;
        try {
            System.setOut(capturedOut);
            System.setErr(capturedErr);

            BaseCmd cmd = cmdClass.newInstance();
            cmd.doMain(args);

            exitCode = 0;
        } catch (Exception e) {
            exitCode = 1;
            capturedErr.println(e.getMessage() != null ? e.getMessage() : e.getClass().getName());
        } finally {
            System.out.flush();
            System.err.flush();
            System.setOut(originalOut);
            System.setErr(originalErr);
        }

        String stdout = outBuf.toString();
        String stderr = errBuf.toString();

        JsonResult result = new JsonResult();
        result.success = (exitCode == 0);
        result.tool = name;
        result.command = buildCommandString(name, args);
        result.exitCode = exitCode;
        result.stdout = stdout;
        result.stderr = stderr;
        result.outputFile = extractOutputFile(stdout, args);

        System.out.println(result.toJson());
        System.exit(result.success ? 0 : 1);
    }

    private static String buildCommandString(String name, String[] args) {
        StringBuilder sb = new StringBuilder("d2j ").append(name);
        for (String arg : args) {
            sb.append(' ').append(arg);
        }
        return sb.toString();
    }

    private static String extractOutputFile(String stdout, String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("-o") || args[i].startsWith("--output")) {
                if (args[i].contains("=")) {
                    return args[i].substring(args[i].indexOf('=') + 1);
                }
                return args[i + 1];
            }
        }
        for (String line : stdout.split("\n")) {
            if (line.contains(" -> ")) {
                String[] parts = line.split(" -> ");
                if (parts.length >= 2) {
                    return parts[parts.length - 1].trim();
                }
            }
        }
        return null;
    }

    private static void printHelp() {
        Map<String, Object> help = new LinkedHashMap<>();
        help.put("success", true);
        help.put("name", "d2j");
        help.put("description", "AI-friendly CLI for dex2jar reverse engineering tools");
        help.put("usage", "d2j <command> [args...]");

        Map<String, String> commands = new LinkedHashMap<>();
        for (Map.Entry<String, Class<? extends BaseCmd>> entry : CommandRegistry.getCommands().entrySet()) {
            BaseCmd.Syntax syntax = entry.getValue().getAnnotation(BaseCmd.Syntax.class);
            commands.put(entry.getKey(), syntax != null ? syntax.desc() : "");
        }
        help.put("commands", commands);

        Map<String, String> special = new LinkedHashMap<>();
        special.put("list", "List all available commands");
        special.put("help", "Show this help");
        help.put("special_commands", special);

        System.out.println(GSON.toJson(help));
    }

    private static void printList() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);

        Map<String, String> descriptions = new LinkedHashMap<>();
        for (Map.Entry<String, Class<? extends BaseCmd>> entry : CommandRegistry.getCommands().entrySet()) {
            BaseCmd.Syntax syntax = entry.getValue().getAnnotation(BaseCmd.Syntax.class);
            descriptions.put(entry.getKey(), syntax != null ? syntax.desc() : "");
        }
        result.put("available_commands", descriptions.keySet().toArray(new String[0]));
        result.put("descriptions", descriptions);

        System.out.println(GSON.toJson(result));
    }
}

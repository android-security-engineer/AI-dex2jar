package com.googlecode.d2j.ai;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "batch",
    description = "Execute multiple commands from a JSON file",
    mixinStandardHelpOptions = true
)
public class BatchCommand implements Callable<Integer> {

    @Parameters(arity = "1", description = "JSON file containing commands array")
    private String inputFile;

    @Override
    public Integer call() {
        D2jAiMain main = CliContext.getMain();
        boolean pretty = main != null && main.isPretty();

        Path path = Paths.get(inputFile);
        if (!Files.exists(path)) {
            System.err.println("File not found: " + inputFile);
            return 1;
        }

        List<BatchEntry> entries = parseBatchFile(path);
        if (entries == null) {
            return 1;
        }

        List<CommandResult> results = new ArrayList<>();
        for (BatchEntry entry : entries) {
            CommandExecutor executor = new CommandExecutor();
            CommandResult result = executor.execute(entry.command, entry.args);
            results.add(result);
        }

        long successCount = results.stream().filter(CommandResult::isSuccess).count();
        long failureCount = results.size() - successCount;

        StringBuilder sb = new StringBuilder();
        sb.append(pretty ? "{\n  \"results\": [\n" : "{\"results\":[");
        for (int i = 0; i < results.size(); i++) {
            if (i > 0) sb.append(pretty ? ",\n" : ",");
            String json = pretty ? JsonWriter.toPrettyJson(results.get(i)) : JsonWriter.toJson(results.get(i));
            if (pretty) {
                sb.append("    ");
                sb.append(json.replace("\n", "\n    "));
            } else {
                sb.append(json);
            }
        }
        sb.append(pretty ? "\n  ],\n" : "],");
        sb.append(pretty ? "  \"total\": " : "\"total\":").append(results.size());
        sb.append(pretty ? ",\n  \"success_count\": " : ",\"success_count\":").append(successCount);
        sb.append(pretty ? ",\n  \"failure_count\": " : ",\"failure_count\":").append(failureCount);
        sb.append(pretty ? "\n}" : "}");
        System.out.println(sb.toString());

        return failureCount == 0 ? 0 : 1;
    }

    private List<BatchEntry> parseBatchFile(Path path) {
        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            return parseBatchJson(content);
        } catch (Exception e) {
            System.err.println("Error reading batch file: " + e.getMessage());
            return null;
        }
    }

    static List<BatchEntry> parseBatchJson(String json) {
        List<BatchEntry> entries = new ArrayList<>();
        json = json.trim();
        int arrStart = json.indexOf('[');
        int arrEnd = json.lastIndexOf(']');
        if (arrStart < 0 || arrEnd < 0) return entries;

        String arrContent = json.substring(arrStart + 1, arrEnd);
        int depth = 0;
        int objStart = -1;
        for (int i = 0; i < arrContent.length(); i++) {
            char c = arrContent.charAt(i);
            if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart >= 0) {
                    String obj = arrContent.substring(objStart, i + 1);
                    BatchEntry entry = parseEntry(obj);
                    if (entry != null) entries.add(entry);
                    objStart = -1;
                }
            }
        }
        return entries;
    }

    static BatchEntry parseEntry(String obj) {
        String command = extractStringField(obj, "command");
        if (command == null) return null;
        String[] args = extractArrayField(obj, "args");
        return new BatchEntry(command, args != null ? args : new String[0]);
    }

    static String extractStringField(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        idx = json.indexOf(':', idx + key.length());
        if (idx < 0) return null;
        int start = json.indexOf('"', idx + 1);
        if (start < 0) return null;
        int end = json.indexOf('"', start + 1);
        if (end < 0) return null;
        return json.substring(start + 1, end);
    }

    static String[] extractArrayField(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx < 0) return new String[0];
        int arrStart = json.indexOf('[', idx);
        if (arrStart < 0) return new String[0];
        int arrEnd = json.indexOf(']', arrStart);
        if (arrEnd < 0) return new String[0];
        String content = json.substring(arrStart + 1, arrEnd).trim();
        if (content.isEmpty()) return new String[0];
        List<String> items = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '"') {
                inString = !inString;
            } else if (c == ',' && !inString) {
                String val = current.toString().trim();
                if (!val.isEmpty()) items.add(val);
                current = new StringBuilder();
            } else if (inString) {
                current.append(c);
            }
        }
        String val = current.toString().trim();
        if (!val.isEmpty()) items.add(val);
        return items.toArray(new String[0]);
    }

    static class BatchEntry {
        final String command;
        final String[] args;
        BatchEntry(String command, String[] args) {
            this.command = command;
            this.args = args;
        }
    }
}

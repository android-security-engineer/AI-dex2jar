package com.googlecode.d2j.ai;

import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class JsonWriter {
    public static void write(CommandResult result) {
        PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
        out.println(toJson(result));
    }

    public static void writePretty(CommandResult result) {
        PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
        out.println(toPrettyJson(result));
    }

    static String toPrettyJson(CommandResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        prettyField(sb, "success", r.isSuccess(), false);
        sb.append(",\n");
        prettyField(sb, "error_code", r.getErrorCode() != null ? r.getErrorCode().name() : null, true);
        sb.append(",\n");
        prettyField(sb, "error_message", r.getErrorMessage(), true);
        sb.append(",\n");
        prettyField(sb, "output_path", r.getOutputPath(), true);
        sb.append(",\n");
        prettyField(sb, "stdout", r.getStdout(), true);
        sb.append(",\n");
        prettyField(sb, "stderr", r.getStderr(), true);
        sb.append(",\n");
        prettyField(sb, "command", r.getCommand(), true);
        sb.append(",\n");
        prettyField(sb, "duration_ms", r.getDurationMs(), false);
        sb.append(",\n");
        prettyField(sb, "exit_code", r.getExitCode(), false);
        sb.append(",\n");
        prettyField(sb, "timestamp", r.getTimestamp(), true);
        sb.append("\n}");
        return sb.toString();
    }

    private static void prettyField(StringBuilder sb, String key, String value, boolean quote) {
        sb.append("  \"").append(key).append("\": ");
        if (value == null) {
            sb.append("null");
        } else if (quote) {
            sb.append("\"").append(escape(value)).append("\"");
        } else {
            sb.append(value);
        }
    }

    private static void prettyField(StringBuilder sb, String key, long value, boolean quote) {
        sb.append("  \"").append(key).append("\": ").append(value);
    }

    private static void prettyField(StringBuilder sb, String key, int value, boolean quote) {
        sb.append("  \"").append(key).append("\": ").append(value);
    }

    private static void prettyField(StringBuilder sb, String key, boolean value, boolean quote) {
        sb.append("  \"").append(key).append("\": ").append(value);
    }

    static String toJson(CommandResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        field(sb, "success", r.isSuccess(), false);
        sb.append(",");
        field(sb, "error_code", r.getErrorCode() != null ? r.getErrorCode().name() : null, true);
        sb.append(",");
        field(sb, "error_message", r.getErrorMessage(), true);
        sb.append(",");
        field(sb, "output_path", r.getOutputPath(), true);
        sb.append(",");
        field(sb, "stdout", r.getStdout(), true);
        sb.append(",");
        field(sb, "stderr", r.getStderr(), true);
        sb.append(",");
        field(sb, "command", r.getCommand(), true);
        sb.append(",");
        field(sb, "duration_ms", r.getDurationMs(), false);
        sb.append(",");
        field(sb, "exit_code", r.getExitCode(), false);
        sb.append(",");
        field(sb, "timestamp", r.getTimestamp(), true);
        sb.append("}");
        return sb.toString();
    }

    private static void field(StringBuilder sb, String key, String value, boolean quote) {
        sb.append("\"").append(key).append("\":");
        if (value == null) {
            sb.append("null");
        } else if (quote) {
            sb.append("\"").append(escape(value)).append("\"");
        } else {
            sb.append(value);
        }
    }

    private static void field(StringBuilder sb, String key, long value, boolean quote) {
        sb.append("\"").append(key).append("\":").append(value);
    }

    private static void field(StringBuilder sb, String key, int value, boolean quote) {
        sb.append("\"").append(key).append("\":").append(value);
    }

    private static void field(StringBuilder sb, String key, boolean value, boolean quote) {
        sb.append("\"").append(key).append("\":").append(value);
    }

    static String escape(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}

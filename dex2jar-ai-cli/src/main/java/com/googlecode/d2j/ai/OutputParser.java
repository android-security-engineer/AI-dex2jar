package com.googlecode.d2j.ai;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OutputParser {
    private static final Pattern OUTPUT_PATH_PATTERN = Pattern.compile("\\S+\\s+->\\s+(\\S+)");

    public static String parseOutputPath(String commandName, String stderr, String stdout) {
        String combined = (stderr != null ? stderr : "") + "\n" + (stdout != null ? stdout : "");
        String[] lines = combined.split("\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            Matcher m = OUTPUT_PATH_PATTERN.matcher(line);
            if (m.find()) {
                String path = m.group(1);
                if (!path.startsWith("-") && !path.startsWith("ERROR")) {
                    return path;
                }
            }
        }
        return null;
    }
}
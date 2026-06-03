package com.googlecode.d2j.ai;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class InputValidator {
    public static String validate(String commandName, String[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        String inputFile = findInputFile(args);
        if (inputFile == null) {
            return null;
        }
        Path path = Paths.get(inputFile);
        if (!Files.exists(path)) {
            return "File not found: " + inputFile;
        }
        if (Files.isDirectory(path)) {
            return null;
        }
        if (!Files.isReadable(path)) {
            return "File not readable: " + inputFile;
        }
        return null;
    }

    private static String findInputFile(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("-")) continue;
            if (arg.endsWith(".dex") || arg.endsWith(".apk") || arg.endsWith(".jar")
                    || arg.endsWith(".zip") || arg.endsWith(".smali") || arg.endsWith(".j")) {
                return arg;
            }
        }
        for (String arg : args) {
            if (!arg.startsWith("-")) return arg;
        }
        return null;
    }
}
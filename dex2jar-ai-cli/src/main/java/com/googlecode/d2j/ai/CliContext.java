package com.googlecode.d2j.ai;

public class CliContext {
    private static final ThreadLocal<D2jAiMain> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<String> PIPED_INPUT = new ThreadLocal<>();

    public static void setMain(D2jAiMain main) {
        CURRENT.set(main);
    }

    public static D2jAiMain getMain() {
        return CURRENT.get();
    }

    public static void setPipedInput(String path) {
        PIPED_INPUT.set(path);
    }

    public static String getPipedInput() {
        return PIPED_INPUT.get();
    }

    public static void clear() {
        CURRENT.remove();
        PIPED_INPUT.remove();
    }
}
package com.googlecode.dex2jar.cli;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonResult {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    public boolean success;
    public String tool;
    public String command;
    public int exitCode;
    public String stdout;
    public String stderr;
    public String outputFile;

    public JsonResult() {}

    public static JsonResult ok(String tool, String command, String stdout, String stderr) {
        JsonResult r = new JsonResult();
        r.success = true;
        r.tool = tool;
        r.command = command;
        r.exitCode = 0;
        r.stdout = stdout;
        r.stderr = stderr;
        return r;
    }

    public static JsonResult fail(String tool, String command, int exitCode, String stderr) {
        JsonResult r = new JsonResult();
        r.success = false;
        r.tool = tool;
        r.command = command;
        r.exitCode = exitCode;
        r.stderr = stderr;
        return r;
    }

    public static JsonResult error(String tool, String stderr) {
        JsonResult r = new JsonResult();
        r.success = false;
        r.tool = tool;
        r.exitCode = -1;
        r.stderr = stderr;
        return r;
    }

    public String toJson() {
        return GSON.toJson(this);
    }
}

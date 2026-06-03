package com.googlecode.d2j.ai;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class OutputCapture {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private PrintStream origOut;
    private PrintStream origErr;

    public void begin() {
        origOut = System.out;
        origErr = System.err;
        System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(errContent, true, StandardCharsets.UTF_8));
    }

    public void end() {
        System.setOut(origOut);
        System.setErr(origErr);
    }

    public String getStdout() {
        return new String(outContent.toByteArray(), StandardCharsets.UTF_8);
    }

    public String getStderr() {
        return new String(errContent.toByteArray(), StandardCharsets.UTF_8);
    }
}
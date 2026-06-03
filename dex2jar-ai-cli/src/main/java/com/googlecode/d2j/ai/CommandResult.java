package com.googlecode.d2j.ai;

public class CommandResult {
    public enum ErrorCode {
        NONE, FILE_NOT_FOUND, INVALID_FORMAT, COMMAND_FAILED, UNKNOWN_COMMAND
    }

    private boolean success;
    private ErrorCode errorCode;
    private String errorMessage;
    private String outputPath;
    private String stdout;
    private String stderr;
    private String command;
    private long durationMs;
    private int exitCode;
    private String timestamp;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public ErrorCode getErrorCode() { return errorCode; }
    public void setErrorCode(ErrorCode errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getOutputPath() { return outputPath; }
    public void setOutputPath(String outputPath) { this.outputPath = outputPath; }

    public String getStdout() { return stdout; }
    public void setStdout(String stdout) { this.stdout = stdout; }

    public String getStderr() { return stderr; }
    public void setStderr(String stderr) { this.stderr = stderr; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public int getExitCode() { return exitCode; }
    public void setExitCode(int exitCode) { this.exitCode = exitCode; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}

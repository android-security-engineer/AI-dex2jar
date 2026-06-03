package com.googlecode.d2j.ai;

import com.googlecode.dex2jar.tools.BaseCmd;
import java.lang.reflect.Method;
import java.util.Set;

public class CommandExecutor {
    public CommandResult execute(String commandName, String[] args) {
        CommandResult result = new CommandResult();
        result.setCommand(commandName);

        if (!CommandRegistry.hasCommand(commandName)) {
            result.setSuccess(false);
            result.setErrorCode(CommandResult.ErrorCode.UNKNOWN_COMMAND);
            result.setErrorMessage("Unknown command: " + commandName);
            return result;
        }

        String validationError = InputValidator.validate(commandName, args);
        if (validationError != null) {
            result.setSuccess(false);
            result.setErrorCode(CommandResult.ErrorCode.FILE_NOT_FOUND);
            result.setErrorMessage(validationError);
            return result;
        }

        OutputCapture capture = new OutputCapture();
        long startTime = System.currentTimeMillis();

        try {
            capture.begin();
            if (CommandRegistry.isBaseCmdCommand(commandName)) {
                Class<? extends BaseCmd> clz = CommandRegistry.getCommandClass(commandName);
                BaseCmd cmd = clz.newInstance();
                cmd.doMain(args != null ? args : new String[0]);
            } else if (CommandRegistry.isStaticMainCommand(commandName)) {
                String className = CommandRegistry.getStaticMainClassName(commandName);
                Class<?> clz = Class.forName(className);
                Method mainMethod = clz.getMethod("main", String[].class);
                mainMethod.invoke(null, (Object) (args != null ? args : new String[0]));
            }
            capture.end();
            long duration = System.currentTimeMillis() - startTime;

            String stdout = capture.getStdout();
            String stderr = capture.getStderr();
            String outputPath = OutputParser.parseOutputPath(commandName, stderr, stdout);

            result.setSuccess(true);
            result.setErrorCode(CommandResult.ErrorCode.NONE);
            result.setExitCode(0);
            result.setTimestamp(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new java.util.Date(startTime)));
            result.setStdout(stdout);
            result.setStderr(stderr);
            result.setOutputPath(outputPath);
            result.setDurationMs(duration);
        } catch (Exception e) {
            capture.end();
            long duration = System.currentTimeMillis() - startTime;
            result.setSuccess(false);
            result.setErrorCode(CommandResult.ErrorCode.COMMAND_FAILED);
            result.setExitCode(1);
            result.setTimestamp(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new java.util.Date(startTime)));
            result.setErrorMessage(e.getMessage() != null ? e.getMessage() : e.getClass().getName());
            result.setStdout(capture.getStdout());
            result.setStderr(capture.getStderr());
            result.setDurationMs(duration);
        }

        return result;
    }

    public String[] listCommands() {
        Set<String> commands = CommandRegistry.listCommands();
        return commands.toArray(new String[0]);
    }
}
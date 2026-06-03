package com.googlecode.d2j.ai;

import com.googlecode.dex2jar.tools.BaseCmd;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class CommandMeta {

    public static class OptionInfo {
        private final String shortOpt;
        private final String longOpt;
        private final boolean hasArg;
        private final String description;
        private final String argName;
        private final boolean required;

        public OptionInfo(String shortOpt, String longOpt, boolean hasArg,
                          String description, String argName, boolean required) {
            this.shortOpt = shortOpt;
            this.longOpt = longOpt;
            this.hasArg = hasArg;
            this.description = description;
            this.argName = argName;
            this.required = required;
        }

        public String getShortOpt() { return shortOpt; }
        public String getLongOpt() { return longOpt; }
        public boolean isHasArg() { return hasArg; }
        public String getDescription() { return description; }
        public String getArgName() { return argName; }
        public boolean isRequired() { return required; }
    }

    public static List<OptionInfo> getOptions(String commandName) {
        List<OptionInfo> options = new ArrayList<>();
        if (!CommandRegistry.isBaseCmdCommand(commandName)) {
            return options;
        }
        Class<? extends BaseCmd> clz = CommandRegistry.getCommandClass(commandName);
        for (Field field : clz.getDeclaredFields()) {
            BaseCmd.Opt opt = field.getAnnotation(BaseCmd.Opt.class);
            if (opt == null) continue;
            String shortOpt = opt.opt();
            String longOpt = opt.longOpt();
            if (shortOpt.isEmpty() && longOpt.isEmpty()) {
                longOpt = fromCamel(field.getName());
            }
            boolean hasArg = opt.hasArg();
            if (!hasArg && (field.getType() != boolean.class && field.getType() != Boolean.class)) {
                hasArg = true;
            }
            options.add(new OptionInfo(
                shortOpt.isEmpty() ? null : shortOpt,
                longOpt.isEmpty() ? null : longOpt,
                hasArg,
                opt.description(),
                opt.argName(),
                opt.required()
            ));
        }
        return options;
    }

    static String fromCamel(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append('-').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}

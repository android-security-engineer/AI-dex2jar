package com.googlecode.d2j.ai;

import com.googlecode.dex2jar.tools.BaseCmd;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class CommandRegistry {
    private static final Map<String, Class<? extends BaseCmd>> COMMANDS;
    private static final Map<String, String> DESCRIPTIONS;
    private static final Map<String, String> STATIC_MAIN_COMMANDS;
    private static final Map<String, String> STATIC_MAIN_DESCRIPTIONS;

    static {
        Map<String, Class<? extends BaseCmd>> cmds = new LinkedHashMap<>();
        Map<String, String> descs = new LinkedHashMap<>();

        cmds.put("dex2jar", com.googlecode.dex2jar.tools.Dex2jarCmd.class);
        descs.put("dex2jar", "Convert .dex/.apk to .jar");

        cmds.put("jar2dex", com.googlecode.dex2jar.tools.Jar2Dex.class);
        descs.put("jar2dex", "Convert .jar to .dex");

        cmds.put("baksmali", com.googlecode.d2j.smali.BaksmaliCmd.class);
        descs.put("baksmali", "Disassemble .dex to smali files");

        cmds.put("smali", com.googlecode.d2j.smali.SmaliCmd.class);
        descs.put("smali", "Assemble smali files into .dex");

        cmds.put("apk-sign", com.googlecode.dex2jar.tools.ApkSign.class);
        descs.put("apk-sign", "Sign APK with test certificate");

        cmds.put("jar-access", com.googlecode.dex2jar.tools.JarAccessCmd.class);
        descs.put("jar-access", "Modify access flags in .jar");

        cmds.put("asm-verify", com.googlecode.dex2jar.tools.AsmVerify.class);
        descs.put("asm-verify", "Verify .class files in jar");

        cmds.put("jar2jasmin", com.googlecode.d2j.jasmin.Jar2JasminCmd.class);
        descs.put("jar2jasmin", "Disassemble .class to jasmin format");

        cmds.put("jasmin2jar", com.googlecode.d2j.jasmin.Jasmin2JarCmd.class);
        descs.put("jasmin2jar", "Assemble jasmin files to .jar");

        cmds.put("decrypt-string", com.googlecode.dex2jar.tools.DecryptStringCmd.class);
        descs.put("decrypt-string", "Decrypt strings in .class files");

        cmds.put("std-apk", com.googlecode.dex2jar.tools.StdApkCmd.class);
        descs.put("std-apk", "Clean up APK to standard zip");

        cmds.put("dex-recompute-checksum", com.googlecode.dex2jar.tools.DexRecomputeChecksum.class);
        descs.put("dex-recompute-checksum", "Recompute CRC and SHA1 of .dex");

        cmds.put("dex-weaver", com.googlecode.dex2jar.tools.DexWeaverCmd.class);
        descs.put("dex-weaver", "Replace invoke in .dex");

        cmds.put("jar-weaver", com.googlecode.dex2jar.tools.JarWeaverCmd.class);
        descs.put("jar-weaver", "Replace invoke in .jar");

        cmds.put("init-deobf", com.googlecode.dex2jar.tools.DeObfInitCmd.class);
        descs.put("init-deobf", "Generate init config file for deobfuscation");

        COMMANDS = Collections.unmodifiableMap(cmds);
        DESCRIPTIONS = Collections.unmodifiableMap(descs);

        Map<String, String> staticCmds = new LinkedHashMap<>();
        Map<String, String> staticDescs = new LinkedHashMap<>();

        staticCmds.put("class-version-switch", "com.googlecode.dex2jar.tools.ClassVersionSwitch");
        staticDescs.put("class-version-switch", "Switch .class file version");

        STATIC_MAIN_COMMANDS = Collections.unmodifiableMap(staticCmds);
        STATIC_MAIN_DESCRIPTIONS = Collections.unmodifiableMap(staticDescs);
    }

    public static Class<? extends BaseCmd> getCommandClass(String name) {
        return COMMANDS.get(name);
    }

    public static String getDescription(String name) {
        String desc = DESCRIPTIONS.get(name);
        if (desc != null) return desc;
        return STATIC_MAIN_DESCRIPTIONS.get(name);
    }

    public static boolean isBaseCmdCommand(String name) {
        return COMMANDS.containsKey(name);
    }

    public static boolean isStaticMainCommand(String name) {
        return STATIC_MAIN_COMMANDS.containsKey(name);
    }

    public static String getStaticMainClassName(String name) {
        return STATIC_MAIN_COMMANDS.get(name);
    }

    public static Set<String> listCommands() {
        Set<String> all = new java.util.LinkedHashSet<>();
        all.addAll(COMMANDS.keySet());
        all.addAll(STATIC_MAIN_COMMANDS.keySet());
        return Collections.unmodifiableSet(all);
    }

    public static boolean hasCommand(String name) {
        return COMMANDS.containsKey(name) || STATIC_MAIN_COMMANDS.containsKey(name);
    }
}
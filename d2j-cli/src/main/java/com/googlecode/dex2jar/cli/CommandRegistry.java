package com.googlecode.dex2jar.cli;

import com.googlecode.dex2jar.tools.BaseCmd;
import com.googlecode.dex2jar.tools.Dex2jarCmd;
import com.googlecode.dex2jar.tools.ApkSign;
import com.googlecode.dex2jar.tools.Jar2Dex;
import com.googlecode.dex2jar.tools.JarAccessCmd;
import com.googlecode.dex2jar.tools.AsmVerify;
import com.googlecode.dex2jar.tools.DecryptStringCmd;
import com.googlecode.dex2jar.tools.DexRecomputeChecksum;
import com.googlecode.dex2jar.tools.DexWeaverCmd;
import com.googlecode.dex2jar.tools.JarWeaverCmd;
import com.googlecode.dex2jar.tools.StdApkCmd;
import com.googlecode.d2j.smali.BaksmaliCmd;
import com.googlecode.d2j.smali.SmaliCmd;
import com.googlecode.d2j.jasmin.Jar2JasminCmd;
import com.googlecode.d2j.jasmin.Jasmin2JarCmd;

import java.util.LinkedHashMap;
import java.util.Map;

public class CommandRegistry {
    private static final Map<String, Class<? extends BaseCmd>> COMMANDS = new LinkedHashMap<>();

    static {
        COMMANDS.put("dex2jar", Dex2jarCmd.class);
        COMMANDS.put("jar2dex", Jar2Dex.class);
        COMMANDS.put("baksmali", BaksmaliCmd.class);
        COMMANDS.put("smali", SmaliCmd.class);
        COMMANDS.put("apk-sign", ApkSign.class);
        COMMANDS.put("jar-access", JarAccessCmd.class);
        COMMANDS.put("asm-verify", AsmVerify.class);
        COMMANDS.put("jar2jasmin", Jar2JasminCmd.class);
        COMMANDS.put("jasmin2jar", Jasmin2JarCmd.class);
        COMMANDS.put("decrypt-string", DecryptStringCmd.class);
        COMMANDS.put("std-apk", StdApkCmd.class);
        COMMANDS.put("dex-recompute-checksum", DexRecomputeChecksum.class);
        COMMANDS.put("dex-weaver", DexWeaverCmd.class);
        COMMANDS.put("jar-weaver", JarWeaverCmd.class);
    }

    public static Map<String, Class<? extends BaseCmd>> getCommands() {
        return COMMANDS;
    }

    public static Class<? extends BaseCmd> getCommand(String name) {
        return COMMANDS.get(name);
    }

    public static String[] getCommandNames() {
        return COMMANDS.keySet().toArray(new String[0]);
    }
}

package com.googlecode.d2j.ai;

import com.googlecode.d2j.DexConstants;
import com.googlecode.d2j.Field;
import com.googlecode.d2j.Method;
import com.googlecode.d2j.node.DexClassNode;
import com.googlecode.d2j.node.DexFieldNode;
import com.googlecode.d2j.node.DexFileNode;
import com.googlecode.d2j.node.DexMethodNode;
import com.googlecode.d2j.reader.BaseDexFileReader;
import com.googlecode.d2j.reader.DexFileReader;
import com.googlecode.d2j.reader.MultiDexFileReader;
import com.googlecode.d2j.visitors.DexClassVisitor;
import com.googlecode.d2j.visitors.DexFileVisitor;
import com.googlecode.dex2jar.tools.BaseCmd;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Inspect DEX/APK structure — list classes, methods, fields as structured JSON.
 */
public class DexInspectCmd extends BaseCmd {

    @Opt(opt = "f", longOpt = "filter", description = "Filter class names by pattern (substring match)")
    String filter;

    @Opt(opt = "d", longOpt = "detail", hasArg = false, description = "Show method and field details for each class")
    boolean detail;

    @Opt(opt = "s", longOpt = "strings", hasArg = false, description = "Include string constants from fields")
    boolean strings;

    @Opt(opt = "o", longOpt = "output", description = "Output file path (default: stdout)")
    String output;

    @Override
    protected void doCommandLine() throws Exception {
        if (remainingArgs == null || remainingArgs.length == 0) {
            throw new HelpException("No input file specified");
        }

        Path input = Paths.get(remainingArgs[0]);
        if (!Files.exists(input)) {
            System.err.println("File not found: " + input);
            return;
        }

        byte[] data = Files.readAllBytes(input);
        BaseDexFileReader reader = MultiDexFileReader.open(data);

        // Read DEX into node structure
        DexFileNode fileNode = new DexFileNode();
        int config = DexFileReader.SKIP_CODE; // Skip code for fast inspection
        reader.accept(fileNode, config);

        // Build JSON result
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        // DEX version
        int dexVersion = reader.getDexVersion();
        sb.append("  \"dex_version\": \"").append(formatDexVersion(dexVersion)).append("\",\n");
        sb.append("  \"min_api_level\": ").append(DexConstants.toMiniAndroidApiLevel(dexVersion)).append(",\n");

        // Filter classes
        List<DexClassNode> filteredClasses = new ArrayList<>();
        if (fileNode.clzs != null) {
            for (DexClassNode cn : fileNode.clzs) {
                if (filter == null || filter.isEmpty() || cn.className.contains(filter)) {
                    filteredClasses.add(cn);
                }
            }
        }

        // Counts
        int methodCount = 0;
        int fieldCount = 0;
        for (DexClassNode cn : filteredClasses) {
            if (cn.methods != null) methodCount += cn.methods.size();
            if (cn.fields != null) fieldCount += cn.fields.size();
        }

        sb.append("  \"class_count\": ").append(filteredClasses.size()).append(",\n");
        sb.append("  \"method_count\": ").append(methodCount).append(",\n");
        sb.append("  \"field_count\": ").append(fieldCount).append(",\n");

        // Classes array
        sb.append("  \"classes\": [");
        for (int i = 0; i < filteredClasses.size(); i++) {
            if (i > 0) sb.append(",");
            DexClassNode cn = filteredClasses.get(i);
            sb.append("\n    {\n");
            sb.append("      \"name\": ").append(jsonStr(cn.className)).append(",\n");
            sb.append("      \"access\": ").append(cn.access).append(",\n");
            sb.append("      \"access_flags\": ").append(jsonStr(accessFlags(cn.access))).append(",\n");
            if (cn.superClass != null) {
                sb.append("      \"super\": ").append(jsonStr(cn.superClass)).append(",\n");
            }
            if (cn.interfaceNames != null && cn.interfaceNames.length > 0) {
                sb.append("      \"interfaces\": [");
                for (int j = 0; j < cn.interfaceNames.length; j++) {
                    if (j > 0) sb.append(", ");
                    sb.append(jsonStr(cn.interfaceNames[j]));
                }
                sb.append("],\n");
            }

            if (detail) {
                // Methods
                sb.append("      \"methods\": [");
                if (cn.methods != null) {
                    for (int j = 0; j < cn.methods.size(); j++) {
                        if (j > 0) sb.append(",");
                        DexMethodNode mn = cn.methods.get(j);
                        sb.append("\n        {\"name\": ").append(jsonStr(mn.method.getName()));
                        sb.append(", \"desc\": ").append(jsonStr(mn.method.getDesc()));
                        sb.append(", \"access\": ").append(mn.access);
                        sb.append(", \"access_flags\": ").append(jsonStr(accessFlags(mn.access)));
                        sb.append("}");
                    }
                }
                sb.append("],\n");

                // Fields
                sb.append("      \"fields\": [");
                if (cn.fields != null) {
                    for (int j = 0; j < cn.fields.size(); j++) {
                        if (j > 0) sb.append(",");
                        DexFieldNode fn = cn.fields.get(j);
                        sb.append("\n        {\"name\": ").append(jsonStr(fn.field.getName()));
                        sb.append(", \"type\": ").append(jsonStr(fn.field.getType()));
                        sb.append(", \"access\": ").append(fn.access);
                        sb.append(", \"access_flags\": ").append(jsonStr(accessFlags(fn.access)));
                        if (strings && fn.cst != null) {
                            sb.append(", \"value\": ").append(jsonStr(String.valueOf(fn.cst)));
                        }
                        sb.append("}");
                    }
                }
                sb.append("],\n");
            } else {
                // Just method/field names
                sb.append("      \"method_count\": ").append(cn.methods != null ? cn.methods.size() : 0).append(",\n");
                sb.append("      \"field_count\": ").append(cn.fields != null ? cn.fields.size() : 0).append(",\n");
            }

            sb.append("      \"source\": ").append(jsonStr(cn.source));
            sb.append("\n    }");
        }
        if (!filteredClasses.isEmpty()) sb.append("\n  ");
        sb.append("]\n}");

        // Output
        String result = sb.toString();
        if (output != null && !output.isEmpty()) {
            Files.write(Paths.get(output), result.getBytes("UTF-8"));
            System.out.println("Written to " + output);
        } else {
            System.out.println(result);
        }
    }

    private static String formatDexVersion(int version) {
        return String.format("%03d", (version & 0xFF));
    }

    private static String accessFlags(int access) {
        List<String> flags = new ArrayList<>();
        if ((access & DexConstants.ACC_PUBLIC) != 0) flags.add("public");
        if ((access & DexConstants.ACC_PRIVATE) != 0) flags.add("private");
        if ((access & DexConstants.ACC_PROTECTED) != 0) flags.add("protected");
        if ((access & DexConstants.ACC_STATIC) != 0) flags.add("static");
        if ((access & DexConstants.ACC_FINAL) != 0) flags.add("final");
        if ((access & DexConstants.ACC_SYNCHRONIZED) != 0) flags.add("synchronized");
        if ((access & DexConstants.ACC_VOLATILE) != 0) flags.add("volatile");
        if ((access & DexConstants.ACC_BRIDGE) != 0) flags.add("bridge");
        if ((access & DexConstants.ACC_VARARGS) != 0) flags.add("varargs");
        if ((access & DexConstants.ACC_TRANSIENT) != 0) flags.add("transient");
        if ((access & DexConstants.ACC_NATIVE) != 0) flags.add("native");
        if ((access & DexConstants.ACC_INTERFACE) != 0) flags.add("interface");
        if ((access & DexConstants.ACC_ABSTRACT) != 0) flags.add("abstract");
        if ((access & DexConstants.ACC_STRICT) != 0) flags.add("strict");
        if ((access & DexConstants.ACC_SYNTHETIC) != 0) flags.add("synthetic");
        if ((access & DexConstants.ACC_ANNOTATION) != 0) flags.add("annotation");
        if ((access & DexConstants.ACC_ENUM) != 0) flags.add("enum");
        if ((access & DexConstants.ACC_CONSTRUCTOR) != 0) flags.add("constructor");
        return String.join(" ", flags);
    }

    private static String jsonStr(String s) {
        return Json.str(s);
    }
}

package com.googlecode.d2j.ai;

import com.googlecode.d2j.node.*;
import com.googlecode.d2j.node.insn.ConstStmtNode;
import com.googlecode.d2j.node.insn.DexStmtNode;
import com.googlecode.d2j.reader.BaseDexFileReader;
import com.googlecode.d2j.reader.MultiDexFileReader;
import com.googlecode.dex2jar.tools.BaseCmd;

import java.nio.file.*;
import java.util.*;

/**
 * Extract all string constants from a DEX/APK file.
 */
public class DexStringsCmd extends BaseCmd {

    @Opt(opt = "f", longOpt = "filter", description = "Filter strings by pattern (substring match)")
    String filter;

    @Opt(opt = "c", longOpt = "class", description = "Filter by class name (substring match)")
    String classFilter;

    @Opt(opt = "u", longOpt = "unique", hasArg = false, description = "Only show unique strings (deduplicated)")
    boolean unique;

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

        // Read DEX with code (need code to find const-string instructions)
        DexFileNode fileNode = new DexFileNode();
        reader.accept(fileNode, 0); // Read everything including code

        Set<String> seen = new LinkedHashSet<>();
        List<String> entries = new ArrayList<>();

        if (fileNode.clzs != null) {
            for (DexClassNode cn : fileNode.clzs) {
                if (classFilter != null && !classFilter.isEmpty() && !cn.className.contains(classFilter)) {
                    continue;
                }

                // Static field constants
                if (cn.fields != null) {
                    for (DexFieldNode fn : cn.fields) {
                        if (fn.cst != null && fn.cst instanceof String) {
                            String val = (String) fn.cst;
                            if (filter != null && !filter.isEmpty() && !val.contains(filter)) continue;
                            String entry = cn.className + " | field " + fn.field.getName() + " = \"" + escape(val) + "\"";
                            if (unique) {
                                if (seen.add(val)) entries.add(entry);
                            } else {
                                entries.add(entry);
                            }
                        }
                    }
                }

                // String constants in code
                if (cn.methods != null) {
                    for (DexMethodNode mn : cn.methods) {
                        if (mn.codeNode == null || mn.codeNode.stmts == null) continue;
                        for (DexStmtNode stmt : mn.codeNode.stmts) {
                            if (stmt instanceof ConstStmtNode) {
                                ConstStmtNode cs = (ConstStmtNode) stmt;
                                if (cs.value instanceof String) {
                                    String val = (String) cs.value;
                                    if (filter != null && !filter.isEmpty() && !val.contains(filter)) continue;
                                    String entry = cn.className + " | " + mn.method.getName() + " | const-string \"" + escape(val) + "\"";
                                    if (unique) {
                                        if (seen.add(val)) entries.add(entry);
                                    } else {
                                        entries.add(entry);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Build JSON
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"string_count\": ").append(entries.size()).append(",\n");
        if (unique) {
            sb.append("  \"unique_count\": ").append(seen.size()).append(",\n");
        }
        sb.append("  \"strings\": [");
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\n    ").append(jsonStr(entries.get(i)));
        }
        if (!entries.isEmpty()) sb.append("\n  ");
        sb.append("]\n}");

        String result = sb.toString();
        if (output != null && !output.isEmpty()) {
            Files.write(Paths.get(output), result.getBytes("UTF-8"));
            System.out.println("Written to " + output);
        } else {
            System.out.println(result);
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static String jsonStr(String s) {
        return Json.str(s);
    }
}

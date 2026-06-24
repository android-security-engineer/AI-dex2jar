package com.googlecode.d2j.ai;

import com.googlecode.d2j.Field;
import com.googlecode.d2j.Method;
import com.googlecode.d2j.node.*;
import com.googlecode.d2j.node.insn.DexStmtNode;
import com.googlecode.d2j.node.insn.FieldStmtNode;
import com.googlecode.d2j.node.insn.MethodStmtNode;
import com.googlecode.d2j.reader.BaseDexFileReader;
import com.googlecode.d2j.reader.MultiDexFileReader;
import com.googlecode.dex2jar.tools.BaseCmd;

import java.nio.file.*;
import java.util.*;

/**
 * Analyze class dependencies in a DEX/APK file.
 *
 * <p>For each class, reports the other types it depends on: superclass,
 * implemented interfaces, field types, and (with --deep) types referenced by
 * method bodies (invoked methods' owners, accessed fields' owners).
 */
public class DexClassDepsCmd extends BaseCmd {

    @Opt(opt = "c", longOpt = "class", description = "Only analyze classes matching this pattern (substring)")
    String classFilter;

    @Opt(opt = "d", longOpt = "deep", hasArg = false, description = "Include dependencies from method bodies (invokes, field access)")
    boolean deep;

    @Opt(opt = "i", longOpt = "internal-only", hasArg = false, description = "Only show dependencies on classes defined in this DEX")
    boolean internalOnly;

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

        DexFileNode fileNode = new DexFileNode();
        reader.accept(fileNode, deep ? 0 : com.googlecode.d2j.reader.DexFileReader.SKIP_CODE);

        // Collect set of internal class names first
        Set<String> internal = new HashSet<>();
        if (fileNode.clzs != null) {
            for (DexClassNode cn : fileNode.clzs) {
                internal.add(cn.className);
            }
        }

        // className -> sorted set of dependency types
        Map<String, TreeSet<String>> deps = new LinkedHashMap<>();

        if (fileNode.clzs != null) {
            for (DexClassNode cn : fileNode.clzs) {
                if (classFilter != null && !classFilter.isEmpty() && !cn.className.contains(classFilter)) {
                    continue;
                }
                TreeSet<String> d = new TreeSet<>();

                if (cn.superClass != null) addDep(d, cn.superClass, cn.className, internal);
                if (cn.interfaceNames != null) {
                    for (String iface : cn.interfaceNames) addDep(d, iface, cn.className, internal);
                }
                if (cn.fields != null) {
                    for (DexFieldNode fn : cn.fields) {
                        if (fn.field != null) addDep(d, fn.field.getType(), cn.className, internal);
                    }
                }
                if (cn.methods != null) {
                    for (DexMethodNode mn : cn.methods) {
                        if (mn.method != null) {
                            String[] ps = mn.method.getParameterTypes();
                            if (ps != null) for (String p : ps) addDep(d, p, cn.className, internal);
                            addDep(d, mn.method.getReturnType(), cn.className, internal);
                        }
                        if (deep && mn.codeNode != null && mn.codeNode.stmts != null) {
                            for (DexStmtNode stmt : mn.codeNode.stmts) {
                                if (stmt instanceof MethodStmtNode) {
                                    Method m = ((MethodStmtNode) stmt).method;
                                    if (m != null) addDep(d, m.getOwner(), cn.className, internal);
                                } else if (stmt instanceof FieldStmtNode) {
                                    Field f = ((FieldStmtNode) stmt).field;
                                    if (f != null) addDep(d, f.getOwner(), cn.className, internal);
                                }
                            }
                        }
                    }
                }
                deps.put(cn.className, d);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"class_count\": ").append(deps.size()).append(",\n");
        sb.append("  \"deep\": ").append(deep).append(",\n");
        sb.append("  \"dependencies\": {");
        boolean first = true;
        for (Map.Entry<String, TreeSet<String>> e : deps.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\n    ").append(jsonStr(e.getKey())).append(": [");
            boolean f2 = true;
            for (String dep : e.getValue()) {
                if (!f2) sb.append(", ");
                f2 = false;
                sb.append(jsonStr(dep));
            }
            sb.append("]");
        }
        if (!deps.isEmpty()) sb.append("\n  ");
        sb.append("}\n}");

        emit(sb.toString());
    }

    /** Normalize a type descriptor to a class name and add it if it's a class dependency. */
    private void addDep(TreeSet<String> d, String type, String self, Set<String> internal) {
        if (type == null) return;
        // Unwrap array dimensions
        String t = type;
        while (t.startsWith("[")) t = t.substring(1);
        // Only object types (Lxxx;) are class dependencies; skip primitives
        if (!t.startsWith("L") || !t.endsWith(";")) return;
        if (t.equals(self)) return; // skip self-reference
        if (internalOnly && !internal.contains(t)) return;
        d.add(t);
    }

    private void emit(String result) throws Exception {
        if (output != null && !output.isEmpty()) {
            Files.write(Paths.get(output), result.getBytes("UTF-8"));
            System.out.println("Written to " + output);
        } else {
            System.out.println(result);
        }
    }

    private static String jsonStr(String s) {
        return Json.str(s);
    }
}

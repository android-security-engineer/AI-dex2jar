package com.googlecode.d2j.ai;

import com.googlecode.d2j.Method;
import com.googlecode.d2j.node.*;
import com.googlecode.d2j.node.insn.DexStmtNode;
import com.googlecode.d2j.node.insn.MethodStmtNode;
import com.googlecode.d2j.reader.BaseDexFileReader;
import com.googlecode.d2j.reader.MultiDexFileReader;
import com.googlecode.dex2jar.tools.BaseCmd;

import java.nio.file.*;
import java.util.*;

/**
 * Build a method call graph from a DEX/APK file.
 *
 * <p>For each method, lists the methods it invokes. Optionally restrict to
 * callers/callees matching a target signature to answer "who calls X?".
 */
public class DexMethodTraceCmd extends BaseCmd {

    @Opt(opt = "c", longOpt = "class", description = "Only trace methods in classes matching this pattern (substring)")
    String classFilter;

    @Opt(opt = "t", longOpt = "target", description = "Find all callers of methods matching this pattern (substring on owner.name)")
    String target;

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
        reader.accept(fileNode, 0); // need code

        // caller signature -> set of callee signatures
        List<String[]> edges = new ArrayList<>();
        int edgeCount = 0;

        if (fileNode.clzs != null) {
            for (DexClassNode cn : fileNode.clzs) {
                if (classFilter != null && !classFilter.isEmpty() && !cn.className.contains(classFilter)) {
                    continue;
                }
                if (cn.methods == null) continue;
                for (DexMethodNode mn : cn.methods) {
                    if (mn.codeNode == null || mn.codeNode.stmts == null) continue;
                    String caller = sig(mn.method);
                    for (DexStmtNode stmt : mn.codeNode.stmts) {
                        if (stmt instanceof MethodStmtNode) {
                            Method callee = ((MethodStmtNode) stmt).method;
                            if (callee == null) continue;
                            String calleeSig = sig(callee);
                            if (target != null && !target.isEmpty()) {
                                String needle = callee.getOwner() + "." + callee.getName();
                                if (!needle.contains(target)) continue;
                            }
                            edges.add(new String[]{caller, calleeSig});
                            edgeCount++;
                        }
                    }
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"edge_count\": ").append(edgeCount).append(",\n");
        if (target != null && !target.isEmpty()) {
            sb.append("  \"target\": ").append(jsonStr(target)).append(",\n");
        }
        sb.append("  \"edges\": [");
        for (int i = 0; i < edges.size(); i++) {
            if (i > 0) sb.append(",");
            String[] e = edges.get(i);
            sb.append("\n    {\"caller\": ").append(jsonStr(e[0]))
              .append(", \"callee\": ").append(jsonStr(e[1])).append("}");
        }
        if (!edges.isEmpty()) sb.append("\n  ");
        sb.append("]\n}");

        emit(sb.toString());
    }

    private void emit(String result) throws Exception {
        if (output != null && !output.isEmpty()) {
            Files.write(Paths.get(output), result.getBytes("UTF-8"));
            System.out.println("Written to " + output);
        } else {
            System.out.println(result);
        }
    }

    private static String sig(Method m) {
        if (m == null) return "?";
        StringBuilder sb = new StringBuilder();
        sb.append(m.getOwner()).append("->").append(m.getName()).append("(");
        String[] ps = m.getParameterTypes();
        if (ps != null) {
            for (String p : ps) sb.append(p);
        }
        sb.append(")").append(m.getReturnType() == null ? "" : m.getReturnType());
        return sb.toString();
    }

    private static String jsonStr(String s) {
        return Json.str(s);
    }
}

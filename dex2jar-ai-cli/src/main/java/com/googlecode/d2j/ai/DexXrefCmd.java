package com.googlecode.d2j.ai;

import com.googlecode.d2j.Field;
import com.googlecode.d2j.Method;
import com.googlecode.d2j.node.*;
import com.googlecode.d2j.node.insn.ConstStmtNode;
import com.googlecode.d2j.node.insn.DexStmtNode;
import com.googlecode.d2j.node.insn.FieldStmtNode;
import com.googlecode.d2j.node.insn.MethodStmtNode;
import com.googlecode.d2j.node.insn.TypeStmtNode;
import com.googlecode.d2j.reader.BaseDexFileReader;
import com.googlecode.d2j.reader.MultiDexFileReader;
import com.googlecode.dex2jar.tools.BaseCmd;

import java.nio.file.*;
import java.util.*;

/**
 * Reverse cross-reference: find every site that references a target symbol.
 *
 * <p>Method-trace answers "what does method X call?"; xref answers the inverse —
 * "where is symbol S used, and from which enclosing method?". A reference is any
 * invoke target, field access, type usage (new-instance / check-cast / …), or
 * string constant whose textual form contains the query.
 *
 * <p>The query is either a free-text substring (<code>--to</code>) or a named
 * preset of sensitive-API indicators (<code>--preset crypto|reflection|dynload|net</code>),
 * which expands to a list of substrings that are OR-matched. Presets turn
 * "see the structure" into "find the sink" — the first move in most malware triage.
 */
public class DexXrefCmd extends BaseCmd {

    @Opt(opt = "t", longOpt = "to", description = "Match references whose symbol contains this substring")
    String to;

    @Opt(opt = "p", longOpt = "preset",
            description = "Sensitive-API preset: crypto | reflection | dynload | net")
    String preset;

    @Opt(opt = "k", longOpt = "kinds",
            description = "Comma-separated reference kinds to include: invoke,field,type,string (default: all)")
    String kinds;

    @Opt(opt = "o", longOpt = "output", description = "Output file path (default: stdout)")
    String output;

    private static final Map<String, String[]> PRESETS = new LinkedHashMap<>();

    static {
        PRESETS.put("crypto", new String[]{
                "Ljavax/crypto/", "Ljava/security/", "Cipher", "MessageDigest",
                "SecretKeySpec", "IvParameterSpec", "Mac", "KeyGenerator",
                "KeyPairGenerator", "Signature", "PBEKeySpec",
        });
        PRESETS.put("reflection", new String[]{
                "Ljava/lang/reflect/", "Ljava/lang/Class;->forName", "->getMethod",
                "->getDeclaredMethod", "->getDeclaredField", "->getField",
                "Ljava/lang/reflect/Method;->invoke", "->setAccessible",
        });
        PRESETS.put("dynload", new String[]{
                "Ldalvik/system/DexClassLoader", "Ldalvik/system/PathClassLoader",
                "Ldalvik/system/BaseDexClassLoader", "Ldalvik/system/InMemoryDexClassLoader",
                "Ljava/lang/System;->load", "Ljava/lang/Runtime;->load",
                "->loadLibrary", "Ljava/lang/ClassLoader",
        });
        PRESETS.put("net", new String[]{
                "Ljava/net/", "Ljavax/net/", "Lokhttp3/", "Lcom/squareup/okhttp",
                "Lorg/apache/http", "HttpURLConnection", "->openConnection",
                "Landroid/webkit/WebView", "Ljava/net/Socket", "http://", "https://",
        });
    }

    @Override
    protected void doCommandLine() throws Exception {
        if (remainingArgs == null || remainingArgs.length == 0) {
            throw new HelpException("No input file specified");
        }

        String[] needles;
        String queryLabel;
        if (preset != null && !preset.isEmpty()) {
            String[] p = PRESETS.get(preset.toLowerCase(Locale.ROOT));
            if (p == null) {
                System.err.println("Unknown preset: " + preset
                        + " (expected one of " + PRESETS.keySet() + ")");
                return;
            }
            needles = p;
            queryLabel = "preset:" + preset.toLowerCase(Locale.ROOT);
        } else if (to != null && !to.isEmpty()) {
            needles = new String[]{to};
            queryLabel = to;
        } else {
            throw new HelpException("Specify either --to <substring> or --preset <name>");
        }

        Set<String> wantKinds = parseKinds(kinds);

        Path input = Paths.get(remainingArgs[0]);
        if (!Files.exists(input)) {
            System.err.println("File not found: " + input);
            return;
        }

        byte[] data = Files.readAllBytes(input);
        BaseDexFileReader reader = MultiDexFileReader.open(data);
        DexFileNode fileNode = new DexFileNode();
        reader.accept(fileNode, 0); // need code

        List<Json.Obj> matches = new ArrayList<>();

        if (fileNode.clzs != null) {
            for (DexClassNode cn : fileNode.clzs) {
                if (cn.methods == null) {
                    continue;
                }
                for (DexMethodNode mn : cn.methods) {
                    if (mn.codeNode == null || mn.codeNode.stmts == null) {
                        continue;
                    }
                    String inMethod = sig(mn.method);
                    for (DexStmtNode stmt : mn.codeNode.stmts) {
                        String kind = null;
                        String symbol = null;

                        if (stmt instanceof MethodStmtNode) {
                            Method m = ((MethodStmtNode) stmt).method;
                            if (m != null) {
                                kind = "invoke";
                                symbol = m.getOwner() + "->" + m.getName();
                            }
                        } else if (stmt instanceof FieldStmtNode) {
                            Field f = ((FieldStmtNode) stmt).field;
                            if (f != null) {
                                kind = "field";
                                symbol = f.getOwner() + "->" + f.getName() + ":" + f.getType();
                            }
                        } else if (stmt instanceof TypeStmtNode) {
                            kind = "type";
                            symbol = ((TypeStmtNode) stmt).type;
                        } else if (stmt instanceof ConstStmtNode) {
                            Object v = ((ConstStmtNode) stmt).value;
                            if (v instanceof String) {
                                kind = "string";
                                symbol = (String) v;
                            }
                        }

                        if (kind == null || symbol == null) {
                            continue;
                        }
                        if (!wantKinds.contains(kind)) {
                            continue;
                        }
                        if (!matchesAny(symbol, needles)) {
                            continue;
                        }

                        Json.Obj match = Json.obj()
                                .put("kind", kind)
                                .put("symbol", symbol)
                                .put("in_method", inMethod)
                                .put("op", stmt.op == null ? "?" : stmt.op.toString());
                        matches.add(match);
                    }
                }
            }
        }

        Json.Arr arr = Json.arr();
        for (Json.Obj m : matches) {
            arr.addRaw(m.build());
        }
        String result = Json.obj()
                .put("query", queryLabel)
                .put("match_count", matches.size())
                .putRaw("matches", arr.build())
                .build();

        emit(result);
    }

    private static Set<String> parseKinds(String kinds) {
        Set<String> all = new LinkedHashSet<>(Arrays.asList("invoke", "field", "type", "string"));
        if (kinds == null || kinds.isEmpty()) {
            return all;
        }
        Set<String> want = new LinkedHashSet<>();
        for (String k : kinds.split(",")) {
            String t = k.trim().toLowerCase(Locale.ROOT);
            if (all.contains(t)) {
                want.add(t);
            }
        }
        return want.isEmpty() ? all : want;
    }

    private static boolean matchesAny(String haystack, String[] needles) {
        for (String n : needles) {
            if (haystack.contains(n)) {
                return true;
            }
        }
        return false;
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
        if (m == null) {
            return "?";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(m.getOwner()).append("->").append(m.getName()).append("(");
        String[] ps = m.getParameterTypes();
        if (ps != null) {
            for (String p : ps) {
                sb.append(p);
            }
        }
        sb.append(")").append(m.getReturnType() == null ? "" : m.getReturnType());
        return sb.toString();
    }
}

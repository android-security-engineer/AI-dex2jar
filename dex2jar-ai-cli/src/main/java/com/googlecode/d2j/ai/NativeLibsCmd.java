package com.googlecode.d2j.ai;

import com.googlecode.dex2jar.tools.BaseCmd;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Enumerate the native libraries bundled in an APK ({@code lib/<abi>/*.so}) and, for
 * each, report its ABI, size, ELF identity, and exported JNI entry points
 * ({@code JNI_OnLoad} and {@code Java_*} bridge functions).
 *
 * <p>This maps the native attack surface: which ABIs are shipped, and which Java
 * classes/methods cross into native code. The {@code Java_*} symbol names decode
 * directly to the bound Java method, pointing at exactly where to start when the logic
 * you care about has been pushed into C/C++.
 *
 * <p>Also accepts a standalone {@code .so} file. ELF parsing is self-contained
 * ({@link ElfSymbols}); no NDK/binutils required.
 */
public class NativeLibsCmd extends BaseCmd {

    @Opt(opt = "s", longOpt = "symbols", hasArg = false,
            description = "Include the full list of JNI symbol names per library")
    boolean withSymbols;

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

        List<String> libObjs = new ArrayList<>();
        Set<String> abis = new LinkedHashSet<>();
        int totalJni = 0;

        String lower = input.getFileName().toString().toLowerCase();
        if (lower.endsWith(".so")) {
            byte[] data = Files.readAllBytes(input);
            ElfSymbols.Info info = ElfSymbols.read(data);
            libObjs.add(libToJson(input.getFileName().toString(), "", data.length, info));
            totalJni += info.jniSymbols.size();
        } else {
            try (ZipFile zf = new ZipFile(input.toFile())) {
                Enumeration<? extends ZipEntry> entries = zf.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry e = entries.nextElement();
                    String n = e.getName();
                    if (e.isDirectory() || !n.startsWith("lib/") || !n.endsWith(".so")) {
                        continue;
                    }
                    String[] parts = n.split("/");
                    String abi = parts.length >= 3 ? parts[1] : "";
                    if (!abi.isEmpty()) {
                        abis.add(abi);
                    }
                    byte[] data;
                    try (InputStream in = zf.getInputStream(e)) {
                        data = drain(in);
                    }
                    ElfSymbols.Info info = ElfSymbols.read(data);
                    libObjs.add(libToJson(n, abi, data.length, info));
                    totalJni += info.jniSymbols.size();
                }
            }
        }

        Json.Arr abiArr = Json.arr();
        for (String a : abis) {
            abiArr.add(a);
        }
        Json.Arr libArr = Json.arr();
        for (String l : libObjs) {
            libArr.addRaw(l);
        }

        String result = Json.obj()
                .put("native_lib_count", libObjs.size())
                .putRaw("abis", abiArr.build())
                .put("total_jni_symbols", totalJni)
                .putRaw("libraries", libArr.build())
                .build();

        emit(result);
    }

    private String libToJson(String path, String abi, int size, ElfSymbols.Info info) {
        Json.Obj o = Json.obj()
                .put("path", path)
                .put("abi", abi)
                .put("size_bytes", size)
                .put("is_elf", info.isElf)
                .put("elf_class", info.elfClass)
                .put("byte_order", info.dataOrder)
                .put("machine", info.machine)
                .put("dynsym_count", info.dynsymCount)
                .put("has_jni_onload", info.hasJniOnLoad)
                .put("jni_symbol_count", info.jniSymbols.size());
        if (withSymbols) {
            Json.Arr syms = Json.arr();
            for (String s : info.jniSymbols) {
                syms.add(s);
            }
            o.putRaw("jni_symbols", syms.build());
        }
        return o.build();
    }

    private static byte[] drain(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int r;
        while ((r = in.read(buf)) != -1) {
            bos.write(buf, 0, r);
        }
        return bos.toByteArray();
    }

    private void emit(String result) throws Exception {
        if (output != null && !output.isEmpty()) {
            Files.write(Paths.get(output), result.getBytes("UTF-8"));
            System.out.println("Written to " + output);
        } else {
            System.out.println(result);
        }
    }
}

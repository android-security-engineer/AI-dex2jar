package com.googlecode.d2j.ai;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal, dependency-free ELF reader that extracts the <em>defined exported</em>
 * symbols from the dynamic symbol table ({@code .dynsym} / {@code .dynstr}).
 *
 * <p>For a JNI {@code .so} the interesting exports are {@code JNI_OnLoad} and the
 * {@code Java_<pkg>_<Class>_<method>} bridge functions — together they reveal which
 * native entry points the Java side can reach. This reader parses both ELF32 and
 * ELF64, little- and big-endian, via the section-header table.
 *
 * <p>Format reference: System V ABI, ELF specification.
 */
public final class ElfSymbols {

    /** Parsed identity + symbol summary of one ELF object. */
    public static final class Info {
        public String elfClass = "?";   // ELF32 / ELF64
        public String dataOrder = "?";  // LE / BE
        public String machine = "?";    // arm / aarch64 / x86 / x86_64 / ...
        public boolean isElf;
        public final List<String> jniSymbols = new ArrayList<>();
        public boolean hasJniOnLoad;
        public int dynsymCount;
    }

    private static final int SHT_DYNSYM = 11;

    private ElfSymbols() {
    }

    public static Info read(byte[] data) {
        Info info = new Info();
        if (data.length < 64
                || (data[0] & 0xFF) != 0x7F || data[1] != 'E' || data[2] != 'L' || data[3] != 'F') {
            return info;
        }
        info.isElf = true;

        boolean is64 = data[4] == 2;
        boolean isLE = data[5] == 1;
        info.elfClass = is64 ? "ELF64" : "ELF32";
        info.dataOrder = isLE ? "LE" : "BE";

        ByteBuffer b = ByteBuffer.wrap(data).order(isLE ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);

        info.machine = machineName(b.getShort(18) & 0xFFFF);

        long shoff;
        int shentsize;
        int shnum;
        if (is64) {
            shoff = b.getLong(40);
            shentsize = b.getShort(58) & 0xFFFF;
            shnum = b.getShort(60) & 0xFFFF;
        } else {
            shoff = b.getInt(32) & 0xFFFFFFFFL;
            shentsize = b.getShort(46) & 0xFFFF;
            shnum = b.getShort(48) & 0xFFFF;
        }
        if (shoff == 0 || shnum == 0) {
            return info;
        }

        // Find the .dynsym section and its linked string table.
        for (int i = 0; i < shnum; i++) {
            long sh = shoff + (long) i * shentsize;
            if (sh + shentsize > data.length) {
                break;
            }
            int type = b.getInt((int) (sh + 4));
            if (type != SHT_DYNSYM) {
                continue;
            }

            long symOff;
            long symSize;
            int link;
            long entSize;
            if (is64) {
                symOff = b.getLong((int) (sh + 24));
                symSize = b.getLong((int) (sh + 32));
                link = b.getInt((int) (sh + 40));
                entSize = b.getLong((int) (sh + 56));
            } else {
                symOff = b.getInt((int) (sh + 16)) & 0xFFFFFFFFL;
                symSize = b.getInt((int) (sh + 20)) & 0xFFFFFFFFL;
                link = b.getInt((int) (sh + 24));
                entSize = b.getInt((int) (sh + 36)) & 0xFFFFFFFFL;
            }
            if (entSize == 0) {
                entSize = is64 ? 24 : 16;
            }

            // The .dynstr is the section pointed to by sh_link.
            long strSh = shoff + (long) link * shentsize;
            long strOff;
            long strSize;
            if (is64) {
                strOff = b.getLong((int) (strSh + 24));
                strSize = b.getLong((int) (strSh + 32));
            } else {
                strOff = b.getInt((int) (strSh + 16)) & 0xFFFFFFFFL;
                strSize = b.getInt((int) (strSh + 20)) & 0xFFFFFFFFL;
            }

            int count = (int) (symSize / entSize);
            info.dynsymCount = count;
            for (int s = 0; s < count; s++) {
                long sym = symOff + (long) s * entSize;
                if (sym + entSize > data.length) {
                    break;
                }
                int nameIdx;
                int shndx;
                if (is64) {
                    nameIdx = b.getInt((int) sym);
                    shndx = b.getShort((int) (sym + 6)) & 0xFFFF;
                } else {
                    nameIdx = b.getInt((int) sym);
                    shndx = b.getShort((int) (sym + 14)) & 0xFFFF;
                }
                if (shndx == 0) {
                    continue; // SHN_UNDEF — imported, not exported
                }
                String name = readCStr(data, (int) (strOff + nameIdx), (int) strSize - nameIdx);
                if (name == null || name.isEmpty()) {
                    continue;
                }
                if (name.equals("JNI_OnLoad") || name.equals("JNI_OnUnload")) {
                    info.hasJniOnLoad = info.hasJniOnLoad || name.equals("JNI_OnLoad");
                    info.jniSymbols.add(name);
                } else if (name.startsWith("Java_")) {
                    info.jniSymbols.add(name);
                }
            }
            break; // one .dynsym is enough
        }
        return info;
    }

    private static String readCStr(byte[] data, int off, int maxLen) {
        if (off < 0 || off >= data.length) {
            return null;
        }
        int end = off;
        int limit = Math.min(data.length, off + Math.max(0, maxLen));
        while (end < limit && data[end] != 0) {
            end++;
        }
        return new String(data, off, end - off);
    }

    private static String machineName(int em) {
        switch (em) {
            case 3:
                return "x86";
            case 40:
                return "arm";
            case 62:
                return "x86_64";
            case 183:
                return "aarch64";
            case 8:
                return "mips";
            default:
                return "machine(" + em + ")";
        }
    }
}

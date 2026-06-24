package com.googlecode.d2j.ai;

import java.util.HashMap;
import java.util.Map;

/**
 * Fallback map from well-known android framework attribute resource IDs to their
 * names, used when an AXML attribute's name string is empty (some toolchains omit
 * the name string and rely on the resource-map id alone).
 *
 * <p>Only the handful of attributes relevant to manifest triage are listed; an
 * unknown id falls back to its hex form, which is still greppable.
 */
final class AndroidAttrs {

    private static final Map<Integer, String> NAMES = new HashMap<>();

    static {
        NAMES.put(0x01010003, "name");
        NAMES.put(0x01010006, "permission");
        NAMES.put(0x01010010, "exported");
        NAMES.put(0x01010270, "targetSdkVersion");
        NAMES.put(0x0101020c, "minSdkVersion");
        NAMES.put(0x0101021b, "versionCode");
        NAMES.put(0x0101021c, "versionName");
        NAMES.put(0x01010001, "label");
        NAMES.put(0x01010000, "theme");
        NAMES.put(0x01010002, "icon");
        NAMES.put(0x0101000f, "enabled");
        NAMES.put(0x01010272, "debuggable"); // android:debuggable
        NAMES.put(0x01010280, "allowBackup");
        NAMES.put(0x010103a3, "usesCleartextTraffic"); // android:usesCleartextTraffic
        NAMES.put(0x0101048d, "networkSecurityConfig");
        NAMES.put(0x01010026, "process");
        NAMES.put(0x01010019, "authorities");
        NAMES.put(0x01010014, "grantUriPermissions");
    }

    private AndroidAttrs() {
    }

    static String name(int resId) {
        return NAMES.get(resId);
    }
}

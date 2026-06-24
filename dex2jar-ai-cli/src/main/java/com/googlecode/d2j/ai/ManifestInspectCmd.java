package com.googlecode.d2j.ai;

import com.googlecode.dex2jar.tools.BaseCmd;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Inspect an APK's binary {@code AndroidManifest.xml} and report the attributes
 * that matter for triage: package identity, SDK levels, requested permissions,
 * and every exported component (the app's attack surface).
 *
 * <p>Accepts either an {@code .apk} (the manifest is extracted from the zip) or a
 * standalone binary {@code AndroidManifest.xml}. The parser is self-contained
 * ({@link AxmlParser}); no aapt or Android SDK is required.
 */
public class ManifestInspectCmd extends BaseCmd {

    @Opt(opt = "o", longOpt = "output", description = "Output file path (default: stdout)")
    String output;

    @Opt(opt = "e", longOpt = "exported-only", hasArg = false,
            description = "Only report exported components (the attack surface)")
    boolean exportedOnly;

    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

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

        byte[] manifest = readManifestBytes(input);
        if (manifest == null) {
            System.err.println("No AndroidManifest.xml found in " + input);
            return;
        }

        ManifestModel model = new ManifestModel();
        new AxmlParser(manifest).parse(model);

        emit(model.toJson(exportedOnly));
    }

    /** Read AndroidManifest.xml from an APK zip, or treat the input as a raw AXML file. */
    private static byte[] readManifestBytes(Path input) throws Exception {
        // Probe the first bytes: AXML files start with chunk type 0x0003 (LE: 03 00).
        byte[] head = new byte[2];
        try (InputStream in = Files.newInputStream(input)) {
            int n = in.read(head);
            if (n == 2 && (head[0] & 0xFF) == 0x03 && (head[1] & 0xFF) == 0x00) {
                return Files.readAllBytes(input);
            }
        }
        // Otherwise treat as a zip/APK.
        try (ZipFile zf = new ZipFile(input.toFile())) {
            ZipEntry e = zf.getEntry("AndroidManifest.xml");
            if (e != null) {
                try (InputStream in = zf.getInputStream(e)) {
                    return drain(in);
                }
            }
        } catch (Exception zipErr) {
            // Fall through to a streaming scan for odd zips.
            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(input))) {
                ZipEntry e;
                while ((e = zis.getNextEntry()) != null) {
                    if ("AndroidManifest.xml".equals(e.getName())) {
                        return drain(zis);
                    }
                }
            }
        }
        return null;
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

    /** Accumulates manifest facts via AxmlParser events. */
    private static final class ManifestModel implements AxmlParser.Handler {
        String pkg;
        String versionCode;
        String versionName;
        String minSdk;
        String targetSdk;
        String compileSdk;
        Boolean debuggable;
        Boolean allowBackup;
        String usesCleartext;
        final List<String> permissions = new ArrayList<>();
        final List<Component> components = new ArrayList<>();

        // Walk state.
        Component current;
        boolean inApplication;

        @Override
        public void startElement(String name, List<AxmlParser.Attribute> attrs) {
            switch (name) {
                case "manifest":
                    pkg = attr(attrs, "package");
                    versionCode = androidAttr(attrs, "versionCode");
                    versionName = androidAttr(attrs, "versionName");
                    String cs = attr(attrs, "compileSdkVersion");
                    if (cs == null) {
                        cs = androidAttr(attrs, "compileSdkVersion");
                    }
                    compileSdk = cs;
                    break;
                case "uses-sdk":
                    minSdk = androidAttr(attrs, "minSdkVersion");
                    targetSdk = androidAttr(attrs, "targetSdkVersion");
                    break;
                case "uses-permission":
                case "uses-permission-sdk-23":
                    String p = androidAttr(attrs, "name");
                    if (p != null) {
                        permissions.add(p);
                    }
                    break;
                case "application":
                    inApplication = true;
                    debuggable = boolAttr(attrs, "debuggable");
                    allowBackup = boolAttr(attrs, "allowBackup");
                    usesCleartext = androidAttr(attrs, "usesCleartextTraffic");
                    break;
                case "activity":
                case "activity-alias":
                case "service":
                case "receiver":
                case "provider":
                    current = new Component();
                    current.type = name;
                    current.name = androidAttr(attrs, "name");
                    current.permission = androidAttr(attrs, "permission");
                    current.exportedExplicit = androidAttr(attrs, "exported");
                    components.add(current);
                    break;
                case "intent-filter":
                    if (current != null) {
                        current.hasIntentFilter = true;
                    }
                    break;
                case "action":
                    if (current != null) {
                        String a = androidAttr(attrs, "name");
                        if (a != null) {
                            current.actions.add(a);
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        public void endElement(String name) {
            switch (name) {
                case "activity":
                case "activity-alias":
                case "service":
                case "receiver":
                case "provider":
                    current = null;
                    break;
                case "application":
                    inApplication = false;
                    break;
                default:
                    break;
            }
        }

        private static String attr(List<AxmlParser.Attribute> attrs, String name) {
            for (AxmlParser.Attribute a : attrs) {
                if (name.equals(a.name) && (a.namespace == null || a.namespace.isEmpty())) {
                    return a.value;
                }
            }
            return null;
        }

        private static String androidAttr(List<AxmlParser.Attribute> attrs, String name) {
            // Prefer the android-namespaced attribute, but accept an unnamespaced
            // match too (covered by the resource-id fallback path).
            String fallback = null;
            for (AxmlParser.Attribute a : attrs) {
                if (name.equals(a.name)) {
                    if (ANDROID_NS.equals(a.namespace)) {
                        return a.value;
                    }
                    fallback = a.value;
                }
            }
            return fallback;
        }

        private static Boolean boolAttr(List<AxmlParser.Attribute> attrs, String name) {
            String v = androidAttr(attrs, name);
            if (v == null) {
                return null;
            }
            return "true".equalsIgnoreCase(v) || "1".equals(v);
        }

        String toJson(boolean exportedOnly) {
            Json.Arr permArr = Json.arr();
            for (String p : permissions) {
                permArr.add(p);
            }

            Json.Arr compArr = Json.arr();
            int exportedCount = 0;
            for (Component c : components) {
                boolean exported = c.isExported();
                if (exported) {
                    exportedCount++;
                }
                if (exportedOnly && !exported) {
                    continue;
                }
                Json.Arr actionArr = Json.arr();
                for (String a : c.actions) {
                    actionArr.add(a);
                }
                Json.Obj co = Json.obj()
                        .put("type", c.type)
                        .put("name", c.name == null ? "" : c.name)
                        .put("exported", exported)
                        .put("exported_source", c.exportedExplicit != null ? "explicit" :
                                (c.hasIntentFilter ? "implicit (intent-filter)" : "default"))
                        .put("has_intent_filter", c.hasIntentFilter);
                if (c.permission != null) {
                    co.put("permission", c.permission);
                }
                co.putRaw("actions", actionArr.build());
                compArr.addRaw(co.build());
            }

            Json.Obj root = Json.obj()
                    .put("package", pkg == null ? "" : pkg);
            if (versionCode != null) {
                root.put("version_code", versionCode);
            }
            if (versionName != null) {
                root.put("version_name", versionName);
            }
            if (minSdk != null) {
                root.put("min_sdk", minSdk);
            }
            if (targetSdk != null) {
                root.put("target_sdk", targetSdk);
            }
            if (compileSdk != null) {
                root.put("compile_sdk", compileSdk);
            }
            if (debuggable != null) {
                root.put("debuggable", debuggable.booleanValue());
            }
            if (allowBackup != null) {
                root.put("allow_backup", allowBackup.booleanValue());
            }
            if (usesCleartext != null) {
                root.put("uses_cleartext_traffic", usesCleartext);
            }
            root.put("permission_count", permissions.size());
            root.putRaw("permissions", permArr.build());
            root.put("component_count", components.size());
            root.put("exported_count", exportedCount);
            root.putRaw("components", compArr.build());
            return root.build();
        }
    }

    private static final class Component {
        String type;
        String name;
        String permission;
        String exportedExplicit; // "true"/"false"/null
        boolean hasIntentFilter;
        final List<String> actions = new ArrayList<>();

        boolean isExported() {
            if (exportedExplicit != null) {
                return "true".equalsIgnoreCase(exportedExplicit) || "1".equals(exportedExplicit);
            }
            // No explicit flag: a component with an intent-filter is exported by default
            // (pre-Android-12 semantics; providers default to exported only via filters too).
            return hasIntentFilter;
        }
    }
}

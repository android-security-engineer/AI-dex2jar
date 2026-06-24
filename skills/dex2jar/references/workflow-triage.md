# Workflow: APK Triage (first pass)

Goal: in a few cheap commands, answer "what is this app, who made it, and where is
the interesting code?" — before committing to a full decompile.

Run these in order; each is independent, so you can also fan them out in parallel.

## 1. Identity & attack surface — `manifest-inspect`

```bash
python3 d2j-ai.py manifest-inspect app.apk -e --pretty
```

Tells you package name, version, min/target/compile SDK, the `debuggable` /
`allowBackup` flags, declared permissions, and every **exported** component
(activities/services/receivers/providers) with its guarding permission and intent
actions. `debuggable=true`, `allowBackup=true`, or an exported component with no
permission are immediate findings. Drop `-e` to see all components.

## 2. Who signed it — `apk-cert`

```bash
python3 d2j-ai.py apk-cert app.apk --pretty
```

Subject/issuer, validity window, key type/size, and SHA-256/SHA-1/MD5
fingerprints. The fingerprint is the signer's identity — use it to cluster
same-author samples or to confirm a repackaged app was re-signed with a different
key than the original.

## 3. Native entry points — `native-libs`

```bash
python3 d2j-ai.py native-libs app.apk -s --pretty
```

Per-ABI ELF identity plus exported JNI symbols (`JNI_OnLoad` and every
`Java_<pkg>_<Class>_<method>` bridge). When the interesting logic has been pushed
into C/C++, the `Java_*` names point at exactly where execution leaves Java.

## 4. Indicators — `dex-strings`

```bash
python3 d2j-ai.py dex-strings app.apk -u --pretty            # all unique strings
python3 d2j-ai.py dex-strings app.apk -f http --pretty        # URLs/endpoints
```

URLs, endpoints, key material, log tags, and feature flags surface here without
any decompilation.

## Then decide

- Sensitive behavior suspected → `sensitive-api-audit` skill (`dex-xref` presets).
- Need to read the code → convert with `dex2jar` (see `dex-to-jar-accuracy.md`).
- Structure questions → `dex-inspect` / `dex-class-deps`.

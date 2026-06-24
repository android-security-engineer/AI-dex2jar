---
name: apk-triage
description: "Fast first-pass triage of an unknown or suspicious Android APK — answer 'what is this app, who signed it, what's the attack surface, is anything suspicious?' without a full decompile. Use when the user hands over an APK and asks whether it's safe/malicious, what permissions or exported components it has, who signed it, what it ships, or for an initial recon/overview before deeper analysis. Triggers include: triage this apk, is this apk safe, is this malware, what does this app request, what's the attack surface, who signed this, quick look at this apk, 这个apk安全吗, 这个应用有没有问题, 先看一下这个apk, 它申请了什么权限, 谁签名的. Use proactively as the first step for any unfamiliar APK."
license: Apache-2.0
---

# apk-triage — First-Pass APK Recon

Quickly characterize an unknown APK with four cheap, self-contained commands —
no decompilation, no aapt/NDK. All run through `python3 d2j-ai.py … --pretty`.

Run them (in order, or fan out in parallel) and synthesize one short verdict:

## 1. Attack surface — `manifest-inspect`

```bash
python3 d2j-ai.py manifest-inspect app.apk -e --pretty
```

Package, version, SDK levels, `debuggable` / `allowBackup`, permissions, and every
**exported** component with guarding permission + intent actions. Flag
`debuggable=true`, `allowBackup=true`, and exported components with no permission.

## 2. Signer identity — `apk-cert`

```bash
python3 d2j-ai.py apk-cert app.apk --pretty
```

Subject/issuer, validity, key size, and SHA-256/SHA-1/MD5 fingerprints. Note
self-signed certs, debug certs (`CN=Android Debug`), implausible validity windows,
and use the fingerprint to cluster same-author samples.

## 3. Native code — `native-libs`

```bash
python3 d2j-ai.py native-libs app.apk -s --pretty
```

Per-ABI ELF identity + exported JNI symbols. Presence of native libs (especially
with few/obfuscated `Java_*` names) signals logic pushed into C/C++.

## 4. Indicators — `dex-strings`

```bash
python3 d2j-ai.py dex-strings app.apk -u --pretty
python3 d2j-ai.py dex-strings app.apk -f http --pretty
```

URLs, endpoints, keys, log tags. Quick way to spot C2 hosts, hardcoded secrets,
or telltale library strings.

## Output

Summarize as: **identity** (package/version/signer), **attack surface** (risky
flags + exported components), **native footprint**, **notable indicators**, and a
one-line **risk read** with what to investigate next. If something looks
sensitive, hand off to the `sensitive-api-audit` skill; if the code needs reading,
to the `dex2jar` skill's DEX→JAR workflow.

This is authorized security triage of an app the user already holds.

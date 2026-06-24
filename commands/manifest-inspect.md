---
description: Parse an APK's binary AndroidManifest.xml — package, SDK, permissions, exported components
allowed-tools: Bash(python3:*), Bash(chmod:*)
argument-hint: <file.apk|AndroidManifest.xml> [-e]
---

# manifest-inspect - APK Manifest Inspector

Decode an APK's compiled (binary) `AndroidManifest.xml` and surface the facts that
matter for triage.

## Usage

```bash
python3 d2j-ai.py manifest-inspect $ARGUMENTS
```

## What it does

A compiled `AndroidManifest.xml` is in Android's binary XML (AXML) format — not
readable with a text editor. This command parses it directly (no aapt / Android SDK
required) and reports:

- **package**, **versionCode**, **versionName**
- **min / target / compile SDK** levels
- **debuggable** and **allowBackup** flags (both common misconfigurations)
- **usesCleartextTraffic**
- requested **permissions**
- every **component** (activity / service / receiver / provider) with its
  **exported** status, the reason it is exported (explicit flag vs. implicit
  intent-filter), any guarding **permission**, and declared intent **actions**

The exported components are the app's externally reachable attack surface — the first
thing to enumerate when assessing an APK.

Accepts either a full `.apk` (the manifest is extracted from the zip) or a standalone
binary `AndroidManifest.xml`.

## Options

- `-e, --exported-only` — only list exported components
- `-o, --output <path>` — write output to file instead of stdout

## Output

JSON with `package`, version/SDK fields, `permissions[]`, `permission_count`,
`components[]`, `component_count`, and `exported_count`. Each component is
`{type, name, exported, exported_source, has_intent_filter, permission?, actions[]}`.

## Example Workflows

### Full manifest overview
```bash
python3 d2j-ai.py manifest-inspect app.apk
```

### Just the attack surface
```bash
python3 d2j-ai.py manifest-inspect app.apk -e
```

## Prerequisites

- Java 8+
- Build: `./gradlew :dex2jar-ai-cli:build`

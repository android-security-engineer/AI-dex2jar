---
description: Sign Android APK files with a test certificate
allowed-tools: Bash(python3:*)
argument-hint: <apk-file> [-o output.apk]
---

# apk-sign - Sign APK with Test Certificate

Sign an Android APK file with a test/debug certificate. Required after modifying an APK.

## Usage

```bash
python3 d2j-ai.py apk-sign $ARGUMENTS
```

## Options

- `-o, --output <path>` — Output signed APK path
- `-f, --force` — Force overwrite
- `-t, --tiny` — Use tiny sign

## Output

Returns JSON with:
- `success` — true/false
- `stdout` — signing output
- `stderr` — any errors

---
description: Clean up APK file to standard ZIP format
allowed-tools: Bash(python3:*)
argument-hint: <apk-file> [-o output.zip]
---

# std-apk - Normalize APK to Standard ZIP

Clean up an APK file to standard ZIP format, removing Android-specific signing and alignment.

## Usage

```bash
python3 d2j-ai.py std-apk $ARGUMENTS
```

## What it does

Processes an APK file and converts it to a standard ZIP format. This removes APK-specific structures (like APK Signing Block, ZIP alignment padding) that can interfere with standard ZIP tools.

## Options

- `-o, --output <path>` — Output ZIP file path
- `-f, --force` — Force overwrite existing output

## Output

Returns JSON with:
- `success` — true/false
- `stdout` — processing output log
- `stderr` — any errors or warnings

## Example

```bash
python3 d2j-ai.py std-apk app.apk -o app_clean.zip
```

---
description: Convert Android DEX/APK files to JAR format for analysis
allowed-tools: Bash(python3:*), Bash(chmod:*)
argument-hint: <file.apk|file.dex> [-o output.jar] [-f]
---

# dex2jar - Convert DEX/APK to JAR

Convert Android .dex or .apk files to .jar format for Java analysis.

## Usage

```bash
python3 d2j-ai.py dex2jar $ARGUMENTS
```

## What it does

Reads Dalvik Executable (.dex) files from APK or raw DEX and converts the Dalvik bytecode to Java bytecode, producing a JAR file that can be analyzed with standard Java tools (jd-gui, procyon, cfr, etc.).

## Options

- `-o, --output <path>` — Output JAR file path (default: input-dex2jar.jar)
- `-f, --force` — Force overwrite existing output file
- `-e, --exception-file` — Write exception details to file
- `-n, --not-handle-exception` — Do not handle exceptions during conversion
- `-d, --debug-info` — Translate debug info (line numbers, variable names)

## Output

Returns JSON with:
- `success` — true/false
- `stdout` — conversion output log
- `stderr` — any errors or warnings

## Prerequisites

If the command fails with "Cannot find dex2jar", build first:
```bash
./gradlew distZip
```

## Example Workflow

1. Convert APK: `python3 d2j-ai.py dex2jar app.apk`
2. Verify output: `python3 d2j-ai.py asm-verify app-dex2jar.jar`
3. Disassemble: `python3 d2j-ai.py baksmali classes.dex`

---
description: Inspect DEX/APK structure — list classes, methods, and fields as structured JSON
allowed-tools: Bash(python3:*), Bash(chmod:*)
argument-hint: <file.dex|file.apk> [-f filter] [-d] [-s]
---

# dex-inspect - DEX/APK Structure Inspector

Quickly inspect the structure of a DEX or APK file without full decompilation.

## Usage

```bash
python3 d2j-ai.py dex-inspect $ARGUMENTS
```

## What it does

Reads the DEX binary structure and outputs a structured JSON summary of all classes, their methods, and fields. This is useful for quick reconnaissance — understanding what's inside a DEX file before deciding which tools to use for deeper analysis.

Unlike `baksmali` or `dex2jar`, this command does NOT decompile or disassemble code. It only reads metadata (class names, access flags, method signatures, field types), making it very fast.

## Options

- `-f, --filter <pattern>` — Filter class names by substring pattern
- `-d, --detail` — Show method and field details for each class (name, descriptor, access flags)
- `-s, --strings` — Include string constant values from fields
- `-o, --output <path>` — Write output to file instead of stdout

## Output

Returns JSON with:
- `dex_version` — DEX format version (e.g. "039")
- `min_api_level` — Minimum Android API level
- `class_count` — Number of classes found
- `method_count` — Total method count
- `field_count` — Total field count
- `classes` — Array of class objects with name, access flags, super class, interfaces
  - With `--detail`: includes `methods` and `fields` arrays with signatures

## Example Workflows

### Quick overview
```bash
python3 d2j-ai.py dex-inspect app.apk
```

### Find specific classes
```bash
python3 d2j-ai.py dex-inspect app.apk -f "Activity"
```

### Detailed analysis
```bash
python3 d2j-ai.py dex-inspect --detail --strings app.apk
```

### Combined with other tools
```bash
# 1. Inspect to find interesting classes
python3 d2j-ai.py dex-inspect app.apk -f "login"
# 2. Convert to JAR for decompilation
python3 d2j-ai.py dex2jar app.apk
# 3. Decompile
python3 d2j-ai.py jar2jasmin app-dex2jar.jar
```

## Prerequisites

- Java 8+ (for the DexFileReader API)
- Build: `./gradlew distZip`

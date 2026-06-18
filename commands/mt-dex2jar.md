---
description: Multi-threaded conversion of DEX/APK files to JAR format
allowed-tools: Bash(python3:*)
argument-hint: <file0> [file1 ...] [-o output.jar] [-mt 4]
---

# mt-dex2jar - Multi-threaded DEX to JAR

Convert .dex/.apk files to .jar format using multiple threads for faster processing.

## Usage

```bash
python3 d2j-ai.py mt-dex2jar $ARGUMENTS
```

## What it does

Same as dex2jar but uses concurrent processing for multi-dex APKs or batches of DEX files. Significantly faster for large APKs with many DEX files.

## Options

- `-o, --output <path>` — Output JAR file path
- `-f, --force` — Force overwrite existing output
- `-mt, --multi-thread <N>` — Number of threads (default: 4)
- `-fl, --file-list <file>` — File containing a list of DEX files to process

## Output

Returns JSON with:
- `success` — true/false
- `stdout` — conversion output log
- `stderr` — any errors or warnings

## Example

```bash
python3 d2j-ai.py mt-dex2jar app.apk -mt 8
```

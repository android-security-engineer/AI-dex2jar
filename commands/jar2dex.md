---
description: Convert JAR files to Android DEX format
allowed-tools: Bash(python3:*)
argument-hint: <dir|jar> [-o output.dex]
---

# jar2dex - Convert JAR to DEX

Convert Java .jar files to Android .dex format by invoking dx.

## Usage

```bash
python3 d2j-ai.py jar2dex $ARGUMENTS
```

## Options

- `-o, --output <path>` — Output .dex file path
- `-f, --force` — Force overwrite

## Output

Returns JSON with:
- `success` — true/false
- `stdout` — conversion output
- `stderr` — any errors

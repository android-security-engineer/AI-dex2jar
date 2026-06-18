---
description: Generate no-code stub JAR from ODEX files
allowed-tools: Bash(python3:*)
argument-hint: <odex0> [odex1 ...] [-o stub.jar]
---

# generate-stub-from-odex - Generate Stub JAR from ODEX

Generate a no-code stub JAR file from ODEX files, containing class/method signatures without implementation.

## Usage

```bash
python3 d2j-ai.py generate-stub-from-odex $ARGUMENTS
```

## What it does

Reads ODEX (optimized DEX) files and generates a JAR containing stub .class files — classes with correct names, method signatures, and field signatures but no method bodies. Useful for understanding the API surface of system ODEX files.

## Options

- `-o, --output <path>` — Output JAR file path (default: stub.jar)
- `-npri, --no-private` — Exclude private members from stubs

## Output

Returns JSON with:
- `success` — true/false
- `stdout` — stub generation output log
- `stderr` — any errors or warnings

## Example

```bash
python3 d2j-ai.py generate-stub-from-odex system.odex -o system_stub.jar
```

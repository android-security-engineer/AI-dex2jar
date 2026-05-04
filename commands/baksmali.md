---
description: Disassemble Android DEX files to smali bytecode for analysis
allowed-tools: Bash(python3:*)
argument-hint: <dex-file> [-o output-dir]
---

# baksmali - Disassemble DEX to Smali

Disassemble .dex files into smali bytecode files for reading and analysis.

## Usage

```bash
python3 d2j-ai.py baksmali $ARGUMENTS
```

## What it does

Takes a Dalvik Executable (.dex) file and disassembles it into human-readable smali files. Each class becomes a .smali file organized by package name.

## Output

Returns JSON with:
- `success` — true/false
- `stdout` — disassembly output log
- `stderr` — any errors

## Example

```bash
python3 d2j-ai.py baksmali classes.dex -o smali_output/
```

The smali files can then be read and analyzed to understand the app's logic.

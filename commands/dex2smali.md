---
description: Disassemble DEX files to smali format (alias for baksmali)
allowed-tools: Bash(python3:*)
argument-hint: <dex-file> [-o output-dir]
---

# dex2smali - Disassemble DEX to Smali

Disassemble .dex files into smali bytecode files. This is an alias for baksmali.

## Usage

```bash
python3 d2j-ai.py dex2smali $ARGUMENTS
```

## What it does

Takes a Dalvik Executable (.dex) file and disassembles it into human-readable smali files. Each class becomes a .smali file organized by package name. Identical to `baksmali` but with a more descriptive name.

## Output

Returns JSON with:
- `success` — true/false
- `stdout` — disassembly output log
- `stderr` — any errors or warnings

## Example

```bash
python3 d2j-ai.py dex2smali classes.dex -o smali_output/
```

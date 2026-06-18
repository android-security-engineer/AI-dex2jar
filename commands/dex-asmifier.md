---
description: Generate ASMifier Java source from DEX files
allowed-tools: Bash(python3:*)
argument-hint: <dex0> [dex1 ...]
---

# dex-asmifier - DEX to ASMifier

Generate ASMifier Java source code from DEX files.

## Usage

```bash
python3 d2j-ai.py dex-asmifier $ARGUMENTS
```

## What it does

Reads DEX files and generates ASMifier output — Java source code that uses the ASM library to recreate the same bytecode programmatically. Useful for understanding bytecode structure at the ASM API level.

## Output

Returns JSON with:
- `success` — true/false
- `stdout` — ASMifier Java source output
- `stderr` — any errors or warnings

## Example

```bash
python3 d2j-ai.py dex-asmifier classes.dex
```

---
description: Assemble smali bytecode files into Android DEX format
allowed-tools: Bash(python3:*)
argument-hint: [<smali-file>|folder]* [-o output.dex]
---

# smali - Assemble Smali to DEX

Assemble smali bytecode files into a Dalvik Executable (.dex) file.

## Usage

```bash
python3 d2j-ai.py smali $ARGUMENTS
```

## What it does

Takes one or more smali files or directories containing smali files and assembles them into a single .dex file. This is the reverse of the baksmali command.

## Options

- `-o, --output <path>` — Output .dex file path
- `-f, --force` — Force overwrite existing output

## Output

Returns JSON with:
- `success` — true/false
- `stdout` — assembly output log
- `stderr` — any errors or warnings

## Example

```bash
python3 d2j-ai.py smali smali_dir/ -o classes.dex
```

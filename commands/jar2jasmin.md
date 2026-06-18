---
description: Disassemble .class files in JAR to jasmin assembly format
allowed-tools: Bash(python3:*)
argument-hint: <jar-file> [-o output-dir]
---

# jar2jasmin - Disassemble JAR to Jasmin

Disassemble .class files in a JAR into jasmin assembly format.

## Usage

```bash
python3 d2j-ai.py jar2jasmin $ARGUMENTS
```

## What it does

Takes a JAR file and disassembles each .class file into jasmin format (.j files). Jasmin is a human-readable assembly language for Java bytecode, more detailed than smali for JVM-level analysis.

## Options

- `-o, --output <dir>` — Output directory for jasmin files
- `-f, --force` — Force overwrite existing files

## Output

Returns JSON with:
- `success` — true/false
- `stdout` — disassembly output log
- `stderr` — any errors or warnings

## Example

```bash
python3 d2j-ai.py jar2jasmin app.jar -o jasmin_output/
```

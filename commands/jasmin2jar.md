---
description: Assemble jasmin files back into a JAR file
allowed-tools: Bash(python3:*)
argument-hint: <dir> [-o output.jar]
---

# jasmin2jar - Assemble Jasmin to JAR

Assemble jasmin format files back into a JAR file.

## Usage

```bash
python3 d2j-ai.py jasmin2jar $ARGUMENTS
```

## What it does

Takes a directory of jasmin (.j) assembly files and assembles them into a JAR file. This is the reverse of jar2jasmin, allowing round-trip editing of Java bytecode.

## Options

- `-o, --output <path>` — Output JAR file path
- `-f, --force` — Force overwrite existing output

## Output

Returns JSON with:
- `success` — true/false
- `stdout` — assembly output log
- `stderr` — any errors or warnings

## Example

```bash
python3 d2j-ai.py jasmin2jar jasmin_output/ -o modified.jar
```

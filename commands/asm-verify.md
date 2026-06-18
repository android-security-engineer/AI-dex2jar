---
description: Verify .class files in JAR files for correctness
allowed-tools: Bash(python3:*)
argument-hint: <jar0> [jar1 ... jarN]
---

# asm-verify - Verify Class Files in JAR

Verify .class files inside JAR files using ASM framework.

## Usage

```bash
python3 d2j-ai.py asm-verify $ARGUMENTS
```

## What it does

Validates .class files in one or more JAR files, checking bytecode correctness and structural integrity. Useful for verifying dex2jar output quality.

## Options

- `-d, --detail` — Show detailed verification information

## Output

Returns JSON with:
- `success` — true/false
- `stdout` — verification results
- `stderr` — any errors or warnings

## Example

```bash
python3 d2j-ai.py asm-verify app-dex2jar.jar
```

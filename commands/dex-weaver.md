---
description: Replace method invocations in DEX files
allowed-tools: Bash(python3:*)
argument-hint: <dex-file> [-c config]
---

# dex-weaver - Weave DEX Invocations

Replace method invocations in a DEX file according to a weave configuration.

## Usage

```bash
python3 d2j-ai.py dex-weaver $ARGUMENTS
```

## What it does

Modifies a .dex file by replacing specified method invocations with others, based on a configuration. Useful for hooking or redirecting method calls in Dalvik bytecode.

## Options

- `-c, --config <file>` — Weave configuration file
- `-o, --output <path>` — Output DEX file path

## Output

Returns JSON with:
- `success` — true/false
- `stdout` — weaving output log
- `stderr` — any errors or warnings

## Example

```bash
python3 d2j-ai.py dex-weaver classes.dex -c weave.cfg -o woven.dex
```

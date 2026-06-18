---
description: Extract ODEX files from Dalvik memory core dumps
allowed-tools: Bash(python3:*)
argument-hint: <core.xxxx>
---

# extract-odex-from-coredump - Extract ODEX from Core Dump

Extract ODEX files from a Dalvik VM memory core dump.

## Usage

```bash
python3 d2j-ai.py extract-odex-from-coredump $ARGUMENTS
```

## What it does

Scans a core dump file (produced when Dalvik crashes) and extracts embedded ODEX files. Useful for forensic analysis of Android runtime crashes to recover the DEX/ODEX code that was loaded in memory.

## Output

Returns JSON with:
- `success` — true/false
- `stdout` — extraction output log
- `stderr` — any errors or warnings

## Example

```bash
python3 d2j-ai.py extract-odex-from-coredump core.12345
```

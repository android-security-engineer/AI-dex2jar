---
description: Execute multiple dex2jar commands from a JSON batch file
allowed-tools: Bash(python3:*), Bash(chmod:*)
argument-hint: <batch-file.json>
---

# batch - Execute Multiple Commands

Execute multiple dex2jar commands from a JSON file in sequence.

## Usage

```bash
python3 d2j-ai.py batch $ARGUMENTS
```

## What it does

Reads a JSON file containing an array of command objects and executes them in sequence.
Returns a summary with per-command results and overall success/failure counts.

## Batch File Format

```json
[
  {"command": "dex2jar", "args": ["app.apk"]},
  {"command": "asm-verify", "args": ["app-dex2jar.jar"]},
  {"command": "jar2jasmin", "args": ["app-dex2jar.jar"]}
]
```

## Output

Returns JSON with:
- `success` — true if all commands succeeded
- `results` — array of per-command results
- `total` — number of commands executed
- `success_count` — number of successful commands
- `failure_count` — number of failed commands

## Example Workflows

### Full APK analysis pipeline
```json
[
  {"command": "dex2jar", "args": ["app.apk", "-o", "app.jar"]},
  {"command": "asm-verify", "args": ["app.jar"]},
  {"command": "jar2jasmin", "args": ["app.jar"]},
  {"command": "decrypt-string", "args": ["app.jar"]}
]
```

### DEX patching and re-signing
```json
[
  {"command": "baksmali", "args": ["classes.dex"]},
  {"command": "smali", "args": ["out"]},
  {"command": "dex-recompute-checksum", "args": ["classes.dex"]},
  {"command": "apk-sign", "args": ["app.apk"]}
]
```

## Prerequisites

- Java 8+
- Build dex2jar: `./gradlew distZip`

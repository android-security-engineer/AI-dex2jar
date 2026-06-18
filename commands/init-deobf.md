---
description: Generate deobfuscation initialization config for JAR files
allowed-tools: Bash(python3:*)
argument-hint: <jar-file> [-o output.txt] [-min 2] [-max 40]
---

# init-deobf - Generate Deobfuscation Config

Generate an initialization configuration file for de-obfuscating a JAR.

## Usage

```bash
python3 d2j-ai.py init-deobf $ARGUMENTS
```

## What it does

Analyzes a JAR file and generates a renaming configuration for de-obfuscation. Identifies short or meaningless names (like `a`, `b`, `aa`) that are likely ProGuard/minifier output and creates a mapping to more readable names.

## Options

- `-o, --output <path>` — Output config file path (default: <jar-name>-deobf-init.txt)
- `-f, --force` — Force overwrite existing output
- `-min, --min-length <N>` — Rename if name length < N (default: 2)
- `-max, --max-length <N>` — Rename if name length > N (default: 40)

## Output

Returns JSON with:
- `success` — true/false
- `stdout` — config generation output log
- `stderr` — any errors or warnings

## Example

```bash
python3 d2j-ai.py init-deobf obfuscated.jar -o rename_config.txt
```

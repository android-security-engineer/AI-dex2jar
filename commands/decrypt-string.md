---
description: Decrypt encrypted strings in .class files within JAR
allowed-tools: Bash(python3:*)
argument-hint: <jar-file> [-o output.jar]
---

# decrypt-string - Decrypt Strings in Class Files

Decrypt obfuscated/encrypted strings in .class files within a JAR.

## Usage

```bash
python3 d2j-ai.py decrypt-string $ARGUMENTS
```

## What it does

Analyzes .class files in a JAR for string encryption patterns and attempts to decrypt them. Commonly used against apps that use string encryption as an obfuscation technique.

## Options

- `-o, --output <path>` — Output JAR file with decrypted strings
- `-f, --force` — Force overwrite existing output
- `-d, --decrypter <class>` — Specify decrypter class to use

## Output

Returns JSON with:
- `success` — true/false
- `stdout` — decryption output log
- `stderr` — any errors or warnings

## Example

```bash
python3 d2j-ai.py decrypt-string obfuscated.jar -o decrypted.jar
```

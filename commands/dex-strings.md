---
description: Extract string constants from a DEX/APK — find URLs, keys, and log messages
allowed-tools: Bash(python3:*), Bash(chmod:*)
argument-hint: <file.dex|file.apk> [-f filter] [-c class] [-u]
---

# dex-strings - DEX/APK String Extractor

Extract all string constants from a DEX or APK file without full decompilation.

## Usage

```bash
python3 d2j-ai.py dex-strings $ARGUMENTS
```

## What it does

Pulls `const-string` literals from method bodies and static field constants. This is one of the fastest ways to find indicators of compromise, hardcoded URLs, API keys, encryption keys, log messages, and suspicious strings — without converting the whole app to JAR.

Each result records which class and method the string appeared in, so you can jump straight to the relevant code.

## Options

- `-f, --filter <pattern>` — Only return strings containing this substring
- `-c, --class <pattern>` — Only scan classes whose name contains this substring
- `-u, --unique` — Deduplicate identical string values
- `-o, --output <path>` — Write output to file instead of stdout

## Output

Returns JSON with:
- `string_count` — Total number of string occurrences
- `unique_count` — Number of distinct strings (with `--unique`)
- `strings` — Array of entries, each showing the class, method/field, and value

## Example Workflows

### Find all strings
```bash
python3 d2j-ai.py dex-strings app.apk
```

### Hunt for URLs
```bash
python3 d2j-ai.py dex-strings app.apk -f "http"
```

### Find unique strings in a specific package
```bash
python3 d2j-ai.py dex-strings app.apk -c "com/example/crypto" -u
```

## Prerequisites

- Java 8+ (uses the DexFileReader API)
- Build: `./gradlew :dex2jar-ai-cli:build`

---
description: Build a method call graph from a DEX/APK — trace callers and callees
allowed-tools: Bash(python3:*), Bash(chmod:*)
argument-hint: <file.dex|file.apk> [-c class] [-t target]
---

# dex-method-trace - DEX/APK Call Graph Builder

Build a method call graph from a DEX or APK file.

## Usage

```bash
python3 d2j-ai.py dex-method-trace $ARGUMENTS
```

## What it does

Walks every method body and records each method invocation as a caller→callee edge. This is useful for understanding control flow, finding all callers of a sensitive API (e.g. `Cipher.getInstance`, `Runtime.exec`, `loadLibrary`), and reconstructing how obfuscated code is wired together.

## Options

- `-c, --class <pattern>` — Only trace methods in classes matching this substring
- `-t, --target <pattern>` — Find all callers of methods matching this substring (matched against `owner.name`)
- `-o, --output <path>` — Write output to file instead of stdout

## Output

Returns JSON with:
- `edge_count` — Number of call edges found
- `target` — The target filter, if supplied
- `edges` — Array of `{caller, callee}` objects with full method signatures

## Example Workflows

### Full call graph for a package
```bash
python3 d2j-ai.py dex-method-trace app.apk -c "com/example/payment"
```

### Who calls a sensitive API?
```bash
# Find every caller of methods named "exec" or in Runtime
python3 d2j-ai.py dex-method-trace app.apk -t "Runtime"
python3 d2j-ai.py dex-method-trace app.apk -t "Cipher.getInstance"
```

## Prerequisites

- Java 8+ (uses the DexFileReader API)
- Build: `./gradlew :dex2jar-ai-cli:build`

---
description: Analyze class dependencies in a DEX/APK — superclasses, interfaces, references
allowed-tools: Bash(python3:*), Bash(chmod:*)
argument-hint: <file.dex|file.apk> [-c class] [-d] [-i]
---

# dex-class-deps - DEX/APK Class Dependency Analyzer

Analyze the dependency graph between classes in a DEX or APK file.

## Usage

```bash
python3 d2j-ai.py dex-class-deps $ARGUMENTS
```

## What it does

For each class, reports the other types it depends on:
- Superclass and implemented interfaces
- Field types
- Method parameter and return types
- With `--deep`: types referenced inside method bodies (invoked methods' owners, accessed fields' owners)

This helps you understand architecture, find tightly-coupled clusters, and isolate the minimal set of classes you need to extract or analyze.

## Options

- `-c, --class <pattern>` — Only analyze classes matching this substring
- `-d, --deep` — Include dependencies from method bodies (invokes, field access)
- `-i, --internal-only` — Only show dependencies on classes defined in this DEX (hide framework/library types)
- `-o, --output <path>` — Write output to file instead of stdout

## Output

Returns JSON with:
- `class_count` — Number of classes analyzed
- `deep` — Whether method-body dependencies were included
- `dependencies` — Map of class name → sorted array of dependency type descriptors

## Example Workflows

### Internal architecture map
```bash
python3 d2j-ai.py dex-class-deps app.apk -i
```

### Deep dependency analysis of one package
```bash
python3 d2j-ai.py dex-class-deps app.apk -c "com/example/core" -d -i
```

## Prerequisites

- Java 8+ (uses the DexFileReader API)
- Build: `./gradlew :dex2jar-ai-cli:build`

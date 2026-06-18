---
description: Replace method invocations in JAR files
allowed-tools: Bash(python3:*)
argument-hint: <jar-file> [-c config]
---

# jar-weaver - Weave JAR Invocations

Replace method invocations in a JAR file according to a weave configuration.

## Usage

```bash
python3 d2j-ai.py jar-weaver $ARGUMENTS
```

## What it does

Modifies a .jar file by replacing specified method invocations with others, based on a configuration. This is the JAR equivalent of dex-weaver, operating on Java bytecode.

## Options

- `-c, --config <file>` — Weave configuration file
- `-o, --output <path>` — Output JAR file path

## Output

Returns JSON with:
- `success` — true/false
- `stdout` — weaving output log
- `stderr` — any errors or warnings

## Example

```bash
python3 d2j-ai.py jar-weaver app.jar -c weave.cfg -o woven.jar
```

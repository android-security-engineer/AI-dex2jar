---
description: Reverse cross-reference a DEX/APK — find where a symbol or sensitive API is used
allowed-tools: Bash(python3:*), Bash(chmod:*)
argument-hint: <file.dex|file.apk> (--to <substr> | --preset crypto|reflection|dynload|net)
---

# dex-xref - DEX/APK Reverse Cross-Reference

Find every site that references a target symbol, and the method it sits in.

## Usage

```bash
python3 d2j-ai.py dex-xref $ARGUMENTS
```

## What it does

Where `dex-method-trace` answers *"what does method X call?"*, `dex-xref` answers the
inverse — *"where is symbol S used, and from which enclosing method?"*. It scans every
instruction and reports each **invoke target**, **field access**, **type usage**
(new-instance / check-cast / instance-of), and **string constant** whose textual form
matches the query.

This is the first move in malware triage: instead of reading the whole app, jump straight
to the call sites of the APIs that matter.

## Query modes

Provide exactly one of:

- `-t, --to <substr>` — match any reference whose symbol contains this substring
  (e.g. `Ljava/lang/Runtime;->exec`, `loadLibrary`, `https://`).
- `-p, --preset <name>` — a curated bundle of sensitive-API indicators, OR-matched:
  - `crypto` — `javax.crypto.*`, `java.security.*`, `Cipher`, `MessageDigest`, `SecretKeySpec`, `Mac`, `Signature`…
  - `reflection` — `java.lang.reflect.*`, `Class.forName`, `getDeclaredMethod`, `Method.invoke`, `setAccessible`…
  - `dynload` — `DexClassLoader`, `PathClassLoader`, `InMemoryDexClassLoader`, `System.load`, `loadLibrary`…
  - `net` — `java.net.*`, `okhttp3`, `HttpURLConnection`, `openConnection`, `WebView`, `http(s)://`…

## Options

- `-k, --kinds <list>` — restrict to a comma-separated subset of `invoke,field,type,string` (default: all)
- `-o, --output <path>` — write output to file instead of stdout

## Output

Returns JSON with:
- `query` — the resolved query label (`preset:crypto` or the raw `--to` string)
- `match_count` — number of reference sites found
- `matches` — array of `{kind, symbol, in_method, op}` objects

## Example Workflows

### Find all crypto usage
```bash
python3 d2j-ai.py dex-xref app.apk --preset crypto
```

### Find dynamic code loading (common in packers/droppers)
```bash
python3 d2j-ai.py dex-xref app.apk --preset dynload
```

### Hunt a specific sink, invokes only
```bash
python3 d2j-ai.py dex-xref app.apk --to "Runtime;->exec" --kinds invoke
```

### Find hardcoded URLs
```bash
python3 d2j-ai.py dex-xref app.apk --to "https://" --kinds string
```

## Prerequisites

- Java 8+ (uses the DexFileReader API)
- Build: `./gradlew :dex2jar-ai-cli:build`

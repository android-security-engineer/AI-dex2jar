---
description: Enumerate an APK's native .so libraries and their exported JNI entry points
allowed-tools: Bash(python3:*), Bash(chmod:*)
argument-hint: <file.apk|lib.so> [-s]
---

# native-libs - APK Native Library / JNI Inspector

List the native libraries shipped in an APK and the JNI functions they export.

## Usage

```bash
python3 d2j-ai.py native-libs $ARGUMENTS
```

## What it does

Walks `lib/<abi>/*.so` in the APK and parses each ELF directly (self-contained — no NDK
or binutils). For every library it reports:

- **abi** (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`, …) and file size
- **ELF identity** — class (ELF32/ELF64), byte order, machine (arm / aarch64 / x86 / …)
- **dynsym_count** and the exported **JNI symbols**: `JNI_OnLoad` plus every
  `Java_<pkg>_<Class>_<method>` bridge function

The `Java_*` names decode straight back to the bound Java method, so they point at
exactly where execution crosses from Java into native code — the place to start when the
interesting logic has been pushed into C/C++.

Also accepts a standalone `.so` file.

## Options

- `-s, --symbols` — include the full list of JNI symbol names per library
- `-o, --output <path>` — write output to file instead of stdout

## Output

JSON with `native_lib_count`, `abis[]`, `total_jni_symbols`, and `libraries[]`, where
each entry is `{path, abi, size_bytes, is_elf, elf_class, byte_order, machine,
dynsym_count, has_jni_onload, jni_symbol_count, jni_symbols[]?}`.

## Example Workflows

### Map the native attack surface
```bash
python3 d2j-ai.py native-libs app.apk -s
```

### Find which Java methods call into native code
```bash
python3 d2j-ai.py native-libs app.apk -s | jq -r '.libraries[].jni_symbols[]' | sort -u
```

## Prerequisites

- Java 8+
- Build: `./gradlew :dex2jar-ai-cli:build`

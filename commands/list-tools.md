---
description: List all available dex2jar reverse engineering tools and their descriptions
allowed-tools: Bash(python3:*)
argument-hint:
---

# list-tools - Available Reverse Engineering Tools

List all available dex2jar tools with descriptions.

## Usage

```bash
python3 d2j-ai.py list
```

## Output

Returns JSON with:
- `available_commands` — All 21 supported commands
- `installed_commands` — Commands actually installed on this system
- `descriptions` — Description of each command

## Full Command Reference

### Core Conversion

| Command | Description |
|---------|-------------|
| `dex2jar` | Convert .dex/.apk to .jar |
| `mt-dex2jar` | Multi-threaded dex to jar conversion |
| `jar2dex` | Convert .jar to .dex |

### Disassembly & Assembly

| Command | Description |
|---------|-------------|
| `baksmali` | Disassemble .dex to smali |
| `dex2smali` | Disassemble .dex to smali (alias for baksmali) |
| `smali` | Assemble smali to .dex |
| `jar2jasmin` | Disassemble .class to jasmin |
| `jasmin2jar` | Assemble jasmin to jar |
| `dex-asmifier` | Generate ASMifier source from dex |

### Modification & Patching

| Command | Description |
|---------|-------------|
| `apk-sign` | Sign APK with test certificate |
| `jar-access` | Modify access flags in jar |
| `dex-weaver` | Replace invoke in dex |
| `jar-weaver` | Replace invoke in jar |
| `class-version-switch` | Switch class file version |
| `decrypt-string` | Decrypt strings in class |

### Deobfuscation

| Command | Description |
|---------|-------------|
| `init-deobf` | Generate deobfuscation config for jar |

### Verification & Repair

| Command | Description |
|---------|-------------|
| `asm-verify` | Verify .class in jar |
| `dex-recompute-checksum` | Recompute dex checksum |
| `std-apk` | Clean APK to standard zip |

### Forensics & ODEX

| Command | Description |
|---------|-------------|
| `generate-stub-from-odex` | Generate stub jar from odex |
| `extract-odex-from-coredump` | Extract odex from core dump |

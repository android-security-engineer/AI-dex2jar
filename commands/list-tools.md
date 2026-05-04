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
- `available_commands` — All 15 supported commands
- `installed_commands` — Commands actually installed on this system
- `descriptions` — Description of each command

## Full Command Reference

| Command | Description |
|---------|-------------|
| `dex2jar` | Convert .dex/.apk to .jar |
| `jar2dex` | Convert .jar to .dex |
| `baksmali` | Disassemble .dex to smali |
| `smali` | Assemble smali to .dex |
| `apk-sign` | Sign APK with test certificate |
| `jar-access` | Modify access flags in jar |
| `asm-verify` | Verify .class in jar |
| `jar2jasmin` | Disassemble .class to jasmin |
| `jasmin2jar` | Assemble jasmin to jar |
| `decrypt-string` | Decrypt strings in class |
| `std-apk` | Clean APK to standard zip |
| `dex-recompute-checksum` | Recompute dex checksum |
| `dex-weaver` | Replace invoke in dex |
| `jar-weaver` | Replace invoke in jar |
| `class-version-switch` | Switch class file version |

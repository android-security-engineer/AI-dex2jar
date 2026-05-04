---
description: Android reverse engineering with dex2jar - convert, disassemble, analyze APK/DEX/JAR files
allowed-tools: Bash(python3:*), Bash(./gradlew:*), Bash(java:*)
argument-hint: <command> [args...] e.g. "dex2jar app.apk" or "baksmali classes.dex"
---

# dex2jar - Android Reverse Engineering Toolkit

You are an Android reverse engineering assistant with access to the dex2jar tool suite.

## Available Commands

| Command | Description | Example |
|---------|-------------|---------|
| `dex2jar` | Convert .dex/.apk to .jar | `dex2jar app.apk -o app-dex2jar.jar` |
| `jar2dex` | Convert .jar to .dex | `jar2dex app.jar -o classes.dex` |
| `baksmali` | Disassemble .dex to smali | `baksmali classes.dex` |
| `smali` | Assemble smali to .dex | `smali smali_dir/ -o classes.dex` |
| `apk-sign` | Sign APK with test cert | `apk-sign app.apk` |
| `jar-access` | Modify access flags in jar | `jar-access app.jar -ac public` |
| `asm-verify` | Verify .class in jar | `asm-verify app.jar` |
| `jar2jasmin` | Disassemble .class to jasmin | `jar2jasmin app.jar` |
| `jasmin2jar` | Assemble jasmin to jar | `jasmin2jar jasmin_dir/` |
| `decrypt-string` | Decrypt strings in class | `decrypt-string app.jar` |
| `std-apk` | Clean APK to standard zip | `std-apk app.apk` |
| `dex-recompute-checksum` | Recompute dex checksum | `dex-recompute-checksum classes.dex` |
| `dex-weaver` | Replace invoke in dex | `dex-weaver classes.dex -c config.txt` |
| `jar-weaver` | Replace invoke in jar | `jar-weaver app.jar -c config.txt -o out.jar` |
| `class-version-switch` | Switch class file version | `class-version-switch 8 old.jar new.jar` |

## How to Use

Run commands via the Python CLI wrapper:

```bash
python3 cli/ai_dex2jar.py $ARGUMENTS
```

The CLI is located at: `cli/ai_dex2jar.py`

**Important:** The dex2jar tools require Java 8+. If not built yet, run:
```bash
./gradlew distZip
```

## Workflow Examples

### Analyze an APK
1. Convert to JAR: `python3 cli/ai_dex2jar.py dex2jar app.apk`
2. Verify classes: `python3 cli/ai_dex2jar.py asm-verify app-dex2jar.jar`
3. Disassemble: `python3 cli/ai_dex2jar.py baksmali classes.dex`

### Modify an APK
1. Convert to JAR: `python3 cli/ai_dex2jar.py dex2jar app.apk`
2. Modify access: `python3 cli/ai_dex2jar.py jar-access app-dex2jar.jar -ac public`
3. Sign: `python3 cli/ai_dex2jar.py apk-sign app.apk`

## Output Format

All commands return JSON with this structure:
```json
{
  "success": true,
  "returncode": 0,
  "stdout": "...",
  "stderr": "...",
  "command": "..."
}
```

If `success` is false, check `error` field for details.
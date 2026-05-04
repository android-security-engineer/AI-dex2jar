# dex2jar - Claude Code Skill

Android reverse engineering toolkit for AI agents.

## Install

```bash
claude plugin add https://github.com/CC11001100/AI-dex2jar.git
```

## Commands

| Slash Command | Description |
|--------------|-------------|
| `/dex2jar` | Convert DEX/APK to JAR |
| `/baksmali` | Disassemble DEX to smali |
| `/jar2dex` | Convert JAR to DEX |
| `/jar-access` | Modify JAR access flags |
| `/apk-sign` | Sign APK with test cert |
| `/list-tools` | List all available tools |

## Prerequisites

- Java 8+
- Python 3.10+
- Build dex2jar: `./gradlew distZip`

## CLI Wrapper

All commands use `d2j-ai.py` which outputs structured JSON:

```bash
python3 d2j-ai.py dex2jar app.apk
python3 d2j-ai.py baksmali classes.dex
python3 d2j-ai.py list
```

# AI-dex2jar as Claude Code Skill

## Quick Install

Add this repository as a skill source in Claude Code:

```bash
# Install via Claude Code CLI
claude skill add https://github.com/CC11001100/AI-dex2jar.git
```

Or manually add to your `.claude/settings.json`:

```json
{
  "skills": {
    "ai-dex2jar": {
      "source": "https://github.com/CC11001100/AI-dex2jar.git"
    }
  }
}
```

## Usage

After installation, use the `/dex2jar` slash command in Claude Code:

```
/dex2jar dex2jar app.apk
/dex2jar baksmali classes.dex
/dex2jar apk-sign app.apk
```

Or invoke the reverse-engineer agent for comprehensive analysis:

```
/dex2jar analyze app.apk
```

## Prerequisites

- Java 8+ (required by dex2jar tools)
- Python 3.10+ (required by CLI wrapper)
- Build dex2jar first: `./gradlew distZip`

## Available Commands

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
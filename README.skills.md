# dex2jar Skills — Claude Code Integration

This repository provides a Claude Code Skill for AI-assisted Android reverse engineering using dex2jar.

## Quick Start

```bash
# Add as a Claude Code plugin
claude plugin add https://github.com/android-security-engineer/dex2jar-skills.git

# Or run the AI CLI directly
python3 d2j-ai.py help
```

## Slash Commands

| Command | Description |
|---------|-------------|
| `/dex2jar <file>` | Convert DEX/APK → JAR |
| `/jar2dex <file>` | Convert JAR → DEX |
| `/baksmali <file>` | Disassemble DEX → smali |
| `/smali <dir>` | Assemble smali → DEX |
| `/jar2jasmin <jar>` | Decompile JAR → jasmin |
| `/apk-sign <apk>` | Sign APK with test cert |
| `/jar-access <jar>` | Modify JAR access flags |
| `/asm-verify <jar>` | Verify .class in JAR |
| `/decrypt-string <jar>` | Decrypt encrypted strings |
| `/dex-inspect <file>` | Inspect DEX/APK structure |
| `/dex-strings <file>` | Extract string constants |
| `/dex-method-trace <file>` | Build method call graph |
| `/dex-class-deps <file>` | Analyze class dependencies |
| `/dex-xref <file>` | Reverse xref: find symbol / sensitive-API usage sites |
| `/manifest-inspect <apk>` | Parse AndroidManifest.xml — perms, exported components |
| `/apk-cert <apk>` | Read v1 signing cert — subject, validity, fingerprints |
| `/native-libs <apk>` | List .so libs — ABI, ELF identity, exported JNI symbols |
| `/list-tools` | List all available tools |
| `/info <command>` | Show command details & options |
| `/batch <file>` | Execute batch commands from JSON |

## AI CLI (d2j-ai.py)

All commands return structured JSON for easy AI parsing:

```bash
python3 d2j-ai.py dex2jar app.apk
```

**Output format:**
```json
{
  "success": true,
  "error_code": "none",
  "output_path": "app-dex2jar.jar",
  "duration_ms": 1234,
  "timestamp": "2026-06-22T10:30:00+0800",
  "command": "/path/to/d2j-dex2jar.sh app.apk"
}
```

**Error codes:** `none`, `file_not_found`, `invalid_format`, `command_failed`, `unknown_command`

### Global Options

- `--pretty` — Pretty-print JSON output
- `--verbose` — Show verbose output including stderr

### All 29 Commands

| Command | Description |
|---------|-------------|
| `dex2jar` | Convert .dex/.apk to .jar |
| `mt-dex2jar` | Multi-threaded dex to jar |
| `jar2dex` | Convert .jar to .dex |
| `baksmali` | Disassemble .dex to smali |
| `dex2smali` | Disassemble .dex to smali (alias) |
| `smali` | Assemble smali files to .dex |
| `apk-sign` | Sign APK with test certificate |
| `jar-access` | Modify access flags in .jar |
| `asm-verify` | Verify .class files in jar |
| `jar2jasmin` | Disassemble .class to jasmin |
| `jasmin2jar` | Assemble jasmin files to .jar |
| `decrypt-string` | Decrypt strings in .class files |
| `init-deobf` | Generate deobfuscation config |
| `std-apk` | Clean up APK to standard zip |
| `dex-asmifier` | Generate ASMifier output |
| `dex-recompute-checksum` | Recompute CRC and SHA1 |
| `dex-weaver` | Replace invoke in .dex |
| `jar-weaver` | Replace invoke in .jar |
| `class-version-switch` | Switch .class file version |
| `generate-stub-from-odex` | Generate stub from .odex |
| `extract-odex-from-coredump` | Extract .odex from coredump |
| `dex-inspect` | Inspect DEX/APK structure |
| `dex-strings` | Extract string constants from .dex/.apk |
| `dex-method-trace` | Build method call graph from .dex/.apk |
| `dex-class-deps` | Analyze class dependencies in .dex/.apk |
| `dex-xref` | Reverse cross-reference: find symbol / sensitive-API usage sites |
| `manifest-inspect` | Parse binary AndroidManifest.xml — package, SDK, permissions, exported components |
| `apk-cert` | Read v1 signing certificates — subject/issuer/validity + SHA-256/SHA-1 fingerprints |
| `native-libs` | Enumerate lib/*/*.so — ABI, ELF identity, and exported JNI symbols |

### Special Commands

| Command | Description |
|---------|-------------|
| `list` | List all available commands |
| `info [command]` | Show command details & options |
| `batch <json-file>` | Execute multiple commands from JSON |

## Batch Workflow

Create a JSON file with commands to execute:

```json
[
  {"command": "dex2jar", "args": ["app.apk"]},
  {"command": "asm-verify", "args": ["app-dex2jar.jar"]},
  {"command": "jar2jasmin", "args": ["app-dex2jar.jar"]}
]
```

Run with:
```bash
python3 d2j-ai.py batch workflow.json
```

## MCP Server

Exposes all 29 tools via MCP protocol for Claude Desktop/Code:

```bash
pip install mcp
python3 dex2jar-mcp/server.py
```

See [dex2jar-mcp/README.md](dex2jar-mcp/README.md) for setup details.

## Skills

Workflow-level Skills live under `skills/<name>/SKILL.md` (with optional
`references/` for progressive disclosure). Each has trigger-rich frontmatter so
Claude loads it when the task fits:

| Skill | Triggers on | Drives |
|-------|-------------|--------|
| **dex2jar** | Any Android RE task (APK/DEX/JAR/smali, convert, decompile, analyze) | The full workbench; routes to triage / DEX→JAR / xref / patch workflows |
| **apk-triage** | "Is this APK safe? what's the attack surface? who signed it?" | `manifest-inspect` + `apk-cert` + `native-libs` + `dex-strings` |
| **sensitive-api-audit** | "Find the crypto/network/reflection/dynamic-loading; who calls X?" | `dex-xref` presets + `dex-method-trace` caller tracing |

```
skills/
├── dex2jar/
│   ├── SKILL.md
│   └── references/
│       ├── command-reference.md     # all 29 commands, grouped
│       ├── workflow-triage.md       # APK first-pass recon
│       ├── dex-to-jar-accuracy.md   # how DEX→JAR works + when it fails
│       └── backends.md              # slash vs CLI vs MCP
├── apk-triage/SKILL.md
└── sensitive-api-audit/SKILL.md
```

## Agents

- **reverse-engineer** — Full reverse engineering workflow agent

## Repository Layout

```
dex2jar-skills/
├── .claude-plugin/
│   ├── plugin.json          # plugin manifest (mirrors root plugin.json)
│   └── marketplace.json     # marketplace listing
├── plugin.json              # plugin manifest (name, version, commands/agents/skills, keywords)
├── skills/                  # workflow Skills (SKILL.md + references/)
├── commands/                # slash commands (one per CLI command)
├── agents/                  # specialized subagents
├── evals/                   # skill trigger tests + offline validator
├── dex2jar-mcp/             # MCP server (one tool per command)
├── d2j-ai.py                # the 29-command structured-JSON CLI backend
└── dex2jar-ai-cli/          # Java analysis module behind the CLI
```

Validate the whole structure offline with `bash evals/validate-skills.sh`.

## Architecture

```
Skills (skills/*/SKILL.md)  +  Slash commands (commands/*.md)  +  Agents (agents/)
        ↓
MCP Server (dex2jar-mcp/server.py) — 29 tools + resources + prompts
        ↓
AI CLI (d2j-ai.py) — structured JSON, input validation, output parsing
        ↓
dex2jar Core (dex-tools, dex-reader, etc.)
```

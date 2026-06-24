# dex2jar MCP Server

MCP (Model Context Protocol) server that exposes dex2jar reverse engineering tools to AI assistants like Claude.

## Quick Start

```bash
# Install MCP SDK
pip install mcp

# Run the server (stdio transport)
python3 dex2jar-mcp/server.py
```

## Features

| Type | Count | Examples |
|------|-------|---------|
| **Tools** | 29 | `dex2jar`, `baksmali`, `dex_inspect`, `dex_xref`, `manifest_inspect`, `apk_cert`, `native_libs` |
| **Resources** | 3 | Command list, command info, version info |
| **Prompts** | 4 | `analyze_apk`, `patch_and_rebuild`, `resign_apk`, `compare_dex` |

## Tools

All dex2jar commands have dedicated MCP tools:

- `list_commands` — List all available dex2jar commands
- `command_info` — Get detailed info about a command
- `dex2jar` — Convert DEX/APK to JAR
- `mt_dex2jar` — Multi-threaded DEX to JAR conversion
- `jar2dex` — Convert JAR to DEX
- `baksmali` — Disassemble DEX to smali
- `dex2smali` — Disassemble DEX to smali (alias)
- `smali` — Assemble smali to DEX
- `jar2jasmin` — Decompile JAR to Jasmin
- `jasmin2jar` — Assemble Jasmin to JAR
- `apk_sign` — Sign an APK
- `jar_access` — Access JAR/APK contents
- `decrypt_string` — Decrypt encrypted strings
- `asm_verify` — Verify assembly correctness
- `init_deobf` — Generate deobfuscation config
- `std_apk` — Clean up APK to standard zip
- `dex_recompute_checksum` — Recompute DEX checksums
- `dex_weaver` — Replace invoke in DEX
- `jar_weaver` — Replace invoke in JAR
- `class_version_switch` — Switch .class file version
- `generate_stub_from_odex` — Generate stub from ODEX
- `extract_odex_from_coredump` — Extract ODEX from coredump
- `dex_asmifier` — Generate ASMifier output
- `dex_inspect` — Inspect DEX/APK structure (classes, methods, fields)
- `dex_strings` — Extract string constants from DEX/APK
- `dex_method_trace` — Build method call graph from DEX/APK
- `dex_class_deps` — Analyze class dependencies in DEX/APK
- `dex_xref` — Reverse cross-reference: find symbol / sensitive-API usage sites
- `manifest_inspect` — Parse binary AndroidManifest.xml (perms, exported components)
- `apk_cert` — Read v1 signing certificate (subject, validity, fingerprints)
- `native_libs` — Enumerate .so libs (ABI, ELF identity, exported JNI symbols)
- `run_d2j_command` — Run any command with custom args (fallback)

## Configuration

### Claude Desktop

Add to `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "dex2jar": {
      "command": "python3",
      "args": ["path/to/dex2jar-skills/dex2jar-mcp/server.py"],
      "env": {
        "D2J_AI_PATH": "path/to/dex2jar-skills/d2j-ai.py"
      }
    }
  }
}
```

### Claude Code

Run `./dex2jar-mcp/install.sh` or manually add to `.claude/settings.json`.

## Architecture

```
MCP Client (Claude)
    ↓ stdio JSON-RPC
MCP Server (dex2jar-mcp/server.py)
    ↓ subprocess JSON
d2j-ai CLI (d2j-ai.py)
    ↓ subprocess
dex2jar Java tools (d2j-*.sh)
```

The MCP server wraps `d2j-ai` as a thin proxy — it does not reimplement any dex2jar functionality. All tool calls are forwarded to `d2j-ai.py` which returns structured JSON with `success`, `error_code`, `output_path`, `duration_ms`, and `timestamp` fields.

# dex2jar MCP Server

MCP (Model Context Protocol) server that exposes dex2jar reverse engineering tools to AI assistants like Claude.

## Quick Start

```bash
# Install MCP SDK
pip install mcp

# Run the server (stdio transport)
python dex2jar-mcp/server.py
```

## Features

| Type | Count | Examples |
|------|-------|---------|
| **Tools** | 14 | `dex2jar`, `baksmali`, `apk_sign`, `decrypt_string` |
| **Resources** | 3 | Command list, command info, version info |
| **Prompts** | 4 | `analyze_apk`, `patch_and_rebuild`, `resign_apk`, `compare_dex` |

## Tools

- `list_commands` — List all available dex2jar commands
- `command_info` — Get detailed info about a command
- `dex2jar` — Convert DEX/APK to JAR
- `jar2dex` — Convert JAR to DEX
- `baksmali` — Disassemble DEX to smali
- `smali` — Assemble smali to DEX
- `jar2jasmin` — Decompile JAR to Jasmin
- `jasmin2jar` — Assemble Jasmin to JAR
- `apk_sign` — Sign an APK
- `jar_access` — Access JAR/APK contents
- `decrypt_string` — Decrypt encrypted strings in DEX
- `asm_verify` — Verify assembly correctness
- `run_d2j_command` — Run any dex2jar command with custom args

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

The MCP server wraps `d2j-ai` as a thin proxy — it does not reimplement any dex2jar functionality. All tool calls are forwarded to `d2j-ai.py` which returns structured JSON, and the MCP server passes that JSON back to the client.
#!/usr/bin/env bash
# Install dex2jar MCP server for Claude Desktop / Claude Code
#
# Usage:
#   ./dex2jar-mcp/install.sh              # install with default settings
#   ./dex2jar-mcp/install.sh --global     # install globally (all projects)
#   D2J_AI_PATH=/custom/path ./mcp/install.sh  # custom d2j-ai location

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Detect Python
PYTHON="${PYTHON:-python3}"
if ! command -v "$PYTHON" &>/dev/null; then
    echo "ERROR: python3 not found. Install Python 3.10+ first." >&2
    exit 1
fi

# Install mcp dependency
echo "Installing MCP SDK..."
pip install -q mcp 2>/dev/null || pip3 install -q mcp 2>/dev/null || {
    echo "ERROR: Failed to install mcp package. Try: pip install mcp" >&2
    exit 1
}

# Determine d2j-ai path
D2J_AI_PATH="${D2J_AI_PATH:-$REPO_ROOT/d2j-ai.py}"
if [ ! -f "$D2J_AI_PATH" ]; then
    echo "WARNING: d2j-ai.py not found at $D2J_AI_PATH" >&2
    echo "  Set D2J_AI_PATH environment variable to the correct location." >&2
fi

# Determine config target
GLOBAL="${1:-}"
if [ "$GLOBAL" = "--global" ]; then
    CONFIG_DIR="$HOME/.claude"
    CONFIG_FILE="$CONFIG_DIR/settings.json"
else
    CONFIG_DIR="$REPO_ROOT/.claude"
    CONFIG_FILE="$CONFIG_DIR/settings.json"
fi

mkdir -p "$CONFIG_DIR"

# Build MCP server config
SERVER_PATH="$SCRIPT_DIR/server.py"

echo ""
echo "═══════════════════════════════════════════════════════"
echo "  dex2jar MCP Server Installation"
echo "═══════════════════════════════════════════════════════"
echo ""
echo "  Server:    $SERVER_PATH"
echo "  d2j-ai:    $D2J_AI_PATH"
echo "  Config:    $CONFIG_FILE"
echo ""

# Output Claude Desktop config snippet
CLAUDE_DESKTOP_CONFIG=$(cat <<EOF
Add this to your Claude Desktop config (claude_desktop_config.json):

{
  "mcpServers": {
    "dex2jar": {
      "command": "$PYTHON",
      "args": ["$SERVER_PATH"],
      "env": {
        "D2J_AI_PATH": "$D2J_AI_PATH"
      }
    }
  }
}
EOF
)

echo "$CLAUDE_DESKTOP_CONFIG"
echo ""

# Output Claude Code config snippet
CLAUDE_CODE_CONFIG=$(cat <<EOF
Or for Claude Code, add to .claude/settings.json:

{
  "mcpServers": {
    "dex2jar": {
      "command": "$PYTHON",
      "args": ["$SERVER_PATH"],
      "env": {
        "D2J_AI_PATH": "$D2J_AI_PATH"
      }
    }
  }
}
EOF
)

echo "$CLAUDE_CODE_CONFIG"
echo ""
echo "Installation complete. Restart Claude Desktop/Code to load the MCP server."

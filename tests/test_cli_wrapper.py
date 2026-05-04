#!/usr/bin/env python3
"""Tests for ai_dex2jar CLI wrapper."""

import json
import subprocess
import sys
from pathlib import Path

CLI_PATH = Path(__file__).parent.parent / "cli" / "ai_dex2jar.py"


def run_cli(*args: str) -> dict:
    """Run the CLI and parse JSON output."""
    result = subprocess.run(
        [sys.executable, str(CLI_PATH)] + list(args),
        capture_output=True,
        text=True,
    )
    try:
        return json.loads(result.stdout)
    except json.JSONDecodeError:
        return {"success": False, "raw_stdout": result.stdout, "raw_stderr": result.stderr, "returncode": result.returncode}


def test_help():
    """help command returns success with command list"""
    result = run_cli("help")
    assert result["success"] is True
    assert "dex2jar" in result["commands"]
    assert "baksmali" in result["commands"]
    assert "apk-sign" in result["commands"]


def test_list():
    """list command returns available commands"""
    result = run_cli("list")
    assert result["success"] is True
    assert "dex2jar" in result["available_commands"]
    assert len(result["available_commands"]) >= 10


def test_dex2jar_no_args():
    """dex2jar without args returns usage info"""
    result = run_cli("dex2jar")
    assert result["success"] is False
    assert "Usage" in result["error"] or "usage" in result.get("error", "").lower()


def test_unknown_command():
    """unknown command returns error with available commands"""
    result = run_cli("nonexistent")
    assert result["success"] is False
    assert "available_commands" in result
    assert "dex2jar" in result["available_commands"]


def test_plugin_json_exists():
    """plugin.json exists and is valid"""
    plugin_path = Path(__file__).parent.parent / ".claude-plugin" / "plugin.json"
    assert plugin_path.exists(), f"plugin.json not found at {plugin_path}"
    data = json.loads(plugin_path.read_text())
    assert data["name"] == "ai-dex2jar"
    assert "description" in data


def test_skill_command_exists():
    """dex2jar.md skill command exists"""
    skill_path = Path(__file__).parent.parent / "commands" / "dex2jar.md"
    assert skill_path.exists(), f"Skill command not found at {skill_path}"
    content = skill_path.read_text()
    assert "dex2jar" in content
    assert "allowed-tools" in content


def test_agent_exists():
    """reverse-engineer.md agent exists"""
    agent_path = Path(__file__).parent.parent / "agents" / "reverse-engineer.md"
    assert agent_path.exists(), f"Agent not found at {agent_path}"
    content = agent_path.read_text()
    assert "reverse-engineer" in content


if __name__ == "__main__":
    tests = [test_help, test_list, test_dex2jar_no_args, test_unknown_command,
             test_plugin_json_exists, test_skill_command_exists, test_agent_exists]
    passed = 0
    failed = 0
    for t in tests:
        try:
            t()
            print(f"  PASS  {t.__doc__}")
            passed += 1
        except AssertionError as e:
            print(f"  FAIL  {t.__doc__}: {e}")
            failed += 1
        except Exception as e:
            print(f"  ERROR {t.__doc__}: {e}")
            failed += 1
    print(f"\n{passed} passed, {failed} failed")
    sys.exit(1 if failed else 0)

#!/usr/bin/env python3
"""Tests for d2j-ai CLI wrapper."""

import json
import subprocess
import sys
from pathlib import Path

CLI = Path(__file__).parent.parent / "d2j-ai.py"


def run(*args: str) -> dict:
    r = subprocess.run([sys.executable, str(CLI)] + list(args), capture_output=True, text=True)
    try:
        return json.loads(r.stdout)
    except json.JSONDecodeError:
        return {"success": False, "stdout": r.stdout, "stderr": r.stderr, "returncode": r.returncode}


def test_help():
    """help returns command list"""
    r = run("help")
    assert r["success"] is True
    assert "dex2jar" in r["commands"]
    assert "baksmali" in r["commands"]


def test_list():
    """list returns available commands"""
    r = run("list")
    assert r["success"] is True
    assert "dex2jar" in r["available_commands"]
    assert len(r["available_commands"]) == 15


def test_dex2jar_no_args():
    """dex2jar without args returns usage"""
    r = run("dex2jar")
    assert r["success"] is False
    assert "Usage" in r["error"]


def test_unknown_command():
    """unknown command returns error with suggestions"""
    r = run("nonexistent")
    assert r["success"] is False
    assert "available_commands" in r


def test_plugin_json():
    """plugin.json exists and is valid"""
    p = Path(__file__).parent.parent / ".claude-plugin" / "plugin.json"
    assert p.exists()
    d = json.loads(p.read_text())
    assert d["name"] == "dex2jar"


def test_commands_dir():
    """all expected command files exist"""
    cmds_dir = Path(__file__).parent.parent / "commands"
    expected = ["dex2jar.md", "baksmali.md", "jar2dex.md", "jar-access.md", "apk-sign.md", "list-tools.md"]
    for name in expected:
        assert (cmds_dir / name).exists(), f"Missing {name}"


def test_command_frontmatter():
    """command files have required frontmatter fields"""
    cmds_dir = Path(__file__).parent.parent / "commands"
    for f in cmds_dir.glob("*.md"):
        content = f.read_text()
        assert "description:" in content, f"{f.name} missing description"
        assert "allowed-tools:" in content, f"{f.name} missing allowed-tools"


if __name__ == "__main__":
    tests = [test_help, test_list, test_dex2jar_no_args, test_unknown_command,
             test_plugin_json, test_commands_dir, test_command_frontmatter]
    p = f = 0
    for t in tests:
        try:
            t()
            print(f"  PASS  {t.__doc__}")
            p += 1
        except AssertionError as e:
            print(f"  FAIL  {t.__doc__}: {e}")
            f += 1
    print(f"\n{p} passed, {f} failed")
    sys.exit(1 if f else 0)

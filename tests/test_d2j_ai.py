#!/usr/bin/env python3
"""Tests for d2j-ai CLI wrapper."""

import json
import subprocess
import sys
import tempfile
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
    assert "info" in r["special_commands"]
    assert "batch" in r["special_commands"]
    assert "--pretty" in r["global_options"]
    assert "--verbose" in r["global_options"]


def test_list():
    """list returns available commands"""
    r = run("list")
    assert r["success"] is True
    assert "dex2jar" in r["available_commands"]
    assert "dex-inspect" in r["available_commands"]
    assert "dex-strings" in r["available_commands"]
    assert "dex-method-trace" in r["available_commands"]
    assert "dex-class-deps" in r["available_commands"]
    assert len(r["available_commands"]) == 25
    assert "error_code" in r
    assert r["error_code"] == "none"


def test_dex2jar_no_args():
    """dex2jar without args returns usage"""
    r = run("dex2jar")
    assert r["success"] is False
    assert "Usage" in r["error"]
    assert r["error_code"] == "invalid_format"
    assert "options" in r


def test_unknown_command():
    """unknown command returns error with suggestions"""
    r = run("nonexistent")
    assert r["success"] is False
    assert "available_commands" in r
    assert r["error_code"] == "unknown_command"


def test_dex_inspect_no_args():
    """dex-inspect without args returns usage"""
    r = run("dex-inspect")
    assert r["success"] is False
    assert "Usage" in r["error"]
    assert r["error_code"] == "invalid_format"
    assert "options" in r


def test_dex_inspect_validate_input():
    """dex-inspect validates input file exists"""
    r = run("dex-inspect", "/nonexistent/classes.dex")
    assert r["success"] is False
    assert "not found" in r.get("error_message", r.get("error", "")).lower()


def test_info_includes_dex_inspect():
    """info all includes dex-inspect"""
    r = run("info")
    assert r["success"] is True
    names = [c["name"] for c in r["commands"]]
    assert "dex-inspect" in names


def test_new_dex_commands_no_args():
    """new dex analysis commands return usage without args"""
    for cmd in ("dex-strings", "dex-method-trace", "dex-class-deps"):
        r = run(cmd)
        assert r["success"] is False, f"{cmd} should fail with no args"
        assert "Usage" in r["error"], f"{cmd} missing usage"
        assert r["error_code"] == "invalid_format", f"{cmd} wrong error_code"
        assert "options" in r, f"{cmd} missing options"


def test_new_dex_commands_validate_input():
    """new dex analysis commands validate input file exists"""
    for cmd in ("dex-strings", "dex-method-trace", "dex-class-deps"):
        r = run(cmd, "/nonexistent/classes.dex")
        assert r["success"] is False, f"{cmd} should fail on missing file"
        msg = r.get("error_message", r.get("error", "")).lower()
        assert "not found" in msg, f"{cmd} should report not found"
        assert r["error_code"] == "file_not_found", f"{cmd} wrong error_code"


def test_info_includes_new_dex_commands():
    """info all includes the new dex analysis commands"""
    r = run("info")
    assert r["success"] is True
    names = [c["name"] for c in r["commands"]]
    for cmd in ("dex-strings", "dex-method-trace", "dex-class-deps"):
        assert cmd in names, f"{cmd} missing from info"


def test_info_all():
    """info without args returns all commands info"""
    r = run("info")
    assert r["success"] is True
    assert "commands" in r
    assert len(r["commands"]) == 25
    # Each entry should have name, description
    for cmd_info in r["commands"]:
        assert "name" in cmd_info
        assert "description" in cmd_info


def test_info_specific():
    """info for a specific command returns details"""
    r = run("info", "dex2jar")
    assert r["success"] is True
    assert r["name"] == "dex2jar"
    assert "options" in r
    assert "usage" in r


def test_info_unknown():
    """info for unknown command returns error"""
    r = run("info", "nonexistent")
    assert r["success"] is False
    assert r["error_code"] == "unknown_command"
    assert "available_commands" in r


def test_batch_no_args():
    """batch without args returns usage"""
    r = run("batch")
    assert r["success"] is False
    assert "Usage" in r["error"]
    assert r["error_code"] == "invalid_format"


def test_batch_missing_file():
    """batch with missing file returns error"""
    r = run("batch", "/nonexistent/batch.json")
    assert r["success"] is False
    assert "not found" in r["error"].lower()
    assert r["error_code"] == "file_not_found"


def test_batch_invalid_json():
    """batch with invalid JSON returns error"""
    with tempfile.NamedTemporaryFile(mode="w", suffix=".json", delete=False) as f:
        f.write("not valid json {{{")
        f.flush()
        r = run("batch", f.name)
    assert r["success"] is False
    assert r["error_code"] == "invalid_format"


def test_batch_valid_execution():
    """batch with valid JSON executes commands"""
    batch_content = json.dumps([
        {"command": "list", "args": []},
    ])
    with tempfile.NamedTemporaryFile(mode="w", suffix=".json", delete=False) as f:
        f.write(batch_content)
        f.flush()
        r = run("batch", f.name)
    assert r["success"] is True
    assert r["total"] == 1
    assert r["success_count"] == 1
    assert r["failure_count"] == 0


def test_validate_input():
    """input validation catches missing files"""
    r = run("dex2jar", "/nonexistent/file.apk")
    assert r["success"] is False
    assert "not found" in r.get("error_message", r.get("error", "")).lower()


def test_structured_error_codes():
    """all error results include error_code field"""
    # Unknown command
    r = run("nonexistent")
    assert "error_code" in r

    # No args
    r = run("dex2jar")
    assert "error_code" in r

    # Successful list
    r = run("list")
    assert "error_code" in r


def test_result_enhancements():
    """successful results include duration_ms and timestamp"""
    r = run("list")
    assert r["success"] is True
    assert "duration_ms" in r
    assert isinstance(r["duration_ms"], int)
    assert "timestamp" in r


def test_pretty_flag():
    """--pretty flag produces indented output"""
    r = subprocess.run(
        [sys.executable, str(CLI), "--pretty", "list"],
        capture_output=True, text=True
    )
    output = r.stdout
    # Pretty output should have indentation
    assert '  "' in output  # indented key


def test_plugin_json():
    """plugin.json exists and is valid"""
    p = Path(__file__).parent.parent / ".claude-plugin" / "plugin.json"
    assert p.exists()
    d = json.loads(p.read_text())
    assert d["name"] == "dex2jar"


def test_commands_dir():
    """all expected command files exist"""
    cmds_dir = Path(__file__).parent.parent / "commands"
    expected = ["dex2jar.md", "baksmali.md", "jar2dex.md", "jar-access.md",
                "apk-sign.md", "list-tools.md", "info.md", "batch.md",
                "dex-inspect.md", "dex-strings.md", "dex-method-trace.md",
                "dex-class-deps.md"]
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
    tests = [
        test_help, test_list, test_dex2jar_no_args, test_unknown_command,
        test_info_all, test_info_specific, test_info_unknown,
        test_batch_no_args, test_batch_missing_file, test_batch_invalid_json,
        test_batch_valid_execution,
        test_validate_input, test_structured_error_codes,
        test_result_enhancements, test_pretty_flag,
        test_dex_inspect_no_args, test_dex_inspect_validate_input,
        test_info_includes_dex_inspect,
        test_new_dex_commands_no_args, test_new_dex_commands_validate_input,
        test_info_includes_new_dex_commands,
        test_plugin_json, test_commands_dir, test_command_frontmatter,
    ]
    p = f = 0
    for t in tests:
        try:
            t()
            print(f"  PASS  {t.__doc__}")
            p += 1
        except AssertionError as e:
            print(f"  FAIL  {t.__doc__}: {e}")
            f += 1
        except Exception as e:
            print(f"  ERROR {t.__doc__}: {e}")
            f += 1
    print(f"\n{p} passed, {f} failed")
    sys.exit(1 if f else 0)

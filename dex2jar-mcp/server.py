#!/usr/bin/env python3
"""
dex2jar MCP Server — exposes dex2jar reverse engineering tools as MCP tools/resources/prompts.

Transport: stdio (for use with Claude Desktop, Claude Code, or any MCP client)
Dependencies: mcp (pip install mcp)
"""

import json
import subprocess
import sys
import os
from pathlib import Path

from mcp.server.fastmcp import FastMCP


def _find_d2j_ai() -> str:
    """Find the d2j-ai executable path."""
    env_path = os.environ.get("D2J_AI_PATH")
    if env_path and Path(env_path).is_file():
        return env_path
    repo_root = Path(__file__).resolve().parent.parent
    candidate = repo_root / "d2j-ai.py"
    if candidate.is_file():
        return str(candidate)
    import shutil
    which = shutil.which("d2j-ai")
    if which:
        return which
    return "d2j-ai.py"


D2J_AI_PATH = _find_d2j_ai()

mcp = FastMCP(
    "dex2jar",
    instructions=(
        "dex2jar reverse engineering toolkit. "
        "Use list_commands to see all available commands. "
        "Common workflows: "
        "1) APK analysis: dex2jar -> jar2jasmin to decompile; "
        "2) DEX patching: baksmali -> edit -> smali; "
        "3) APK signing: jar-access to modify -> apk-sign to resign. "
        "All tools return structured JSON with success/error/output_path fields."
    ),
)


def _run_d2j_ai(command: str, args: list[str], timeout: int = 300) -> dict:
    """Invoke d2j-ai CLI and parse JSON output."""
    cmd = [sys.executable, D2J_AI_PATH, command] + args
    try:
        result = subprocess.run(
            cmd, capture_output=True, text=True, timeout=timeout,
        )
        output = result.stdout.strip()
        if output:
            try:
                return json.loads(output)
            except json.JSONDecodeError:
                return {
                    "success": False,
                    "error_message": f"Non-JSON output: {output[:500]}",
                    "stdout": output,
                    "stderr": result.stderr,
                    "returncode": result.returncode,
                }
        return {
            "success": False,
            "error_message": result.stderr[:500] if result.stderr else "No output",
            "stderr": result.stderr,
            "returncode": result.returncode,
        }
    except subprocess.TimeoutExpired:
        return {"success": False, "error_message": f"Command timed out after {timeout}s"}
    except FileNotFoundError:
        return {"success": False, "error_message": f"d2j-ai not found at {D2J_AI_PATH}"}


# ── Tools ──────────────────────────────────────────────────────────────────

@mcp.tool()
def list_commands() -> str:
    """List all available dex2jar commands with descriptions.

    Returns a JSON array of command names and their descriptions.
    Use this to discover what tools are available before running them.
    """
    result = _run_d2j_ai("list", [])
    return json.dumps(result, indent=2)


@mcp.tool()
def command_info(command: str) -> str:
    """Get detailed information about a specific dex2jar command.

    Shows the command's description, available options, and their types.

    Args:
        command: The command name (e.g. "dex2jar", "baksmali", "apk-sign")
    """
    result = _run_d2j_ai("info", [command])
    return json.dumps(result, indent=2)


@mcp.tool()
def dex2jar(input_file: str, output_file: str = "", extra_args: list[str] | None = None) -> str:
    """Convert DEX/APK files to JAR format.

    Primary tool for converting Android DEX bytecode to Java JAR files.

    Args:
        input_file: Path to the input .dex or .apk file
        output_file: Optional output JAR path (auto-generated if omitted)
        extra_args: Additional flags (e.g. ["--no-code"] to skip method bodies)
    """
    args = [input_file]
    if output_file:
        args.extend(["-o", output_file])
    if extra_args:
        args.extend(extra_args)
    result = _run_d2j_ai("dex2jar", args)
    return json.dumps(result, indent=2)


@mcp.tool()
def jar2dex(input_file: str, output_file: str = "", extra_args: list[str] | None = None) -> str:
    """Convert JAR files to DEX format.

    Reverse of dex2jar — converts Java JAR back to Android DEX bytecode.

    Args:
        input_file: Path to the input .jar file
        output_file: Optional output DEX path
        extra_args: Additional flags
    """
    args = [input_file]
    if output_file:
        args.extend(["-o", output_file])
    if extra_args:
        args.extend(extra_args)
    result = _run_d2j_ai("jar2dex", args)
    return json.dumps(result, indent=2)


@mcp.tool()
def baksmali(input_file: str, output_dir: str = "", extra_args: list[str] | None = None) -> str:
    """Disassemble DEX files to smali format.

    Converts DEX bytecode into human-readable smali assembly language.

    Args:
        input_file: Path to the input .dex or .apk file
        output_dir: Optional output directory for smali files
        extra_args: Additional flags (e.g. ["-r"] for debug info)
    """
    args = [input_file]
    if output_dir:
        args.extend(["-o", output_dir])
    if extra_args:
        args.extend(extra_args)
    result = _run_d2j_ai("baksmali", args)
    return json.dumps(result, indent=2)


@mcp.tool()
def smali(input_dir: str, output_file: str = "", extra_args: list[str] | None = None) -> str:
    """Assemble smali files back to DEX format.

    Reassembles edited smali files back into DEX bytecode.

    Args:
        input_dir: Path to directory containing smali files
        output_file: Optional output DEX path
        extra_args: Additional flags
    """
    args = [input_dir]
    if output_file:
        args.extend(["-o", output_file])
    if extra_args:
        args.extend(extra_args)
    result = _run_d2j_ai("smali", args)
    return json.dumps(result, indent=2)


@mcp.tool()
def jar2jasmin(input_file: str, output_dir: str = "", extra_args: list[str] | None = None) -> str:
    """Decompile JAR to Jasmin assembly format.

    Converts Java bytecode to Jasmin assembly language for analysis.

    Args:
        input_file: Path to the input .jar file
        output_dir: Optional output directory for Jasmin files
        extra_args: Additional flags
    """
    args = [input_file]
    if output_dir:
        args.extend(["-o", output_dir])
    if extra_args:
        args.extend(extra_args)
    result = _run_d2j_ai("jar2jasmin", args)
    return json.dumps(result, indent=2)


@mcp.tool()
def jasmin2jar(input_dir: str, output_file: str = "", extra_args: list[str] | None = None) -> str:
    """Assemble Jasmin files back to JAR format.

    Args:
        input_dir: Path to directory containing Jasmin files
        output_file: Optional output JAR path
        extra_args: Additional flags
    """
    args = [input_dir]
    if output_file:
        args.extend(["-o", output_file])
    if extra_args:
        args.extend(extra_args)
    result = _run_d2j_ai("jasmin2jar", args)
    return json.dumps(result, indent=2)


@mcp.tool()
def apk_sign(input_file: str, keystore: str = "", key_alias: str = "", extra_args: list[str] | None = None) -> str:
    """Sign an APK file with a keystore.

    Args:
        input_file: Path to the APK file to sign
        keystore: Path to the keystore file
        key_alias: Key alias within the keystore
        extra_args: Additional flags
    """
    args = [input_file]
    if keystore:
        args.extend(["--ks", keystore])
    if key_alias:
        args.extend(["--ks-key-alias", key_alias])
    if extra_args:
        args.extend(extra_args)
    result = _run_d2j_ai("apk-sign", args)
    return json.dumps(result, indent=2)


@mcp.tool()
def jar_access(input_file: str, operation: str, extra_args: list[str] | None = None) -> str:
    """Access and modify JAR/APK file contents.

    Operations: list, extract, add, delete entries in JAR/APK files.

    Args:
        input_file: Path to the JAR/APK file
        operation: Operation to perform (list/extract/add/delete)
        extra_args: Additional flags and arguments
    """
    args = [input_file, operation]
    if extra_args:
        args.extend(extra_args)
    result = _run_d2j_ai("jar-access", args)
    return json.dumps(result, indent=2)


@mcp.tool()
def decrypt_string(input_file: str, extra_args: list[str] | None = None) -> str:
    """Decrypt encrypted strings in DEX files.

    Attempts to decrypt string encryption in obfuscated Android apps.

    Args:
        input_file: Path to the input .dex file
        extra_args: Additional flags
    """
    args = [input_file]
    if extra_args:
        args.extend(extra_args)
    result = _run_d2j_ai("decrypt-string", args)
    return json.dumps(result, indent=2)


@mcp.tool()
def asm_verify(input_file: str, extra_args: list[str] | None = None) -> str:
    """Verify DEX/JAR assembly correctness.

    Args:
        input_file: Path to the file to verify
        extra_args: Additional flags
    """
    args = [input_file]
    if extra_args:
        args.extend(extra_args)
    result = _run_d2j_ai("asm-verify", args)
    return json.dumps(result, indent=2)


@mcp.tool()
def run_d2j_command(command: str, args: list[str] | None = None, timeout: int = 300) -> str:
    """Run any dex2jar command with custom arguments.

    Use for commands not covered by dedicated tools, or for fine-grained control.

    Args:
        command: The dex2jar command name (e.g. "dex-weaver", "jar-weaver")
        args: Arguments to pass to the command
        timeout: Execution timeout in seconds (default 300)
    """
    result = _run_d2j_ai(command, args or [], timeout=timeout)
    return json.dumps(result, indent=2)


# ── Resources ──────────────────────────────────────────────────────────────

@mcp.resource("d2j://commands")
def get_commands_resource() -> str:
    """Available dex2jar commands and their descriptions."""
    result = _run_d2j_ai("list", [])
    return json.dumps(result, indent=2)


@mcp.resource("d2j://commands/{command_name}/info")
def get_command_info_resource(command_name: str) -> str:
    """Detailed info about a specific command including options and flags.

    Args:
        command_name: The command name (e.g. "dex2jar", "baksmali")
    """
    result = _run_d2j_ai("info", [command_name])
    return json.dumps(result, indent=2)


@mcp.resource("d2j://version")
def get_version() -> str:
    """dex2jar version and build information."""
    return json.dumps({
        "name": "dex2jar",
        "version": "2.x",
        "mcp_server_version": "1.0.0",
        "tools_count": 15,
    }, indent=2)


# ── Prompts ────────────────────────────────────────────────────────────────

@mcp.prompt()
def analyze_apk(apk_path: str) -> str:
    """Analyze an Android APK file — decompile and examine its contents.

    Guides a complete APK analysis workflow:
    convert APK to JAR, decompile, examine manifest, check for obfuscation.

    Args:
        apk_path: Path to the APK file to analyze
    """
    return (
        f"I need to analyze the Android APK at: {apk_path}\n\n"
        "Please perform the following analysis workflow:\n\n"
        "1. **Convert to JAR**: Use `dex2jar` to convert the APK to a JAR file\n"
        "2. **Decompile**: Use `jar2jasmin` to decompile the JAR to readable assembly\n"
        "3. **List contents**: Use `jar_access` with 'list' to see all files in the APK\n"
        "4. **Check for obfuscation**: Look for encrypted strings using `decrypt_string`\n"
        "5. **Verify integrity**: Run `asm_verify` on the output\n\n"
        "After each step, report key findings. Summarize the app structure, "
        "entry points, and security-relevant observations at the end."
    )


@mcp.prompt()
def patch_and_rebuild(dex_path: str, description: str) -> str:
    """Patch a DEX file and rebuild — smali editing workflow.

    Disassemble to smali, edit, reassemble back to DEX.

    Args:
        dex_path: Path to the DEX file to patch
        description: Description of what changes to make
    """
    return (
        f"I need to patch the DEX file at: {dex_path}\n"
        f"Changes needed: {description}\n\n"
        "Please follow this workflow:\n\n"
        "1. **Disassemble**: Use `baksmali` to convert the DEX to smali format\n"
        "2. **Show structure**: List the resulting smali files\n"
        "3. **Wait for my instructions** on which smali files to modify\n"
        "4. **Reassemble**: Use `smali` to rebuild the modified smali into a DEX\n"
        "5. **Verify**: Run `asm_verify` on the output DEX\n\n"
        "Do NOT make any code changes until I specify what to modify."
    )


@mcp.prompt()
def resign_apk(apk_path: str, keystore_path: str = "") -> str:
    """Re-sign an APK with a new keystore.

    Args:
        apk_path: Path to the APK file to re-sign
        keystore_path: Path to the keystore file (uses default if omitted)
    """
    ks_instruction = f"the keystore at {keystore_path}" if keystore_path else "the default keystore"
    return (
        f"I need to re-sign the APK at: {apk_path}\n"
        f"Using {ks_instruction}\n\n"
        "Please:\n"
        "1. Use `apk_sign` to sign the APK\n"
        "2. Use `asm_verify` to verify the signed APK\n"
        "3. Report the result and any errors"
    )


@mcp.prompt()
def compare_dex(original: str, modified: str) -> str:
    """Compare two DEX files to understand differences.

    Useful for understanding what changed between versions of an app.

    Args:
        original: Path to the original DEX file
        modified: Path to the modified DEX file
    """
    return (
        f"I need to compare two DEX files:\n"
        f"- Original: {original}\n"
        f"- Modified: {modified}\n\n"
        "Please:\n"
        "1. Use `baksmali` on both files\n"
        "2. Compare the smali output to identify differences\n"
        "3. Summarize: new/removed/modified classes and methods\n"
        "4. Highlight any security-relevant changes"
    )


# ── Main ───────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    mcp.run()

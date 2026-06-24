#!/usr/bin/env python3
"""d2j-ai: AI-friendly CLI wrapper for dex2jar reverse engineering tools.

All output is structured JSON for easy parsing by AI agents.
"""

import json
import os
import re
import shutil
import subprocess
import sys
import time
from pathlib import Path
from typing import Any, Optional


# ── Structured error codes (mirrors Java CLI's ErrorCode enum) ──────────────

class ErrorCode:
    NONE = "none"
    FILE_NOT_FOUND = "file_not_found"
    INVALID_FORMAT = "invalid_format"
    COMMAND_FAILED = "command_failed"
    UNKNOWN_COMMAND = "unknown_command"


# ── Global options ──────────────────────────────────────────────────────────

PRETTY = False
VERBOSE = False


# ── Core utilities ──────────────────────────────────────────────────────────

def find_d2j_home() -> Path:
    """Locate dex2jar installation directory."""
    # 1. Explicit env var
    if env_home := os.environ.get("D2J_HOME"):
        p = Path(env_home)
        if p.is_dir():
            return p
    # 2. Relative to repo root (sibling of this script)
    repo_root = Path(__file__).resolve().parent
    # Check for extracted dist
    dist_dir = repo_root / "dex-tools" / "build" / "distributions"
    if dist_dir.is_dir():
        dirs = sorted(dist_dir.glob("dex-tools-*"))
        dirs = [d for d in dirs if d.is_dir()]
        if dirs:
            return dirs[-1]
        # Try extracting latest zip
        zips = sorted(dist_dir.glob("dex-tools-*.zip"))
        if zips:
            import tempfile
            extract_dir = Path(tempfile.mkdtemp(prefix="d2j-"))
            shutil.unpack_archive(str(zips[-1]), extract_dir)
            extracted = list(extract_dir.glob("dex-tools-*"))
            if extracted:
                return extracted[-1]
    # 3. Check PATH
    if shutil.which("d2j-dex2jar.sh"):
        return Path(shutil.which("d2j-dex2jar.sh")).parent.parent
    raise RuntimeError(
        "Cannot find dex2jar. Set D2J_HOME or build with: ./gradlew distZip"
    )


def validate_input(command: str, args: list[str]) -> Optional[str]:
    """Validate input file exists and is readable. Returns error message or None."""
    if not args:
        return None
    input_file = _find_input_file(args)
    if input_file is None:
        return None
    path = Path(input_file)
    if not path.exists():
        return f"File not found: {input_file}"
    if not path.is_dir() and not os.access(str(path), os.R_OK):
        return f"File not readable: {input_file}"
    return None


def _find_input_file(args: list[str]) -> Optional[str]:
    """Find the input file argument from a command's args list."""
    # First pass: look for known file extensions
    for arg in args:
        if arg.startswith("-"):
            continue
        if arg.endswith((".dex", ".apk", ".jar", ".zip", ".smali", ".j", ".odex")):
            return arg
    # Second pass: first non-flag argument
    for arg in args:
        if not arg.startswith("-"):
            return arg
    return None


def parse_output_path(stderr: str, stdout: str) -> Optional[str]:
    """Parse the output file path from command output.

    Looks for patterns like '... -> /path/to/output.jar'
    """
    combined = (stderr or "") + "\n" + (stdout or "")
    # Match patterns like "dex2jar ... -> /path/to/file.jar"
    pattern = re.compile(r'\S+\s+->\s+(\S+)')
    for line in reversed(combined.split("\n")):
        line = line.strip()
        m = pattern.search(line)
        if m:
            path = m.group(1)
            if not path.startswith("-") and not path.startswith("ERROR"):
                return path
    return None


def run_d2j_command(cmd_name: str, args: list[str]) -> dict[str, Any]:
    """Execute a d2j-* command and return structured JSON result."""
    # Input validation
    validation_error = validate_input(cmd_name, args)
    if validation_error:
        return {
            "success": False,
            "error_code": ErrorCode.FILE_NOT_FOUND,
            "error_message": validation_error,
            "command": cmd_name,
            "duration_ms": 0,
            "timestamp": _timestamp(),
        }

    try:
        d2j_home = find_d2j_home()
    except RuntimeError as e:
        return {
            "success": False,
            "error_code": ErrorCode.COMMAND_FAILED,
            "error_message": str(e),
            "command": cmd_name,
            "duration_ms": 0,
            "timestamp": _timestamp(),
        }

    bin_dir = d2j_home / "bin"
    script = bin_dir / f"{cmd_name}.sh"
    if not script.exists():
        return {
            "success": False,
            "error_code": ErrorCode.UNKNOWN_COMMAND,
            "error_message": f"Command {cmd_name} not found at {script}",
            "available_commands": list_available_commands(),
            "command": cmd_name,
            "duration_ms": 0,
            "timestamp": _timestamp(),
        }

    full_cmd = [str(script)] + args
    start_time = time.monotonic()
    try:
        result = subprocess.run(
            full_cmd, capture_output=True, text=True, timeout=300
        )
        duration_ms = int((time.monotonic() - start_time) * 1000)
        output_path = parse_output_path(result.stderr, result.stdout)
        response = {
            "success": result.returncode == 0,
            "error_code": ErrorCode.NONE if result.returncode == 0 else ErrorCode.COMMAND_FAILED,
            "returncode": result.returncode,
            "stdout": result.stdout,
            "stderr": result.stderr,
            "command": " ".join(full_cmd),
            "output_path": output_path,
            "duration_ms": duration_ms,
            "timestamp": _timestamp(),
        }
        if result.returncode != 0 and result.stderr:
            response["error_message"] = result.stderr.strip().split("\n")[-1][:200]
        return response
    except subprocess.TimeoutExpired:
        duration_ms = int((time.monotonic() - start_time) * 1000)
        return {
            "success": False,
            "error_code": ErrorCode.COMMAND_FAILED,
            "error_message": f"Command timed out after 300s",
            "command": " ".join(full_cmd),
            "duration_ms": duration_ms,
            "timestamp": _timestamp(),
        }
    except Exception as e:
        duration_ms = int((time.monotonic() - start_time) * 1000)
        return {
            "success": False,
            "error_code": ErrorCode.COMMAND_FAILED,
            "error_message": str(e),
            "command": cmd_name,
            "duration_ms": duration_ms,
            "timestamp": _timestamp(),
        }


def _timestamp() -> str:
    """Return ISO 8601 timestamp."""
    return time.strftime("%Y-%m-%dT%H:%M:%S%z")


def list_available_commands() -> list[str]:
    """List all available d2j commands from the installation."""
    try:
        d2j_home = find_d2j_home()
        bin_dir = d2j_home / "bin"
    except RuntimeError:
        return []
    if not bin_dir.is_dir():
        return []
    return sorted(f.stem for f in bin_dir.glob("d2j-*.sh"))


# ── Command definitions ─────────────────────────────────────────────────────

def cmd_dex2jar(args: list[str]) -> dict[str, Any]:
    """Convert .dex/.apk to .jar"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai dex2jar <file.apk|file.dex> [-o output.jar] [-f]",
            "description": "Convert dex to jar",
            "options": {
                "-o, --output": "Output .jar file path",
                "-f, --force": "Force overwrite",
                "-e, --exception-file": "Detail exception file",
                "-n, --not-handle-exception": "Not handle exceptions",
                "-d, --debug-info": "Translate debug info",
            },
        }
    return run_d2j_command("d2j-dex2jar", args)


def cmd_jar2dex(args: list[str]) -> dict[str, Any]:
    """Convert .jar to .dex"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai jar2dex <dir|jar> [-o output.dex]",
            "description": "Convert jar to dex by invoking dx",
            "options": {"-o, --output": "Output .dex file path", "-f, --force": "Force overwrite"},
        }
    return run_d2j_command("d2j-jar2dex", args)


def cmd_baksmali(args: list[str]) -> dict[str, Any]:
    """Disassemble .dex to smali files"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai baksmali <dex-file>",
            "description": "Disassemble dex to smali",
        }
    return run_d2j_command("d2j-baksmali", args)


def cmd_smali(args: list[str]) -> dict[str, Any]:
    """Assemble smali files into .dex"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai smali [<smali-file>|folder]*",
            "description": "Assemble smali files into a dex file",
        }
    return run_d2j_command("d2j-smali", args)


def cmd_apk_sign(args: list[str]) -> dict[str, Any]:
    """Sign an APK with test certificate"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai apk-sign <apk-file>",
            "description": "Sign an android apk with a test certificate",
            "options": {"-o, --output": "Output .apk file", "-f, --force": "Force overwrite"},
        }
    return run_d2j_command("d2j-apk-sign", args)


def cmd_jar_access(args: list[str]) -> dict[str, Any]:
    """Modify access flags in .jar"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai jar-access <jar-file> [-ac public]",
            "description": "Add or remove class/method/field access in jar",
            "options": {
                "-ac, --add-class-access": "Add access to classes",
                "-am, --add-method-access": "Add access to methods",
                "-af, --add-field-access": "Add access to fields",
                "-rc, --remove-class-access": "Remove access from classes",
                "-rm, --remove-method-access": "Remove access from methods",
                "-rf, --remove-field-access": "Remove access from fields",
            },
        }
    return run_d2j_command("d2j-jar-access", args)


def cmd_asm_verify(args: list[str]) -> dict[str, Any]:
    """Verify .class files in jar"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai asm-verify <jar0> [jar1 ...]",
            "description": "Verify .class in jar",
        }
    return run_d2j_command("d2j-asm-verify", args)


def cmd_jar2jasmin(args: list[str]) -> dict[str, Any]:
    """Disassemble .class to jasmin format"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai jar2jasmin <jar-file>",
            "description": "Disassemble .class in jar to jasmin",
        }
    return run_d2j_command("d2j-jar2jasmin", args)


def cmd_jasmin2jar(args: list[str]) -> dict[str, Any]:
    """Assemble jasmin files to .jar"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai jasmin2jar <dir>",
            "description": "Assemble jasmin files to jar",
        }
    return run_d2j_command("d2j-jasmin2jar", args)


def cmd_decrypt_string(args: list[str]) -> dict[str, Any]:
    """Decrypt strings in .class files"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai decrypt-string <jar-file>",
            "description": "Decrypt string in class file",
        }
    return run_d2j_command("d2j-decrypt-string", args)


def cmd_std_apk(args: list[str]) -> dict[str, Any]:
    """Clean up APK to standard zip"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai std-apk <apk-file>",
            "description": "Clean up apk to standard zip",
        }
    return run_d2j_command("d2j-std-apk", args)


def cmd_dex_recompute_checksum(args: list[str]) -> dict[str, Any]:
    """Recompute CRC and SHA1 of .dex"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai dex-recompute-checksum <dex-file>",
            "description": "Recompute crc and sha1 of dex",
        }
    return run_d2j_command("d2j-dex-recompute-checksum", args)


def cmd_dex_weaver(args: list[str]) -> dict[str, Any]:
    """Replace invoke in .dex"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai dex-weaver <dex-file>",
            "description": "Replace invoke in dex",
        }
    return run_d2j_command("d2j-dex-weaver", args)


def cmd_jar_weaver(args: list[str]) -> dict[str, Any]:
    """Replace invoke in .jar"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai jar-weaver <jar-file>",
            "description": "Replace invoke in jar",
        }
    return run_d2j_command("d2j-jar-weaver", args)


def cmd_class_version_switch(args: list[str]) -> dict[str, Any]:
    """Switch .class file version"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai class-version-switch <version> <old.jar> <new.jar>",
            "description": "Switch class file version in jar",
        }
    return run_d2j_command("d2j-class-version-switch", args)


def cmd_dex2smali(args: list[str]) -> dict[str, Any]:
    """Disassemble .dex to smali (alias for baksmali)"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai dex2smali <dex-file>",
            "description": "Disassemble dex to smali files",
        }
    return run_d2j_command("d2j-dex2smali", args)


def cmd_mt_dex2jar(args: list[str]) -> dict[str, Any]:
    """Convert .dex/.apk to .jar using multiple threads"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai mt-dex2jar <file0> [file1 ...] [-o output.jar]",
            "description": "Multi-threaded dex to jar conversion",
            "options": {
                "-o, --output": "Output .jar file path",
                "-f, --force": "Force overwrite",
                "-mt, --multi-thread": "Number of threads (default 4)",
                "-fl, --file-list": "File containing list of dex files to process",
            },
        }
    return run_d2j_command("d2j-mt-dex2jar", args)


def cmd_init_deobf(args: list[str]) -> dict[str, Any]:
    """Generate deobfuscation init config for .jar"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai init-deobf <jar-file>",
            "description": "Generate an init config file for de-obfuscating a jar",
            "options": {
                "-o, --output": "Output config file path",
                "-f, --force": "Force overwrite",
                "-min, --min-length": "Rename if name length < MIN (default 2)",
                "-max, --max-length": "Rename if name length > MAX (default 40)",
            },
        }
    return run_d2j_command("d2j-init-deobf", args)


def cmd_generate_stub_from_odex(args: list[str]) -> dict[str, Any]:
    """Generate no-code stub .jar from .odex"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai generate-stub-from-odex <odex0> [odex1 ...]",
            "description": "Generate no-code jar from odex files",
            "options": {
                "-o, --output": "Output .jar file (default stub.jar)",
                "-npri, --no-private": "Exclude private members",
            },
        }
    return run_d2j_command("d2j-generate-stub-from-odex", args)


def cmd_extract_odex_from_coredump(args: list[str]) -> dict[str, Any]:
    """Extract .odex from dalvik memory core dump"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai extract-odex-from-coredump <core.xxxx>",
            "description": "Extract odex from dalvik memory core dump",
        }
    return run_d2j_command("d2j-extract-odex-from-coredump", args)


def cmd_dex_asmifier(args: list[str]) -> dict[str, Any]:
    """Generate ASMifier output from .dex files"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai dex-asmifier <dex0> [dex1 ...]",
            "description": "Generate ASMifier Java source from dex files",
        }
    return run_d2j_command("d2j-dex-asmifier", args)


def _find_ai_cli_jar() -> Optional[Path]:
    """Locate the built dex2jar-ai-cli jar (with all dependencies)."""
    ai_cli_dir = Path(__file__).resolve().parent / "dex2jar-ai-cli" / "build" / "libs"
    if not ai_cli_dir.is_dir():
        return None
    # Prefer fat/shadow jar if present
    for pattern in ("dex2jar-ai-cli-*-all.jar", "dex2jar-ai-cli-*.jar"):
        jars = sorted(ai_cli_dir.glob(pattern))
        if jars:
            return jars[-1]
    return None


def run_java_cli(cmd_name: str, args: list[str], timeout: int = 120) -> Optional[dict[str, Any]]:
    """Invoke a dex2jar-ai-cli command via the Java D2jAiMain dispatcher.

    Returns parsed JSON dict on success, or None if the Java CLI is
    unavailable / produced non-JSON output (so callers can fall back).
    """
    java_cli_jar = _find_ai_cli_jar()
    if not java_cli_jar or not java_cli_jar.exists():
        return None
    if not shutil.which("java"):
        return None

    full_cmd = ["java", "-cp", str(java_cli_jar),
                "com.googlecode.d2j.ai.D2jAiMain", cmd_name] + args
    start_time = time.monotonic()
    try:
        result = subprocess.run(full_cmd, capture_output=True, text=True, timeout=timeout)
    except subprocess.TimeoutExpired:
        return {
            "success": False,
            "error_code": ErrorCode.COMMAND_FAILED,
            "error_message": f"Java CLI '{cmd_name}' timed out after {timeout}s",
            "command": cmd_name,
            "duration_ms": timeout * 1000,
            "timestamp": _timestamp(),
        }
    except Exception:
        return None

    duration_ms = int((time.monotonic() - start_time) * 1000)
    stdout = (result.stdout or "").strip()
    try:
        parsed = json.loads(stdout)
    except json.JSONDecodeError:
        return None
    if not isinstance(parsed, dict):
        parsed = {"result": parsed}
    parsed.setdefault("success", result.returncode == 0)
    parsed.setdefault("error_code", ErrorCode.NONE if result.returncode == 0 else ErrorCode.COMMAND_FAILED)
    parsed["duration_ms"] = duration_ms
    parsed["timestamp"] = _timestamp()
    return parsed


def cmd_dex_inspect(args: list[str]) -> dict[str, Any]:
    """Inspect DEX/APK structure — list classes, methods, fields"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai dex-inspect <file.dex|file.apk> [-f filter] [-d] [-s]",
            "description": "Inspect DEX/APK structure — list classes, methods, fields",
            "options": {
                "-f, --filter": "Filter class names by pattern (substring match)",
                "-d, --detail": "Show method and field details for each class",
                "-s, --strings": "Include string constants from fields",
                "-o, --output": "Output file path (default: stdout)",
            },
        }
    validation_error = validate_input("dex-inspect", args)
    if validation_error:
        return {
            "success": False,
            "error_code": ErrorCode.FILE_NOT_FOUND,
            "error_message": validation_error,
            "command": "dex-inspect",
            "duration_ms": 0,
            "timestamp": _timestamp(),
        }
    parsed = run_java_cli("dex-inspect", args)
    if parsed is not None:
        return parsed
    # Fallback: use baksmali to get class list (less detailed)
    return run_d2j_command("d2j-baksmali", args)


def cmd_dex_strings(args: list[str]) -> dict[str, Any]:
    """Extract string constants from .dex/.apk"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai dex-strings <file.dex|file.apk> [-f filter] [-c class] [-u]",
            "description": "Extract string constants from a DEX/APK file",
            "options": {
                "-f, --filter": "Filter strings by pattern (substring match)",
                "-c, --class": "Filter by class name (substring match)",
                "-u, --unique": "Only show unique strings (deduplicated)",
                "-o, --output": "Output file path (default: stdout)",
            },
        }
    validation_error = validate_input("dex-strings", args)
    if validation_error:
        return {
            "success": False,
            "error_code": ErrorCode.FILE_NOT_FOUND,
            "error_message": validation_error,
            "command": "dex-strings",
            "duration_ms": 0,
            "timestamp": _timestamp(),
        }
    parsed = run_java_cli("dex-strings", args)
    if parsed is not None:
        return parsed
    return {
        "success": False,
        "error_code": ErrorCode.COMMAND_FAILED,
        "error_message": "dex-strings requires the dex2jar-ai-cli Java module. Build with: ./gradlew :dex2jar-ai-cli:build",
        "command": "dex-strings",
        "duration_ms": 0,
        "timestamp": _timestamp(),
    }


def cmd_dex_method_trace(args: list[str]) -> dict[str, Any]:
    """Build method call graph from .dex/.apk"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai dex-method-trace <file.dex|file.apk> [-c class] [-t target]",
            "description": "Build a method call graph from a DEX/APK file",
            "options": {
                "-c, --class": "Only trace methods in classes matching this pattern",
                "-t, --target": "Find all callers of methods matching this pattern (owner.name)",
                "-o, --output": "Output file path (default: stdout)",
            },
        }
    validation_error = validate_input("dex-method-trace", args)
    if validation_error:
        return {
            "success": False,
            "error_code": ErrorCode.FILE_NOT_FOUND,
            "error_message": validation_error,
            "command": "dex-method-trace",
            "duration_ms": 0,
            "timestamp": _timestamp(),
        }
    parsed = run_java_cli("dex-method-trace", args)
    if parsed is not None:
        return parsed
    return {
        "success": False,
        "error_code": ErrorCode.COMMAND_FAILED,
        "error_message": "dex-method-trace requires the dex2jar-ai-cli Java module. Build with: ./gradlew :dex2jar-ai-cli:build",
        "command": "dex-method-trace",
        "duration_ms": 0,
        "timestamp": _timestamp(),
    }


def cmd_dex_class_deps(args: list[str]) -> dict[str, Any]:
    """Analyze class dependencies in .dex/.apk"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai dex-class-deps <file.dex|file.apk> [-c class] [-d] [-i]",
            "description": "Analyze class dependencies in a DEX/APK file",
            "options": {
                "-c, --class": "Only analyze classes matching this pattern",
                "-d, --deep": "Include dependencies from method bodies (invokes, field access)",
                "-i, --internal-only": "Only show dependencies on classes defined in this DEX",
                "-o, --output": "Output file path (default: stdout)",
            },
        }
    validation_error = validate_input("dex-class-deps", args)
    if validation_error:
        return {
            "success": False,
            "error_code": ErrorCode.FILE_NOT_FOUND,
            "error_message": validation_error,
            "command": "dex-class-deps",
            "duration_ms": 0,
            "timestamp": _timestamp(),
        }
    parsed = run_java_cli("dex-class-deps", args)
    if parsed is not None:
        return parsed
    return {
        "success": False,
        "error_code": ErrorCode.COMMAND_FAILED,
        "error_message": "dex-class-deps requires the dex2jar-ai-cli Java module. Build with: ./gradlew :dex2jar-ai-cli:build",
        "command": "dex-class-deps",
        "duration_ms": 0,
        "timestamp": _timestamp(),
    }


def cmd_dex_xref(args: list[str]) -> dict[str, Any]:
    """Reverse cross-reference: find usage sites of a symbol or sensitive-API preset"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai dex-xref <file.dex|file.apk> (--to <substr> | --preset <name>) [--kinds ...]",
            "description": "Find every site that references a target symbol or sensitive-API preset",
            "options": {
                "-t, --to": "Match references whose symbol contains this substring",
                "-p, --preset": "Sensitive-API preset: crypto | reflection | dynload | net",
                "-k, --kinds": "Comma-separated kinds to include: invoke,field,type,string (default: all)",
                "-o, --output": "Output file path (default: stdout)",
            },
        }
    validation_error = validate_input("dex-xref", args)
    if validation_error:
        return {
            "success": False,
            "error_code": ErrorCode.FILE_NOT_FOUND,
            "error_message": validation_error,
            "command": "dex-xref",
            "duration_ms": 0,
            "timestamp": _timestamp(),
        }
    parsed = run_java_cli("dex-xref", args)
    if parsed is not None:
        return parsed
    return {
        "success": False,
        "error_code": ErrorCode.COMMAND_FAILED,
        "error_message": "dex-xref requires the dex2jar-ai-cli Java module. Build with: ./gradlew :dex2jar-ai-cli:build",
        "command": "dex-xref",
        "duration_ms": 0,
        "timestamp": _timestamp(),
    }


def cmd_manifest_inspect(args: list[str]) -> dict[str, Any]:
    """Parse a binary AndroidManifest.xml from an APK (or standalone AXML)"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai manifest-inspect <file.apk|AndroidManifest.xml> [-e]",
            "description": "Inspect package, SDK levels, permissions, and exported components",
            "options": {
                "-e, --exported-only": "Only report exported components (the attack surface)",
                "-o, --output": "Output file path (default: stdout)",
            },
        }
    validation_error = validate_input("manifest-inspect", args)
    if validation_error:
        return {
            "success": False,
            "error_code": ErrorCode.FILE_NOT_FOUND,
            "error_message": validation_error,
            "command": "manifest-inspect",
            "duration_ms": 0,
            "timestamp": _timestamp(),
        }
    parsed = run_java_cli("manifest-inspect", args)
    if parsed is not None:
        return parsed
    return {
        "success": False,
        "error_code": ErrorCode.COMMAND_FAILED,
        "error_message": "manifest-inspect requires the dex2jar-ai-cli Java module. Build with: ./gradlew :dex2jar-ai-cli:build",
        "command": "manifest-inspect",
        "duration_ms": 0,
        "timestamp": _timestamp(),
    }


def cmd_apk_cert(args: list[str]) -> dict[str, Any]:
    """Read v1 signing certificates and fingerprints from an APK"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai apk-cert <file.apk>",
            "description": "Read v1 signing certificates: subject/issuer/serial/validity + SHA-256/SHA-1/MD5",
            "options": {
                "-o, --output": "Output file path (default: stdout)",
            },
        }
    validation_error = validate_input("apk-cert", args)
    if validation_error:
        return {
            "success": False,
            "error_code": ErrorCode.FILE_NOT_FOUND,
            "error_message": validation_error,
            "command": "apk-cert",
            "duration_ms": 0,
            "timestamp": _timestamp(),
        }
    parsed = run_java_cli("apk-cert", args)
    if parsed is not None:
        return parsed
    return {
        "success": False,
        "error_code": ErrorCode.COMMAND_FAILED,
        "error_message": "apk-cert requires the dex2jar-ai-cli Java module. Build with: ./gradlew :dex2jar-ai-cli:build",
        "command": "apk-cert",
        "duration_ms": 0,
        "timestamp": _timestamp(),
    }


def cmd_native_libs(args: list[str]) -> dict[str, Any]:
    """Enumerate native .so libraries and their exported JNI symbols"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai native-libs <file.apk|lib.so> [-s]",
            "description": "List lib/*/*.so — ABI, ELF identity, and exported JNI entry points",
            "options": {
                "-s, --symbols": "Include the full list of JNI symbol names per library",
                "-o, --output": "Output file path (default: stdout)",
            },
        }
    validation_error = validate_input("native-libs", args)
    if validation_error:
        return {
            "success": False,
            "error_code": ErrorCode.FILE_NOT_FOUND,
            "error_message": validation_error,
            "command": "native-libs",
            "duration_ms": 0,
            "timestamp": _timestamp(),
        }
    parsed = run_java_cli("native-libs", args)
    if parsed is not None:
        return parsed
    return {
        "success": False,
        "error_code": ErrorCode.COMMAND_FAILED,
        "error_message": "native-libs requires the dex2jar-ai-cli Java module. Build with: ./gradlew :dex2jar-ai-cli:build",
        "command": "native-libs",
        "duration_ms": 0,
        "timestamp": _timestamp(),
    }


# ── Special commands ────────────────────────────────────────────────────────

COMMANDS = {
    "dex2jar": cmd_dex2jar,
    "mt-dex2jar": cmd_mt_dex2jar,
    "jar2dex": cmd_jar2dex,
    "baksmali": cmd_baksmali,
    "dex2smali": cmd_dex2smali,
    "smali": cmd_smali,
    "apk-sign": cmd_apk_sign,
    "jar-access": cmd_jar_access,
    "asm-verify": cmd_asm_verify,
    "jar2jasmin": cmd_jar2jasmin,
    "jasmin2jar": cmd_jasmin2jar,
    "decrypt-string": cmd_decrypt_string,
    "init-deobf": cmd_init_deobf,
    "std-apk": cmd_std_apk,
    "dex-asmifier": cmd_dex_asmifier,
    "dex-recompute-checksum": cmd_dex_recompute_checksum,
    "dex-weaver": cmd_dex_weaver,
    "jar-weaver": cmd_jar_weaver,
    "class-version-switch": cmd_class_version_switch,
    "generate-stub-from-odex": cmd_generate_stub_from_odex,
    "extract-odex-from-coredump": cmd_extract_odex_from_coredump,
    "dex-inspect": cmd_dex_inspect,
    "dex-strings": cmd_dex_strings,
    "dex-method-trace": cmd_dex_method_trace,
    "dex-class-deps": cmd_dex_class_deps,
    "dex-xref": cmd_dex_xref,
    "manifest-inspect": cmd_manifest_inspect,
    "apk-cert": cmd_apk_cert,
    "native-libs": cmd_native_libs,
}


def cmd_list(_args: list[str]) -> dict[str, Any]:
    """List all available commands"""
    return {
        "success": True,
        "error_code": ErrorCode.NONE,
        "available_commands": list(COMMANDS.keys()),
        "installed_commands": list_available_commands(),
        "descriptions": {k: fn.__doc__ for k, fn in COMMANDS.items()},
        "duration_ms": 0,
        "timestamp": _timestamp(),
    }


def cmd_info(args: list[str]) -> dict[str, Any]:
    """Show detailed info about a command including options"""
    if not args:
        # Show all commands with descriptions and options
        commands_info = []
        for name, fn in COMMANDS.items():
            info = _get_command_info(name, fn)
            commands_info.append(info)
        return {
            "success": True,
            "error_code": ErrorCode.NONE,
            "commands": commands_info,
            "duration_ms": 0,
            "timestamp": _timestamp(),
        }

    command_name = args[0]
    if command_name not in COMMANDS and command_name not in SPECIAL_COMMANDS:
        return {
            "success": False,
            "error_code": ErrorCode.UNKNOWN_COMMAND,
            "error": f"Unknown command: {command_name}",
            "available_commands": list(COMMANDS.keys()) + list(SPECIAL_COMMANDS.keys()),
            "duration_ms": 0,
            "timestamp": _timestamp(),
        }

    fn = COMMANDS.get(command_name, SPECIAL_COMMANDS.get(command_name))
    info = _get_command_info(command_name, fn)
    return {
        "success": True,
        "error_code": ErrorCode.NONE,
        "duration_ms": 0,
        "timestamp": _timestamp(),
        **info,
    }


def _get_command_info(name: str, fn) -> dict[str, Any]:
    """Extract command info by calling it with no args to get usage/options."""
    # Call with empty args to get the usage info
    result = fn([])
    info = {
        "name": name,
        "description": fn.__doc__,
    }
    # Extract options from the no-args result if available
    if "options" in result:
        info["options"] = result["options"]
    if "error" in result:
        info["usage"] = result["error"]
    return info


def cmd_batch(args: list[str]) -> dict[str, Any]:
    """Execute multiple commands from a JSON file"""
    if not args:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Usage: d2j-ai batch <json-file>",
            "description": "Execute multiple commands from a JSON file",
            "format": {
                "example": [
                    {"command": "dex2jar", "args": ["app.apk"]},
                    {"command": "asm-verify", "args": ["app-dex2jar.jar"]},
                ],
            },
        }

    input_path = Path(args[0])
    if not input_path.exists():
        return {
            "success": False,
            "error_code": ErrorCode.FILE_NOT_FOUND,
            "error": f"Batch file not found: {args[0]}",
        }

    try:
        content = input_path.read_text(encoding="utf-8")
        entries = json.loads(content)
    except json.JSONDecodeError as e:
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": f"Invalid JSON in batch file: {e}",
        }
    except Exception as e:
        return {
            "success": False,
            "error_code": ErrorCode.COMMAND_FAILED,
            "error": f"Error reading batch file: {e}",
        }

    if not isinstance(entries, list):
        return {
            "success": False,
            "error_code": ErrorCode.INVALID_FORMAT,
            "error": "Batch file must contain a JSON array of command objects",
        }

    results = []
    for i, entry in enumerate(entries):
        if not isinstance(entry, dict) or "command" not in entry:
            results.append({
                "success": False,
                "error_code": ErrorCode.INVALID_FORMAT,
                "error": f"Entry {i}: missing 'command' field",
            })
            continue

        command = entry["command"]
        cmd_args = entry.get("args", [])

        if command in SPECIAL_COMMANDS:
            result = SPECIAL_COMMANDS[command](cmd_args)
        elif command in COMMANDS:
            result = COMMANDS[command](cmd_args)
        else:
            results.append({
                "success": False,
                "error_code": ErrorCode.UNKNOWN_COMMAND,
                "error": f"Entry {i}: unknown command '{command}'",
                "available_commands": list(COMMANDS.keys()) + list(SPECIAL_COMMANDS.keys()),
            })
            continue

        results.append(result)

    success_count = sum(1 for r in results if r.get("success"))
    failure_count = len(results) - success_count

    return {
        "success": failure_count == 0,
        "error_code": ErrorCode.NONE if failure_count == 0 else ErrorCode.COMMAND_FAILED,
        "results": results,
        "total": len(results),
        "success_count": success_count,
        "failure_count": failure_count,
    }


SPECIAL_COMMANDS = {
    "list": cmd_list,
    "info": cmd_info,
    "batch": cmd_batch,
}


def _parse_global_args(args: list[str]) -> tuple[list[str], list[str]]:
    """Separate global options from command args.

    Returns (global_args, remaining_args).
    """
    global_opts = []
    remaining = list(args)
    i = 0
    while i < len(remaining):
        if remaining[i] == "--pretty":
            global_opts.append("--pretty")
            remaining.pop(i)
        elif remaining[i] == "--verbose":
            global_opts.append("--verbose")
            remaining.pop(i)
        else:
            i += 1
    return global_opts, remaining


def main():
    global PRETTY, VERBOSE

    raw_args = sys.argv[1:]

    # Parse global options
    global_opts, args = _parse_global_args(raw_args)
    PRETTY = "--pretty" in global_opts
    VERBOSE = "--verbose" in global_opts

    if not args or args[0] in ("-h", "--help", "help"):
        result = {
            "success": True,
            "name": "d2j-ai",
            "description": "AI-friendly CLI for dex2jar reverse engineering tools",
            "usage": "d2j-ai [--pretty] [--verbose] <command> [args...]",
            "global_options": {
                "--pretty": "Pretty-print JSON output",
                "--verbose": "Show verbose output including stderr",
            },
            "commands": {k: fn.__doc__ for k, fn in COMMANDS.items()},
            "special_commands": {k: fn.__doc__ for k, fn in SPECIAL_COMMANDS.items()},
        }
        print(json.dumps(result, indent=2 if PRETTY else None))
        sys.exit(0)

    command = args[0]
    cmd_args = args[1:]

    if command in SPECIAL_COMMANDS:
        result = SPECIAL_COMMANDS[command](cmd_args)
    elif command in COMMANDS:
        result = COMMANDS[command](cmd_args)
    else:
        result = {
            "success": False,
            "error_code": ErrorCode.UNKNOWN_COMMAND,
            "error": f"Unknown command: {command}",
            "available_commands": list(COMMANDS.keys()) + list(SPECIAL_COMMANDS.keys()),
        }

    indent = 2 if PRETTY else None
    print(json.dumps(result, indent=indent, default=str))

    # Verbose: print stderr to real stderr if present
    if VERBOSE and isinstance(result, dict) and result.get("stderr"):
        print(f"[stderr] {result['stderr']}", file=sys.stderr)

    sys.exit(0 if result.get("success") else 1)


if __name__ == "__main__":
    main()

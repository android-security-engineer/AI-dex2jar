#!/usr/bin/env python3
"""dex2jar-skills: AI-friendly CLI wrapper for dex2jar reverse engineering tools.

Provides structured JSON output for all dex2jar commands,
making it easy for AI agents to parse and use the results.
"""

import json
import os
import shutil
import subprocess
import sys
from pathlib import Path
from typing import Any, Optional


def find_d2j_home() -> Path:
    """Locate dex2jar installation directory."""
    # 1. Explicit env var
    if env_home := os.environ.get("D2J_HOME"):
        p = Path(env_home)
        if p.is_dir():
            return p
    # 2. Look relative to this script (sibling of cli/)
    script_dir = Path(__file__).resolve().parent
    repo_root = script_dir.parent
    candidate = repo_root / "dex-tools" / "build" / "distributions"
    if candidate.is_dir():
        zips = sorted(candidate.glob("dex-tools-*.zip"))
        if zips:
            return zips[-1]  # latest build
    # 3. Check if d2j-dex2jar.sh is on PATH
    if shutil.which("d2j-dex2jar.sh"):
        return Path(shutil.which("d2j-dex2jar.sh")).parent
    raise RuntimeError(
        "Cannot find dex2jar. Set D2J_HOME or build with: ./gradlew distZip"
    )


def find_java() -> str:
    """Locate Java runtime."""
    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        return str(Path(java_home) / "bin" / "java")
    java_bin = shutil.which("java")
    if java_bin:
        return java_bin
    raise RuntimeError("Cannot find Java. Install JDK 8+ or set JAVA_HOME.")


def run_d2j_command(cmd_name: str, args: list[str]) -> dict[str, Any]:
    """Execute a d2j-* command and return structured result."""
    d2j_home = find_d2j_home()
    java_bin = find_java()

    # If d2j_home is a zip, we need the extracted dir
    if d2j_home.suffix == ".zip":
        import tempfile
        extract_dir = Path(tempfile.mkdtemp(prefix="d2j-"))
        shutil.unpack_archive(str(d2j_home), extract_dir)
        # Find the extracted directory
        extracted = list(extract_dir.glob("dex-tools-*"))
        if not extracted:
            return {"success": False, "error": f"Cannot extract {d2j_home}"}
        bin_dir = extracted[0] / "bin"
    else:
        bin_dir = d2j_home / "bin"

    script = bin_dir / f"{cmd_name}.sh"
    if not script.exists():
        return {
            "success": False,
            "error": f"Command {cmd_name} not found at {script}",
            "available_commands": list_available_commands(bin_dir),
        }

    full_cmd = [str(script)] + args
    try:
        result = subprocess.run(
            full_cmd,
            capture_output=True,
            text=True,
            timeout=300,
        )
        return {
            "success": result.returncode == 0,
            "returncode": result.returncode,
            "stdout": result.stdout,
            "stderr": result.stderr,
            "command": " ".join(full_cmd),
        }
    except subprocess.TimeoutExpired:
        return {"success": False, "error": f"Command timed out after 300s"}
    except Exception as e:
        return {"success": False, "error": str(e)}


def list_available_commands(bin_dir: Optional[Path] = None) -> list[str]:
    """List all available d2j commands."""
    if bin_dir is None:
        try:
            d2j_home = find_d2j_home()
            bin_dir = d2j_home / "bin"
        except RuntimeError:
            return []
    commands = []
    if bin_dir.is_dir():
        for f in sorted(bin_dir.glob("d2j-*.sh")):
            commands.append(f.stem)
    return commands


def cmd_dex2jar(args: list[str]) -> dict[str, Any]:
    """Convert .dex/.apk to .jar"""
    if not args:
        return {
            "success": False,
            "error": "Usage: d2j-dex2jar <file.apk|file.dex> [-o output.jar] [-f]",
            "description": "Convert dex to jar",
            "options": {
                "-o, --output": "Output .jar file path",
                "-f, --force": "Force overwrite existing output",
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
            "error": "Usage: d2j-jar2dex <dir|jar>",
            "description": "Convert jar to dex by invoking dx",
            "options": {
                "-o, --output": "Output .dex file path",
                "-f, --force": "Force overwrite",
            },
        }
    return run_d2j_command("d2j-jar2dex", args)


def cmd_baksmali(args: list[str]) -> dict[str, Any]:
    """Disassemble .dex to smali files"""
    if not args:
        return {
            "success": False,
            "error": "Usage: d2j-baksmali <dex>",
            "description": "Disassemble and/or dump a dex file to smali",
        }
    return run_d2j_command("d2j-baksmali", args)


def cmd_smali(args: list[str]) -> dict[str, Any]:
    """Assemble smali files into .dex"""
    if not args:
        return {
            "success": False,
            "error": "Usage: d2j-smali [<smali-file>|folder]*",
            "description": "Assemble smali files into a dex file",
        }
    return run_d2j_command("d2j-smali", args)


def cmd_apk_sign(args: list[str]) -> dict[str, Any]:
    """Sign an APK with test certificate"""
    if not args:
        return {
            "success": False,
            "error": "Usage: d2j-apk-sign <apk>",
            "description": "Sign an android apk file with a test certificate",
            "options": {
                "-o, --output": "Output .apk file",
                "-f, --force": "Force overwrite",
                "-t, --tiny": "Use tiny sign",
            },
        }
    return run_d2j_command("d2j-apk-sign", args)


def cmd_jar_access(args: list[str]) -> dict[str, Any]:
    """Modify access flags in .jar"""
    if not args:
        return {
            "success": False,
            "error": "Usage: d2j-jar-access <jar>",
            "description": "Add or remove class/method/field access in jar file",
            "options": {
                "-af, --add-field-access": "Add access from field (e.g. public)",
                "-am, --add-method-access": "Add access from method",
                "-ac, --add-class-access": "Add access from class",
                "-rf, --remove-field-access": "Remove access from field",
                "-rm, --remove-method-access": "Remove access from method",
                "-rc, --remove-class-access": "Remove access from class",
                "-rd, --remove-debug": "Remove debug info",
            },
        }
    return run_d2j_command("d2j-jar-access", args)


def cmd_asm_verify(args: list[str]) -> dict[str, Any]:
    """Verify .class files in jar"""
    if not args:
        return {
            "success": False,
            "error": "Usage: d2j-asm-verify <jar0> [jar1 ...]",
            "description": "Verify .class in jar",
        }
    return run_d2j_command("d2j-asm-verify", args)


def cmd_jar2jasmin(args: list[str]) -> dict[str, Any]:
    """Disassemble .class to jasmin format"""
    if not args:
        return {
            "success": False,
            "error": "Usage: d2j-jar2jasmin <jar>",
            "description": "Disassemble .class in jar to jasmin file",
        }
    return run_d2j_command("d2j-jar2jasmin", args)


def cmd_jasmin2jar(args: list[str]) -> dict[str, Any]:
    """Assemble jasmin files to .jar"""
    if not args:
        return {
            "success": False,
            "error": "Usage: d2j-jasmin2jar <dir>",
            "description": "Assemble jasmin files to jar",
        }
    return run_d2j_command("d2j-jasmin2jar", args)


def cmd_decrypt_string(args: list[str]) -> dict[str, Any]:
    """Decrypt strings in .class files"""
    if not args:
        return {
            "success": False,
            "error": "Usage: d2j-decrypt-string <jar>",
            "description": "Decrypt string in class file",
        }
    return run_d2j_command("d2j-decrypt-string", args)


def cmd_std_apk(args: list[str]) -> dict[str, Any]:
    """Clean up APK to standard zip"""
    if not args:
        return {
            "success": False,
            "error": "Usage: d2j-std-zip <zip>",
            "description": "Clean up apk to standard zip",
        }
    return run_d2j_command("d2j-std-apk", args)


def cmd_dex_recompute_checksum(args: list[str]) -> dict[str, Any]:
    """Recompute CRC and SHA1 of .dex"""
    if not args:
        return {
            "success": False,
            "error": "Usage: d2j-dex-recompute-checksum <dex>",
            "description": "Recompute crc and sha1 of dex",
        }
    return run_d2j_command("d2j-dex-recompute-checksum", args)


def cmd_dex_weaver(args: list[str]) -> dict[str, Any]:
    """Replace invoke in .dex"""
    if not args:
        return {
            "success": False,
            "error": "Usage: d2j-dex-weaver <dex>",
            "description": "Replace invoke in dex",
        }
    return run_d2j_command("d2j-dex-weaver", args)


def cmd_jar_weaver(args: list[str]) -> dict[str, Any]:
    """Replace invoke in .jar"""
    if not args:
        return {
            "success": False,
            "error": "Usage: d2j-jar-weaver <jar>",
            "description": "Replace invoke in jar",
        }
    return run_d2j_command("d2j-jar-weaver", args)


def cmd_class_version_switch(args: list[str]) -> dict[str, Any]:
    """Switch .class file version"""
    if not args:
        return {
            "success": False,
            "error": "Usage: d2j-class-version-switch <version> <old.jar> <new.jar>",
            "description": "Switch class file version in jar",
        }
    return run_d2j_command("d2j-class-version-switch", args)


def cmd_dex2smali(args: list[str]) -> dict[str, Any]:
    """Disassemble .dex to smali (alias for baksmali)"""
    if not args:
        return {
            "success": False,
            "error": "Usage: d2j-dex2smali <dex-file>",
            "description": "Disassemble dex to smali files",
        }
    return run_d2j_command("d2j-dex2smali", args)


def cmd_mt_dex2jar(args: list[str]) -> dict[str, Any]:
    """Convert .dex/.apk to .jar using multiple threads"""
    if not args:
        return {
            "success": False,
            "error": "Usage: d2j-mt-dex2jar <file0> [file1 ...] [-o output.jar]",
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
            "error": "Usage: d2j-init-deobf <jar-file>",
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
            "error": "Usage: d2j-generate-stub-from-odex <odex0> [odex1 ...]",
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
            "error": "Usage: d2j-extract-odex-from-coredump <core.xxxx>",
            "description": "Extract odex from dalvik memory core dump",
        }
    return run_d2j_command("d2j-extract-odex-from-coredump", args)


def cmd_dex_asmifier(args: list[str]) -> dict[str, Any]:
    """Generate ASMifier output from .dex files"""
    if not args:
        return {
            "success": False,
            "error": "Usage: d2j-dex-asmifier <dex0> [dex1 ...]",
            "description": "Generate ASMifier Java source from dex files",
        }
    return run_d2j_command("d2j-dex-asmifier", args)


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
}


def cmd_list(_args: list[str]) -> dict[str, Any]:
    """List all available commands"""
    commands = list_available_commands()
    return {
        "success": True,
        "available_commands": list(COMMANDS.keys()),
        "installed_commands": commands,
        "descriptions": {k: fn.__doc__ for k, fn in COMMANDS.items()},
    }


def main():
    if len(sys.argv) < 2 or sys.argv[1] in ("-h", "--help", "help"):
        print(json.dumps({
            "success": True,
            "name": "ai-dex2jar",
            "description": "AI-friendly CLI for dex2jar reverse engineering tools",
            "usage": "ai-dex2jar <command> [args...]",
            "commands": {k: fn.__doc__ for k, fn in COMMANDS.items()},
            "special_commands": {
                "list": "List all available commands",
                "help": "Show this help",
            },
        }, indent=2))
        sys.exit(0)

    command = sys.argv[1]
    args = sys.argv[2:]

    if command == "list":
        result = cmd_list(args)
    elif command in COMMANDS:
        result = COMMANDS[command](args)
    else:
        result = {
            "success": False,
            "error": f"Unknown command: {command}",
            "available_commands": list(COMMANDS.keys()),
        }

    print(json.dumps(result, indent=2))
    sys.exit(0 if result.get("success") else 1)


if __name__ == "__main__":
    main()

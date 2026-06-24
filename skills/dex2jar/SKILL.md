---
name: dex2jar
description: "Android reverse-engineering workbench built on dex2jar. Use whenever the user works with an APK, DEX, ODEX, JAR, smali, or any Android binary — converting DEX/APK to JAR, disassembling to smali, decompiling, signing/resigning APKs, or analyzing app structure, strings, call graphs, dependencies, manifests, signing certificates, or native .so/JNI code. Triggers include: reverse engineer this APK, what does this app do, convert dex to jar, decompile, dex2jar, baksmali, smali, is this apk malicious, what permissions does it request, find the crypto/network/reflection calls, who calls this method, read the signing cert, what native libraries does it ship, JNI entry points, 逆向, 反编译, 把apk转成jar, 看看这个安卓应用做了什么, 这个apk有没有问题. Use proactively for any Android app analysis or triage even if dex2jar is not named explicitly."
license: Apache-2.0
---

# dex2jar — Android Reverse-Engineering Workbench

A workflow skill for analyzing Android applications with the dex2jar toolchain. It
turns "I have an APK/DEX and need to understand it" into a concrete, repeatable
sequence backed by a 29-command CLI that emits structured JSON.

## The backend: `d2j-ai.py`

Every capability is exposed through one script at the repo root. Always invoke it
through Python and pass `--pretty` when you (the model) need to read the result:

```bash
python3 d2j-ai.py <command> <file> [options] --pretty
```

It returns JSON with `success`, `error_code`, and command-specific fields, so you
can branch on the result without scraping text. Run `python3 d2j-ai.py list` to see
all commands, or `python3 d2j-ai.py info <command>` for a command's options. The
full command catalog (grouped by purpose) is in
[`references/command-reference.md`](references/command-reference.md) — read it when
you need a command you don't already know.

## How to approach a task

Pick the entry point by what the user actually has and wants:

1. **An unknown or suspicious APK, "what is this / is it safe / what's the attack
   surface?"** → start with triage. See
   [`references/workflow-triage.md`](references/workflow-triage.md). In short:
   `manifest-inspect -e` (exported attack surface) → `apk-cert` (who signed it) →
   `native-libs -s` (native entry points) → `dex-strings -u` (indicators). This is
   also packaged as the focused `apk-triage` skill.

2. **"Convert this to readable Java / decompile it."** → `dex2jar` produces a JAR
   you can open in any Java decompiler. Conversion is *bytecode translation, not
   decompilation*, and it can legitimately fail on obfuscated or hand-crafted DEX —
   read [`references/dex-to-jar-accuracy.md`](references/dex-to-jar-accuracy.md)
   before promising a clean result, and verify with `asm-verify`.

3. **"Find where X happens / find the crypto/network/dynamic-loading calls / who
   calls this method?"** → reverse cross-reference. `dex-xref --preset crypto`
   (also `reflection`, `dynload`, `net`) or `dex-xref --to <substring>`, then
   `dex-method-trace --target <method>` to walk callers. This is packaged as the
   focused `sensitive-api-audit` skill.

4. **Structure-level questions ("what classes/methods are in here?", "what does
   this class depend on?")** → `dex-inspect`, `dex-class-deps`, `dex-method-trace`.

5. **Patching / rebuilding (baksmali → edit → smali → sign).** → `baksmali` to
   disassemble, edit smali, `smali` to reassemble, `apk-sign` to resign with a test
   cert. `dex-weaver`/`jar-weaver` replace invokes in place.

When a request spans several of these, do triage first — it's cheap and tells you
where to dig.

## Backends (slash command vs CLI vs MCP)

The same 29 capabilities are reachable three ways; pick by context. Details in
[`references/backends.md`](references/backends.md):

- **Slash commands** (`commands/*.md`) — interactive Claude Code use.
- **`d2j-ai.py`** — the canonical scripted backend; what this skill drives.
- **MCP server** (`dex2jar-mcp/server.py`) — tool-per-command for Claude
  Desktop/Code over MCP.

## Operating notes

- **Input safety:** the CLI validates file type and existence and returns an
  `error_code` (`file_not_found`, `invalid_format`, …) rather than throwing — check
  it before continuing a workflow.
- **Build requirement:** the structured-analysis commands (`dex-inspect`,
  `dex-xref`, `manifest-inspect`, `apk-cert`, `native-libs`, …) run through the
  `dex2jar-ai-cli` Java module. If it isn't built, the CLI says so; build with
  `./gradlew :dex2jar-ai-cli:build` (Java 8+). The self-contained parsers
  (AXML, ELF, PKCS#7) need no external tools — no aapt, NDK, or binutils.
- **Self-contained parsing:** manifest, native-lib, and certificate inspection are
  implemented in-tree, so they work offline and on truncated/odd files.

## Scope

This is authorized reverse-engineering / security-analysis tooling (CTF,
pentesting, malware triage, app-security review). It reads and transforms Android
binaries the user already possesses; it does not exfiltrate data or attack remote
systems.

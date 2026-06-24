# Roadmap — closing the Android-RE capability gaps

This roadmap tracks the work to evolve the CLI surface from "a wrapper around dex2jar's
bytecode tools" into a more complete **AI-driven Android reverse-engineering workbench**.
It is derived from an audit of the exposed CLI (`d2j-ai.py` + `dex2jar-ai-cli`) against
what real RE workflows need.

Status legend: ✅ done · 🚧 in progress · ⬜ planned

## Phase 0 — Audit findings (baseline)

The current CLI exposes **28 entry points** (25 functional + `list`/`info`/`batch`) through two
backends: classic `bin/d2j-*.sh` scripts and the `dex2jar-ai-cli` Java module (structured JSON).
Coverage of dex2jar's *own* tools is complete, but Android-RE coverage has gaps.

Known issues:
- **CLI drift**: `CommandRegistry.java` registers 20 commands; `d2j-ai.py` exposes 25. The Java
  CLI is missing `mt-dex2jar`, `dex2smali`, `dex-asmifier`, `generate-stub-from-odex`,
  `extract-odex-from-coredump`.
- **Duplicated JSON assembly**: each analysis command hand-rolls its own `jsonStr` escaper.
- **Capability gaps** (need new commands): AndroidManifest/resources, reverse xref / sensitive-API
  detection, signing-certificate inspection, native `.so`/JNI.

## Phase 1 — Consistency & foundation

- ✅ **Fix CommandRegistry drift** — registered the 5 missing commands (`mt-dex2jar`,
  `dex2smali`, `dex-asmifier`, `generate-stub-from-odex`, `extract-odex-from-coredump`);
  both CLIs now expose the same 25 commands. *(task #1)*
- ✅ **Shared `Json` builder** — added `ai/Json.java` (fluent object/array builder + escaper);
  migrated all four existing analysis commands off their private `jsonStr` (now delegate to
  `Json.str`), and `dex-xref` is built on it. *(task #3)*

## Phase 2 — Find problems, not just structure

- ✅ **`dex-xref`** — reverse cross-reference. Given a `--to` pattern or a preset
  (`crypto` / `reflection` / `dynload` / `net`), reports every invoke/field/type/string usage
  site with its enclosing method and opcode. Turns "see structure" into "find the sink".
  Verified against the test dex (crypto/reflection/net indicators all detected). *(task #2)*

## Phase 3 — APK-level visibility

- ✅ **`manifest-inspect`** — self-contained binary `AndroidManifest.xml` (AXML) parser
  (`ai/AxmlParser.java` + `AndroidAttrs.java` + `ManifestInspectCmd.java`): package, version,
  min/target/compile SDK, `debuggable`/`allowBackup`/cleartext flags, permissions, and every
  component with its exported status (explicit vs. implicit-via-intent-filter), guarding
  permission, and intent actions. `-e` filters to the exported attack surface. Verified against
  InsecureBankv2, UnCrackable-L2, HelloWord-JNI, r2pay, Xamarin APKs. *(task #4)*

## Phase 4 — Forensics & native

- ✅ **`apk-cert`** — reads v1 signing certificates via the JDK's `CertificateFactory`:
  subject/issuer/serial/validity, key type/size, and SHA-256/SHA-1/MD5 fingerprints
  (same-author identification). Verified against InsecureBankv2, UnCrackable-L2, r2pay. *(task #5)*
- ✅ **`native-libs`** — enumerates `lib/<abi>/*.so`, with a self-contained ELF reader
  (`ai/ElfSymbols.java`, ELF32+ELF64 / LE+BE) that extracts ABI, machine, and exported
  `JNI_OnLoad` / `Java_*` symbols from `.dynsym`. Verified against HelloWord-JNI and
  UnCrackable-L2 (recovered the exact native check entry points). *(task #6)*

## Status: all phases complete

The CLI now exposes **29 commands** (both backends in perfect parity), adding five
Android-RE capabilities on top of the original dex2jar bytecode tools:
`dex-xref`, `manifest-inspect`, `apk-cert`, `native-libs` (plus the registry-drift fix and
the shared `Json` builder). Each is wired across all five layers and verified offline.

## Wiring checklist (applies to every new command)

1. Java command class in `dex2jar-ai-cli/.../ai/` (extends `BaseCmd`, emits JSON).
2. Register in `CommandRegistry.java`.
3. Add handler + dispatch entry in `d2j-ai.py`.
4. Add `commands/<name>.md` slash command.
5. Add row to `README.skills.md` (and the README table if user-facing).
6. Verify with the offline javac harness (gradle is broken offline — see project memory).

## Build/verify note

The gradle build does not work offline. Analysis commands are compiled and run in isolation
with a focused `javac` classpath (JDK 11 + cached ASM/picocli/dx). See the project memory
`offline-build-verify-harness` for the exact commands.

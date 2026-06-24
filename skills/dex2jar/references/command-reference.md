# Command Reference

All 29 commands, grouped by purpose. Invoke any of them as
`python3 d2j-ai.py <command> <file> [options] --pretty`. Run
`python3 d2j-ai.py info <command>` for the exact flags of one command.

## Conversion

| Command | Purpose |
|---------|---------|
| `dex2jar` | Convert `.dex`/`.apk` → `.jar` (bytecode translation) |
| `mt-dex2jar` | Same, multi-threaded (large multi-dex APKs) |
| `jar2dex` | Convert `.jar` → `.dex` |
| `baksmali` / `dex2smali` | Disassemble `.dex` → smali |
| `smali` | Assemble smali → `.dex` |
| `jar2jasmin` | Disassemble `.class` → jasmin |
| `jasmin2jar` | Assemble jasmin → `.jar` |
| `std-apk` | Clean an APK into a standard zip |

## Structure analysis (Java `dex2jar-ai-cli` backend, JSON)

| Command | Purpose |
|---------|---------|
| `dex-inspect` | List classes / methods / fields; `-f` filter, `-d` detail, `-s` strings |
| `dex-strings` | Extract const-string literals; `-f`/`-c` filter, `-u` unique |
| `dex-method-trace` | Caller→callee graph; `-t` find callers of a method |
| `dex-class-deps` | Per-class type dependencies; `-d` deep (method bodies), `-i` internal-only |
| `dex-xref` | Reverse xref; `--to <substr>` or `--preset crypto\|reflection\|dynload\|net`, `-k` kinds |

## APK forensics (self-contained parsers, JSON)

| Command | Purpose |
|---------|---------|
| `manifest-inspect` | Parse binary `AndroidManifest.xml`; `-e` exported-only attack surface |
| `apk-cert` | v1 signing certs: subject/issuer/validity + SHA-256/SHA-1/MD5 fingerprints |
| `native-libs` | Enumerate `lib/<abi>/*.so`; `-s` list exported JNI symbols |

## Signing / patching / transformation

| Command | Purpose |
|---------|---------|
| `apk-sign` | Sign an APK with a test certificate |
| `jar-access` | Modify access flags in a `.jar` |
| `asm-verify` | Verify `.class` files in a jar (validate conversion output) |
| `decrypt-string` | Decrypt encrypted strings in `.class` files |
| `init-deobf` | Generate a deobfuscation config |
| `dex-weaver` | Replace invoke targets in a `.dex` |
| `jar-weaver` | Replace invoke targets in a `.jar` |
| `class-version-switch` | Switch `.class` file version |
| `dex-recompute-checksum` | Recompute CRC + SHA-1 of a `.dex` |
| `dex-asmifier` | Generate ASMifier Java source from a `.dex` |

## ODEX / forensics

| Command | Purpose |
|---------|---------|
| `generate-stub-from-odex` | Generate compile stubs from `.odex` |
| `extract-odex-from-coredump` | Recover `.odex` from a Dalvik memory core dump |

## Meta

| Command | Purpose |
|---------|---------|
| `list` | List all commands |
| `info <command>` | Show a command's options |
| `batch <json>` | Run a sequence of commands from a JSON file |

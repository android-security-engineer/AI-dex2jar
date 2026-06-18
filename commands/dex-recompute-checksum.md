---
description: Recompute CRC and SHA1 checksums of DEX files
allowed-tools: Bash(python3:*)
argument-hint: <dex-file>
---

# dex-recompute-checksum - Recompute DEX Checksums

Recompute CRC32 and SHA1 checksums in a DEX file header.

## Usage

```bash
python3 d2j-ai.py dex-recompute-checksum $ARGUMENTS
```

## What it does

Recalculates and updates the CRC32 and SHA1 checksum fields in a .dex file header. This is necessary after manually editing a DEX file, as the checksums will no longer match the file contents.

## Output

Returns JSON with:
- `success` — true/false
- `stdout` — recomputation output log
- `stderr` — any errors or warnings

## Example

```bash
python3 d2j-ai.py dex-recompute-checksum modified_classes.dex
```

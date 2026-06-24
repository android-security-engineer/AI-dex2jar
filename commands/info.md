---
description: Show detailed info about dex2jar commands including options and usage
allowed-tools: Bash(python3:*), Bash(chmod:*)
argument-hint: [command-name]
---

# info - Command Details & Options

Show detailed information about dex2jar commands including available options and usage.

## Usage

```bash
# Show all commands with details
python3 d2j-ai.py info

# Show specific command details
python3 d2j-ai.py info $ARGUMENTS
```

## What it does

When called without arguments, lists all 21 commands with their descriptions and options.
When called with a command name, shows detailed info for that specific command.

## Output

Returns JSON with:
- `success` — true/false
- `name` — command name (when specific)
- `description` — what the command does
- `options` — available flags and arguments
- `usage` — usage string

## Examples

```bash
# All commands info
python3 d2j-ai.py info

# Specific command
python3 d2j-ai.py info dex2jar
python3 d2j-ai.py info baksmali
python3 d2j-ai.py info apk-sign
```

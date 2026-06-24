# Backends: slash command vs CLI vs MCP

The same 29 capabilities are reachable three ways. They all ultimately drive the
same dex2jar core; pick by where you're running.

## 1. `d2j-ai.py` — the canonical scripted backend

```bash
python3 d2j-ai.py <command> <file> [options] --pretty
```

Structured JSON out (`success`, `error_code`, command fields), input validation,
and a `batch` mode that runs a JSON list of commands. This is what the skills
drive, and the most reliable target for programmatic use. Global flags:
`--pretty` (readable JSON), `--verbose` (include stderr).

Two internal paths sit behind it:
- `run_d2j_command()` invokes the classic `bin/d2j-*.sh` scripts (conversion,
  signing, smali).
- `run_java_cli()` invokes the `dex2jar-ai-cli` Java module for the structured
  analysis commands; it degrades gracefully (returns a build-required message)
  when the module isn't compiled.

## 2. Slash commands — interactive Claude Code

`commands/<name>.md` defines a slash command per capability (e.g. `/dex2jar`,
`/manifest-inspect`, `/dex-xref`). Each wraps the same `d2j-ai.py` call with an
argument hint and docs. Best for hands-on interactive sessions.

## 3. MCP server — Claude Desktop / Code over MCP

`dex2jar-mcp/server.py` exposes one MCP tool per command (`dex2jar`,
`manifest_inspect`, `apk_cert`, `native_libs`, …) plus `run_d2j_command` as a
generic fallback, and resources (`d2j://commands`, `d2j://version`) and prompts
(`analyze_apk`, …). Install with `pip install mcp` and point the MCP client at
`server.py` (`D2J_AI_PATH` env var locates `d2j-ai.py`). See
`dex2jar-mcp/README.md`.

## Which to use

| Context | Use |
|---------|-----|
| This skill / scripting / automation | `d2j-ai.py` |
| Interactive Claude Code session | slash commands |
| Claude Desktop or MCP-based client | MCP server |

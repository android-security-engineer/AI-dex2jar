# Evals

Tests for the dex2jar Skills. Two layers, because the skills trigger differently
from a hook-based plugin: our skills load via the **model reading each
`SKILL.md` description**, not a regex hook — so a fast structural check is
runnable offline, while trigger accuracy needs the model.

## 1. Structural validation (offline, CI-safe) — `validate-skills.sh`

```bash
bash evals/validate-skills.sh
```

Checks, without calling the model:

- all JSON manifests parse (`plugin.json`, `.claude-plugin/*.json`, `evals.json`);
- `plugin.json` `commands`/`agents`/`skills` pointers exist;
- root and `.claude-plugin/plugin.json` agree on name+version;
- every `skills/*/SKILL.md` has `name`+`description` frontmatter, a body under the
  500-line budget, and a `name` matching its directory;
- every `(…​.md)` link in a SKILL.md resolves;
- every command referenced as `d2j-ai.py <cmd>` in the skills is a real CLI command;
- the trigger-prompt fixtures exist and are tagged.

Exit code is the failure count, so it drops into CI as-is.

## 2. Trigger accuracy (needs the model) — `trigger-prompts/`

- `should-trigger.txt` — natural prompts that should load a skill, each tagged with
  the expected skill (`[dex2jar]`, `[apk-triage]`, `[sensitive-api-audit]`).
- `should-not-trigger.txt` — adjacent/unrelated prompts that should **not** load any
  dex2jar skill (general coding, non-Android security, iOS, incidental "jar").

Run a prompt against Claude with the plugin loaded and confirm the right skill
activates (and the negatives stay quiet). Use these when tuning a `description`
field — if a skill under- or over-triggers, the description is the lever.

## 3. Behavior cases — `evals.json`

Realistic per-skill task prompts with `expected_output`, following the
skill-creator schema. Use them to spot-check that, once a skill loads, it drives
the right commands and produces the right shape of answer.

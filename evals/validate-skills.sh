#!/usr/bin/env bash
# Offline structural validator for the dex2jar Skills repo.
# Does NOT call the Claude model — checks that the repo is a well-formed
# Skills/plugin repository. Safe to run in CI.
#
# Usage: bash evals/validate-skills.sh
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT"

PASS=0
FAIL=0
ok()   { printf '  \xe2\x9c\x85 %s\n' "$1"; PASS=$((PASS+1)); }
bad()  { printf '  \xe2\x9d\x8c %s\n' "$1"; FAIL=$((FAIL+1)); }

echo "== JSON manifests parse =="
for f in plugin.json .claude-plugin/plugin.json .claude-plugin/marketplace.json evals/evals.json; do
  if python3 -c "import json,sys; json.load(open(sys.argv[1]))" "$f" 2>/dev/null; then
    ok "$f"
  else
    bad "$f does not parse"
  fi
done

echo "== plugin.json path pointers exist =="
for d in commands agents skills; do
  [ -d "$ROOT/$d" ] && ok "./$d/" || bad "./$d/ referenced by plugin.json but missing"
done

echo "== root and .claude-plugin plugin.json agree on name+version =="
python3 - <<'PY' && ok "name+version in sync" || bad "name/version mismatch between manifests"
import json
a=json.load(open('plugin.json')); b=json.load(open('.claude-plugin/plugin.json'))
import sys; sys.exit(0 if (a.get('name')==b.get('name') and a.get('version')==b.get('version')) else 1)
PY

echo "== every SKILL.md has valid frontmatter (name+description) and body < 500 lines =="
python3 - <<'PY'
import glob, re, sys
rc=0
for p in sorted(glob.glob('skills/*/SKILL.md')):
    t=open(p).read()
    m=re.match(r'^---\n(.*?)\n---\n', t, re.S)
    if not m:
        print(f"  ❌ {p}: no YAML frontmatter"); rc=1; continue
    fm=m.group(1)
    name=re.search(r'^name:\s*\S', fm, re.M)
    desc=re.search(r'^description:\s*\S', fm, re.M)
    body=t[m.end():].count('\n')
    bad=[]
    if not name: bad.append("missing name")
    if not desc: bad.append("missing description")
    if body>500: bad.append(f"body {body}>500 lines")
    # dir name should match frontmatter name
    dirname=p.split('/')[1]
    nm=re.search(r'^name:\s*(\S+)', fm, re.M)
    if nm and nm.group(1).strip().strip('"\'')!=dirname:
        bad.append(f"name '{nm.group(1)}' != dir '{dirname}'")
    if bad:
        print(f"  ❌ {p}: {'; '.join(bad)}"); rc=1
    else:
        print(f"  ✅ {p}")
sys.exit(rc)
PY
[ $? -eq 0 ] && PASS=$((PASS+1)) || FAIL=$((FAIL+1))

echo "== referenced files in SKILL.md bodies exist =="
python3 - <<'PY'
import glob, re, os, sys
rc=0
for p in sorted(glob.glob('skills/*/SKILL.md')):
    base=os.path.dirname(p)
    for ref in re.findall(r'\(([^)]+\.md)\)', open(p).read()):
        if ref.startswith('http'): continue
        target=os.path.normpath(os.path.join(base, ref))
        if not os.path.exists(target):
            print(f"  ❌ {p} -> {ref} (missing)"); rc=1
if rc==0: print("  ✅ all referenced .md targets exist")
sys.exit(rc)
PY
[ $? -eq 0 ] && PASS=$((PASS+1)) || FAIL=$((FAIL+1))

echo "== every command referenced in skills/ exists in d2j-ai.py =="
python3 - <<'PY'
import re, glob, sys
s=open('d2j-ai.py').read()
m=re.search(r'COMMANDS\s*=\s*\{(.*?)\n\}', s, re.S)
real=set(re.findall(r'"([a-z0-9-]+)"\s*:', m.group(1))) | {'list','info','batch'}
refd=set()
for p in glob.glob('skills/**/*.md', recursive=True):
    refd|=set(re.findall(r'd2j-ai\.py\s+([a-z0-9-]+)', open(p).read()))
missing=sorted(refd-real)
if missing:
    print(f"  ❌ unknown commands referenced: {missing}"); sys.exit(1)
print(f"  ✅ all {len(refd)} referenced commands exist")
PY
[ $? -eq 0 ] && PASS=$((PASS+1)) || FAIL=$((FAIL+1))

echo "== trigger-prompt fixtures present and tagged =="
python3 - <<'PY'
import re, sys, os
rc=0
st='evals/trigger-prompts/should-trigger.txt'
snt='evals/trigger-prompts/should-not-trigger.txt'
for f in (st, snt):
    if not os.path.exists(f):
        print(f"  ❌ {f} missing"); rc=1
if os.path.exists(st):
    skills={'dex2jar','apk-triage','sensitive-api-audit'}
    lines=[l for l in open(st) if l.strip() and not l.startswith('#')]
    bad=[l for l in lines if not re.match(r'\[(dex2jar|apk-triage|sensitive-api-audit)\]', l.strip())]
    if bad:
        print(f"  ❌ {len(bad)} should-trigger lines missing a valid [skill] tag"); rc=1
    else:
        print(f"  ✅ {len(lines)} tagged should-trigger prompts")
sys.exit(rc)
PY
[ $? -eq 0 ] && PASS=$((PASS+1)) || FAIL=$((FAIL+1))

echo
echo "== summary: $PASS passed, $FAIL failed =="
[ "$FAIL" -eq 0 ] && printf '\xe2\x9c\x85 repo is a well-formed Skills/plugin repository\n' || printf '\xe2\x9d\x8c fix the above\n'
exit $FAIL

# 贡献指南 / Contributing

感谢你为 **dex2jar-skills** 贡献！本仓库既是一个 Claude Code 插件，也是一个
Skills 仓库——核心是 `skills/` 编排层、29 条命令的 `d2j-ai.py` JSON CLI、底层
`dex2jar-ai-cli`(Java) 与 `dex2jar-mcp`(MCP server)。先读 [docs/architecture.md](../docs/architecture.md)
了解分层。

Thanks for contributing! This repo is both a Claude Code plugin and a Skills
repository. Read [docs/architecture.md](../docs/architecture.md) first.

---

## Issue 规范 / Issues

每个 Issue 请用模板并带正确前缀 / Use a template with the right prefix:

| 类型 / Type | 前缀 / Prefix | 用途 / For |
|------|--------|------|
| Bug | `bug:` | 命令报错、skill 不触发、转换/分析结果不对 |
| Feature | `feat:` | 新命令、新 skill、新 reference |
| Question | `question:` | 使用疑问（先看 [docs/FAQ.md](../docs/FAQ.md)） |

恶意样本请勿直接上传到 Issue；脱敏或给出最小可复现片段。
Do not upload malware samples to issues; sanitize or give a minimal repro.

## PR 规范 / Pull requests

提交 PR 前，**本地必须通过结构校验** / Before opening a PR, the structural
validator must pass locally:

```bash
bash evals/validate-skills.sh   # 期望 / expect: 12 passed, 0 failed
```

CI 会在 push/PR 时自动跑同一脚本（`.github/workflows/skills-validate.yml`）。

### 改 Skills 时 / When touching `skills/`

- 每个 skill 是 `skills/<name>/SKILL.md`，frontmatter 的 `name` **必须等于目录名**，
  且 body **少于 500 行**（超长内容拆到 `references/`）。
- `description` 要 trigger-rich、双语（EN + 中文触发词），便于模型按描述路由。
- **Skills 只编排现有 `d2j-ai.py` 命令，不复制命令逻辑。** 新分析能力应先落在
  CLI 层（`d2j-ai.py` + `dex2jar-ai-cli`），skill 再去调用。
- skill 里引用的每个 `d2j-ai.py <command>` 都必须真实存在——校验脚本会检查。

  Skills only **orchestrate** existing `d2j-ai.py` commands — never inline
  command logic. Add new analysis to the CLI layer first.

### 改命令 / When touching the CLI

- 新命令注册到 `d2j-ai.py` 的 `COMMANDS` 表，保持 JSON 输出风格一致。
- 若有对应 slash command，请同步 `commands/<name>.md`。

### 加 trigger 用例 / Trigger fixtures

新增/调整路由时，更新 `evals/trigger-prompts/should-trigger.txt`
（每行以 `[dex2jar]` / `[apk-triage]` / `[sensitive-api-audit]` 标签开头）和
必要的反例 `should-not-trigger.txt`。

## 提交信息 / Commits

遵循 Conventional Commits：`feat:` / `fix:` / `docs:` / `chore:` 等。

## 行为准则 / Conduct

仅用于授权的安全研究、CTF、教学与防御性分析。请勿提交以破坏、规避检测或
大规模攻击为目的的内容。Authorized security research, CTF, education, and
defensive use only.

# 架构 / Architecture

本仓库把 Android 逆向能力分成四层。**上层只编排，不重复实现**——所有分析逻辑
最终收敛到一条 JSON CLI。

```
┌─────────────────────────────────────────────────────────────┐
│  入口层 / Entry points （三个入口，共享同一套能力）          │
│                                                               │
│   skills/            commands/            dex2jar-mcp/         │
│   (描述路由,         (斜杠命令,           (MCP server,         │
│    Claude 自选)      直接调用)            暴露给 MCP 客户端)   │
│   agents/reverse-engineer.md — 逆向分析 agent                 │
└───────────────┬───────────────┬───────────────┬──────────────┘
                │               │               │
                └───────────────┼───────────────┘
                                ▼
┌─────────────────────────────────────────────────────────────┐
│  编排/CLI 层 / Orchestration CLI                              │
│   d2j-ai.py — 29 条命令、统一 JSON 输出                       │
│   COMMANDS 表：dex2jar, baksmali, dex-xref, manifest-inspect, │
│                apk-cert, native-libs, dex-method-trace, ...   │
└───────────────────────────────┬─────────────────────────────┘
                                ▼
┌─────────────────────────────────────────────────────────────┐
│  实现层 / Implementation                                     │
│   dex2jar-ai-cli (Java)  — 新增的分析命令实现                 │
│   dex-tools / dex-* 模块 — 原 dex2jar 工具链（转换/汇编等）   │
└─────────────────────────────────────────────────────────────┘
```

## 关键原则 / Key principles

1. **单一真源。** 能力实现只在 CLI/Java 层存在一份。skills、commands、MCP 都是薄入口，
   调用同一条 `d2j-ai.py <command>`。
   *Single source of truth — entry points are thin wrappers over the CLI.*

2. **Skills 只编排。** `SKILL.md` 描述何时用、按什么顺序串哪些命令、如何解读输出；
   **不内联**命令逻辑。新分析先加到 CLI，再被 skill 引用。
   校验脚本会检查 skill 里引用的每个命令都真实存在于 `COMMANDS`。

3. **结构可离线校验，触发需模型。** `evals/validate-skills.sh` 离线检查清单/frontmatter/
   引用完整性（CI 门禁）；触发准确性靠 `evals/trigger-prompts/` + 模型评测。

## 目录速览 / Layout

| 路径 / Path | 作用 / Role |
|------|------|
| `skills/<name>/SKILL.md (+references/)` | 描述路由的逆向工作流 |
| `commands/*.md` | 斜杠命令 |
| `agents/reverse-engineer.md` | 逆向分析 agent |
| `d2j-ai.py` | 29 命令 JSON CLI（编排层） |
| `dex2jar-ai-cli/` | Java 实现的新分析命令 |
| `dex2jar-mcp/` | MCP server 入口 |
| `evals/` | 结构校验脚本 + trigger 用例 |
| `plugin.json` / `.claude-plugin/` | 插件 + marketplace 清单 |
| `docs/` | FAQ、本架构说明 |

详见根 [README.md](../README.md) 与 [FAQ.md](FAQ.md)。

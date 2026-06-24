# FAQ — 常见问题

## 这是什么？/ What is this?

一个面向 Android 逆向的 Claude Code 仓库，三种用法叠在同一套能力上：

- **Skills**（`skills/`）— Claude 按 `SKILL.md` 的 `description` 自动路由到的逆向工作流。
- **Slash commands**（`commands/`）— 直接调用单条能力的斜杠命令。
- **MCP server**（`dex2jar-mcp/`）— 把同一套命令暴露给支持 MCP 的客户端。

三者都落到 **`d2j-ai.py`**：一个 29 条命令、统一 JSON 输出的 CLI，背后是 `dex2jar-ai-cli`(Java)
和原有的 dex2jar 工具链。分层细节见 [architecture.md](architecture.md)。

## Skills 是怎么触发的？是 hook 吗？/ How do skills trigger?

**不是 hook，是 description 路由。** 每个 `skills/<name>/SKILL.md` 的 frontmatter 里有一段
trigger-rich 的 `description`（含中英文触发词），Claude 读到相关请求时自行选择加载哪个 skill。
所以没有正则 hook、没有常驻进程——这也是为什么离线、CI 里只能做**结构校验**而非触发校验
（触发准确性需要模型，见 `evals/README.md`）。

目前三个 skill：

- `dex2jar` — 主工作台（DEX/APK↔JAR、smali、签名、结构/字符串/调用图/依赖/清单/证书/native）。
- `apk-triage` — 未知 APK 的快速首过三连（manifest-inspect + apk-cert + native-libs + dex-strings）。
- `sensitive-api-audit` — 敏感 API 狩猎（dex-xref 预设 + dex-method-trace 回溯调用者）。

## 离线能用吗？/ Does it work offline?

能。命令与结构校验（`bash evals/validate-skills.sh`）都不联网。只有打 tag 发 Release、
`claude plugin add` 拉取仓库这类操作需要网络。

## dex → jar 的结果可靠吗？/ How accurate is dex→jar?

dex2jar 做的是 Dalvik 字节码到 JVM 字节码的转换，绝大多数情况可用，但**混淆、非法/损坏的 dex、
某些 Dalvik 专有构造**可能导致个别方法转换失败或语义偏差。把转换结果当作**辅助**而非
ground truth；关键逻辑用 `dex2smali` / `baksmali` 对照 smali 复核。细节见
`skills/dex2jar/references/dex-to-jar-accuracy.md`。

## Skills 和 commands、MCP 是什么关系？/ Relationship?

同一组能力的三个入口，**没有重复实现**：skills 编排、commands 直调、MCP 转发，全部最终调用
`d2j-ai.py`。新增分析能力时，逻辑加在 CLI 层，三个入口自然共享——skill 里**不应**内联命令逻辑。

## 计划里有什么？/ Roadmap?

见根目录 [ROADMAP.md](../ROADMAP.md)。

## 我能贡献吗？/ Can I contribute?

可以，见 [.github/CONTRIBUTING.md](../.github/CONTRIBUTING.md)。PR 前请本地跑通
`bash evals/validate-skills.sh`。

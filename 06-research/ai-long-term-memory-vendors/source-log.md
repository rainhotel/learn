# AI 厂商长期记忆实现调研 Source Log

## Source Template

### Source Title

- Type:
- Link or identifier:
- Date:
- Why it matters:
- Reliability:
- Used for:

## Sources

### OpenAI Memory FAQ

- Type: 官方帮助中心
- Link or identifier: https://help.openai.com/en/articles/8590148-memory-faq
- Date: updated 2026-03 左右，访问于 2026-03-30
- Why it matters: 说明 `ChatGPT` 的 saved memories、chat history、删除与控制逻辑
- Reliability: 高
- Used for: OpenAI 消费级长期记忆能力判断

### What is Memory? | OpenAI Help Center

- Type: 官方帮助中心
- Link or identifier: https://help.openai.com/en/articles/8983136
- Date: updated 2026-03 左右，访问于 2026-03-30
- Why it matters: 补充 free / plus / pro 的 memory 能力差异
- Reliability: 高
- Used for: OpenAI 记忆产品层定义

### Custom instructions with AGENTS.md – Codex

- Type: 官方产品文档
- Link or identifier: https://developers.openai.com/codex/guides/agents-md
- Date: 访问于 2026-03-30
- Why it matters: 说明 `Codex` 的持久化项目指令链与发现顺序
- Reliability: 高
- Used for: `Codex` 长期记忆中“显式规则层”的证据

### Compaction – OpenAI API

- Type: 官方 API 文档
- Link or identifier: https://developers.openai.com/api/docs/guides/compaction
- Date: 访问于 2026-03-30
- Why it matters: 说明 `Codex` 长时任务中的上下文压缩机制
- Reliability: 高
- Used for: `Codex` 长时任务续航机制判断

### Introducing GPT-5.2-Codex

- Type: 官方发布文章
- Link or identifier: https://openai.com/index/introducing-gpt-5-2-codex/
- Date: 2025-12-18
- Why it matters: 明确提到 `context compaction`、`long-horizon work`
- Reliability: 高
- Used for: `Codex` 产品定位与长任务能力

### How Claude remembers your project

- Type: 官方产品文档
- Link or identifier: https://code.claude.com/docs/en/memory
- Date: 访问于 2026-03-30
- Why it matters: `Claude Code` 长期记忆最核心的官方说明页
- Reliability: 高
- Used for: `Claude Code` 记忆结构、存储、加载与控制方式

### Get personalisation with memory of your past Gemini chats

- Type: 官方帮助中心
- Link or identifier: https://support.google.com/gemini/answer/16598469
- Date: 访问于 2026-03-30
- Why it matters: 说明 `Gemini` 如何使用 past chats 形成个性化记忆
- Reliability: 高
- Used for: Google 路线判断

### Get personalisation in Gemini Apps

- Type: 官方帮助中心
- Link or identifier: https://support.google.com/gemini/answer/16598623
- Date: 访问于 2026-03-30
- Why it matters: 把 past chats、connected apps、instructions 三种个性化来源放到同一框架中
- Reliability: 高
- Used for: Google 记忆层次总结

### Get started with personalizing what Microsoft 365 Copilot remembers

- Type: 官方帮助中心
- Link or identifier: https://support.microsoft.com/en-gb/topic/get-started-with-personalizing-what-microsoft-365-copilot-remembers-cba7b79a-c46f-4ca7-b46e-2fa22c563f90
- Date: 访问于 2026-03-30
- Why it matters: 说明 `Copilot Memory`、history inference、custom instructions、temporary chat 的关系
- Reliability: 高
- Used for: Microsoft 路线判断

### Manage Copilot Memory in Microsoft 365 Copilot

- Type: 官方帮助中心
- Link or identifier: https://support.microsoft.com/en-us/topic/manage-copilot-memory-in-microsoft-365-copilot-b3231eae-9e60-4b3c-ac58-81fddbe56279
- Date: last updated 2026-02
- Why it matters: 说明 saved memories 的写入、提示、删除与“Memory updated”
- Reliability: 高
- Used for: Microsoft 的显式记忆层

### MemGPT: Towards LLMs as Operating Systems

- Type: 论文 / arXiv
- Link or identifier: https://arxiv.org/abs/2310.08560
- Date: 2023-10-12，修订于 2024-02-12
- Why it matters: 分层内存与虚拟上下文管理的代表工作
- Reliability: 高
- Used for: 映射 `Codex` 的 compaction / tiered context 思路

### MemoryBank: Enhancing Large Language Models with Long-Term Memory

- Type: 论文 / arXiv
- Link or identifier: https://arxiv.org/abs/2305.10250
- Date: 2023-05-17，修订于 2023-05-21
- Why it matters: 显式长期记忆库 + 遗忘曲线机制的代表工作
- Reliability: 高
- Used for: 映射显式可管理记忆库思路

### Recursively Summarizing Enables Long-Term Dialogue Memory in Large Language Models

- Type: 论文 / arXiv
- Link or identifier: https://arxiv.org/abs/2308.15022
- Date: 2023-08-29，修订于 2025-08-25
- Why it matters: 递归摘要作为长对话记忆手段
- Reliability: 高
- Used for: 映射 `Codex` 的 compaction / summarization 路线

### Generative Agents: Interactive Simulacra of Human Behavior

- Type: 论文 / arXiv
- Link or identifier: https://arxiv.org/abs/2304.03442
- Date: 2023-04-07，修订于 2023-08-06
- Why it matters: memory stream + reflection + retrieval 的经典代理架构
- Reliability: 高
- Used for: 映射 agent memory 的 observation / reflection / planning 框架

### From Human Memory to AI Memory: A Survey on Memory Mechanisms in the Era of LLMs

- Type: 论文 / arXiv survey
- Link or identifier: https://arxiv.org/abs/2504.15965
- Date: 2025-04-22，修订于 2025-04-23
- Why it matters: 用更系统的分类法整理 LLM memory 研究
- Reliability: 高
- Used for: 给产品层比较提供术语框架

### get-shit-done (GSD)

- Type: GitHub repository + user guide
- Link or identifier: https://github.com/gsd-build/get-shit-done
- Date: accessed 2026-03-30
- Why it matters: 展示了一个围绕 Claude Code 的外部规范/状态工件系统，重点解决 context rot 与项目规范持久化
- Reliability: 中高（开源仓库自述 + 用户指南，非模型厂商官方）
- Used for: 作为“生态层长期记忆补丁”的代表案例

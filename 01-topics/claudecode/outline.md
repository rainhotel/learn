# ClaudeCode 源码学习 Mastery Outline

## Phase 0: Positioning And Scope

- 先区分“模型推理逻辑”和“agent 外围调度逻辑”
- 明确当前主题聚焦 ClaudeCode SDK / headless 会话主干
- 先修：TypeScript、async iterator、状态管理、持久化、tool 调用

## Phase 1: ClaudeCode 架构地图

- 建立从 `ask()` 到 `QueryEngine` 到 `query()` 的调用地图
- 识别 message、tool、session、plugin、memory、MCP 这些子系统的位置
- 用自己的话描述“一次 turn 的完整生命周期”

## Phase 2: Input And Setup Layer

- 读懂 `QueryEngineConfig` 的依赖注入方式
- 读懂 `submitMessage()` 前半段：system prompt、skills、plugins、permissions、processUserInput
- 搞清楚为什么 `processUserInputContext` 要构建两次

## Phase 3: Main Loop And Message Flow

- 读懂 `for await (const message of query(...))`
- 理解不同 `message.type` 的分支处理和 SDK 输出映射
- 建立 assistant / user / progress / attachment / system 的流向图

## Phase 4: Persistence, Resume, And Safety

- 理解 transcript 为什么要“尽早写”和“有些 await、有些 fire-and-forget”
- 理解 compact boundary 为什么会裁掉旧消息
- 理解权限拒绝、max turns、budget、structured output retry 这些安全边界

## Phase 5: Deliberate Practice

- 不看源码复述 `submitMessage()` 的 10 个阶段
- 手画时序图：用户输入、query 流、transcript、result
- 选 3 处注释，反推出它们在修什么历史 bug

## Phase 6: Build Something Real

- 用自己的代码实现一个极简版 turn engine
- 只支持 user / assistant / tool_result / usage，但保留相同调度骨架
- 为 transcript resume 与 maxBudget 写出最小测试

## Phase 7: Explain, Teach, Defend

- 解释“这个文件为什么长，但并不是坏代码”
- 解释“为什么外围工程代码往往比核心算法更难读”
- 能比较 QueryEngine 与更简单的 request handler 有何不同

## Phase 8: Frontier And Taste

- 比较 ClaudeCode、Codex、Cursor 一类 agent 编排层的共同模式
- 形成自己对“长函数 orchestrator 应如何写”的判断标准
- 形成对 streaming、resume、tool-safety 取舍的工程审美

## Phase Exit Criteria

### 每一阶段结束前都检查

- 我能不能解释而不是复述？
- 我能不能举出反例和边界？
- 我能不能做一个最小实践？
- 我能不能指出常见误区？

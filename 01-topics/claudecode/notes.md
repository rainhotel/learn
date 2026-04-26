# ClaudeCode 源码学习 Notes

## 直接结论

- 这份代码不是在“生成回答”，而是在“编排一次会话回合”。
- `QueryEngine` 是一个有状态的对话引擎对象，负责跨 turn 保留消息、缓存、usage、权限拒绝记录等状态。
- `submitMessage()` 是真正的核心。它接收一次用户输入，处理本地命令和上下文，必要时调用 `query()`，把内部消息流转成 SDK 消息，并在过程中维护 transcript、预算、usage、compact 和最终结果。
- `ask()` 只是一个一次性包装器。它创建 `QueryEngine`，把参数塞进去，执行一次 `submitMessage()`，最后把读文件缓存写回。
- 真正跟模型交互、产出 assistant/progress/attachment/system 消息的核心循环不在这里，而在 `query()` 里。这份文件的重点是“外围控制层”。

## 一句话定位

这份文件可以看成 ClaudeCode 的“会话 orchestration 层”。它把很多外围职责绑在一次 turn 上：

- system prompt 组装
- skills / plugins 初始化
- 用户输入预处理
- transcript 持久化
- query 流消费
- SDK 消息归一化
- usage / budget 统计
- 异常与边界处理

## 先看结构

最先应该抓住下面这段字段定义，因为它直接暴露了这个类“持有状态”的本质：

```ts
export class QueryEngine {
  private config: QueryEngineConfig
  private mutableMessages: Message[]
  private abortController: AbortController
  private permissionDenials: SDKPermissionDenial[]
  private totalUsage: NonNullableUsage
  private hasHandledOrphanedPermission = false
  private readFileState: FileStateCache
  private discoveredSkillNames = new Set<string>()
  private loadedNestedMemoryPaths = new Set<string>()
}
```

直接结论：

- 这不是一个纯函数式 request handler。
- 它是“一个会话一个 engine”的设计。
- conversation 级状态会存活在这个实例里，`submitMessage()` 每调用一次，只是走完其中一个 turn。

源码自己也把这个设计写出来了：

```ts
/**
 * One QueryEngine per conversation. Each submitMessage() call starts a new
 * turn within the same conversation. State (messages, file cache, usage, etc.)
 * persists across turns.
 */
```

## 三个关键对象

### 1. `QueryEngineConfig`

- 它是依赖注入入口。
- 几乎所有外部能力都不是在类里 new 出来的，而是通过 config 注入：
  - `tools`
  - `commands`
  - `mcpClients`
  - `getAppState` / `setAppState`
  - `canUseTool`
  - `readFileCache`
  - `setSDKStatus`
  - `handleElicitation`
- 这意味着 `QueryEngine` 不是底层基础设施，而是上层编排器。

### 2. `QueryEngine`

- 它持有会话状态。
- 它负责 turn 级别的 orchestration。
- 它提供 `interrupt()`、`getMessages()`、`setModel()` 这种面向会话对象的操作。

### 3. `ask()`

它很薄，核心逻辑只有这几步：

```ts
const engine = new QueryEngine({
  ...
})

try {
  yield* engine.submitMessage(prompt, {
    uuid: promptUuid,
    isMeta,
  })
} finally {
  setReadFileCache(engine.getReadFileState())
}
```

直接结论：

- `ask()` 不是主逻辑。
- `ask()` 的职责是“构造一个 engine，然后把执行权交给 `submitMessage()`”。
- 如果你想读懂行为，主战场一定在 `submitMessage()`。

## `submitMessage()` 主流程

先看函数签名：

```ts
async *submitMessage(
  prompt: string | ContentBlockParam[],
  options?: { uuid?: string; isMeta?: boolean },
): AsyncGenerator<SDKMessage, void, unknown>
```

这说明两件事：

- 输入是一条 prompt 或一组内容块。
- 输出不是单个结果，而是一串 `SDKMessage` 流。

把这个长函数拆开，你可以按下面 10 个阶段读。

### 阶段 1：解构配置，重置 turn 级状态

- 从 `this.config` 拿出当前 turn 需要的依赖。
- 清空 `this.discoveredSkillNames`。
- `setCwd(cwd)` 让后续工具都跑在正确目录。
- 记录 `persistSession` 和 `startTime`。

### 阶段 2：包装权限检查

```ts
const wrappedCanUseTool: CanUseToolFn = async (...) => {
  const result = await canUseTool(...)
  if (result.behavior !== 'allow') {
    this.permissionDenials.push({
      tool_name: sdkCompatToolName(tool.name),
      tool_use_id: toolUseID,
      tool_input: input,
    })
  }
  return result
}
```

直接结论：

- 真正的权限判断还是原来的 `canUseTool` 做。
- 这里做的是“旁路采样”，把拒绝记录下来，最后塞到 SDK result 里。

### 阶段 3：构建 system prompt 和上下文

这一段会做很多准备工作：

- 读取 `AppState`
- 解析模型
- 生成 thinkingConfig
- `fetchSystemPromptParts(...)`
- 合成 `userContext`、`systemContext`
- 特定条件下注入 memory mechanics prompt
- 合成最终 `systemPrompt`

这里的关键认识是：这份文件负责准备 prompt 环境，但不负责真正发模型请求。

### 阶段 4：构建第一次 `processUserInputContext`

第一次 context 很特别，里面的 `setMessages` 不是空操作：

```ts
setMessages: fn => {
  this.mutableMessages = fn(this.mutableMessages)
},
```

这意味着 slash command 在预处理阶段可以直接改历史消息。

为什么重要：

- 因为有些命令不是“生成一条新消息”，而是“改会话历史”。
- 如果你忽略这一点，就会看不懂为什么后面还要重新建一次 context。

### 阶段 5：处理 orphaned permission 和用户输入

这里依次做两件事：

1. 如果有 `orphanedPermission`，先走 `handleOrphanedPermission(...)`
2. 再走 `processUserInput(...)`

`processUserInput(...)` 的产出特别关键：

- `messagesFromUserInput`
- `shouldQuery`
- `allowedTools`
- `modelFromUserInput`
- `resultText`

直接结论：

- 用户输入进来后，不一定会走模型查询。
- 本地 slash command 可能直接给出结果并结束。
- 所以 `submitMessage()` 其实先是“命令路由器”，然后才是“查询编排器”。

### 阶段 6：先把用户消息写进 transcript

这是整份代码里最值钱的工程细节之一。

```ts
if (persistSession && messagesFromUserInput.length > 0) {
  const transcriptPromise = recordTranscript(messages)
  if (isBareMode()) {
    void transcriptPromise
  } else {
    await transcriptPromise
    ...
  }
}
```

下面的注释直接说了原因：如果进 API 之前进程就被杀掉，没有这一步，`--resume` 会找不到这次对话。

直接结论：

- 这是“先持久化输入，再进入远端交互”的恢复性设计。
- 不是为了性能，是为了 crash / stop / kill 后还能 resume。

### 阶段 7：如果不需要 query，就走本地结果返回

当 `shouldQuery` 为 `false` 时，这里直接：

- 回放本地命令产生的 user/system 消息
- 记录 transcript
- 产出一个 `result: success`
- `return`

这说明 `submitMessage()` 有两种模式：

- 本地处理模式
- 远端 query 模式

### 阶段 8：进入真正的 query 流

核心入口就是：

```ts
for await (const message of query({
  messages,
  systemPrompt,
  userContext,
  systemContext,
  canUseTool: wrappedCanUseTool,
  toolUseContext: processUserInputContext,
  fallbackModel,
  querySource: 'sdk',
  maxTurns,
  taskBudget,
})) {
  ...
}
```

直接结论：

- `query()` 是上游消息生产者。
- `submitMessage()` 是下游消费与编排者。
- 这份文件的主要工作是“消费 query 流，并把流上的事件变成稳定的 SDK 输出和会话状态变更”。

### 阶段 9：按 `message.type` 分发处理

这是整个函数最长的部分，但其实是一个很标准的事件分发器。

主要分支如下：

- `assistant`
- `progress`
- `user`
- `stream_event`
- `attachment`
- `system`
- `tool_use_summary`
- `tombstone`

你应该这样理解它们：

- `assistant` / `user`：对话内容本身
- `progress`：过程更新
- `stream_event`：底层流式事件，主要用于 usage 和 stop_reason
- `attachment`：附带控制信息，例如 structured output、max turns、queued command
- `system`：系统信号，例如 compact boundary、api retry

### 阶段 10：做预算、重试和最终结果收尾

在循环过程中不断检查：

- 是否超出 `maxBudgetUsd`
- structured output retry 是否超限
- attachment 是否声明 `max_turns_reached`

最后再做：

- 找出最后一个 assistant 或 user 消息
- 判断 `isResultSuccessful(...)`
- 提取 text result
- 组装统一的 `result` 消息返回

## 五个第一次看最容易漏掉的设计点

### 1. `mutableMessages` 和 `messages` 不是重复变量

这里同时维护两套消息数组：

- `this.mutableMessages`：会话级真实状态
- `messages`：当前 turn 的局部工作集 / transcript 视图

为什么要这样做：

- `mutableMessages` 要跨 turn 存活
- `messages` 要被当前 turn 随时切片、裁剪、写 transcript
- 某些阶段需要以“快照”方式工作，而不是直接拿同一个引用满地改

如果你第一次看就把它们当成重复代码，很容易误判。

### 2. `processUserInputContext` 重建两次是故意的

第一次是为了让 slash command 能修改消息历史。

第二次是在 `processUserInput(...)` 之后，把：

- 更新后的 `messages`
- 更新后的 `model`
- 新的上下文状态

重新绑定进去。

这不是啰嗦，而是把“预处理阶段”和“正式 query 阶段”分开。

### 3. assistant transcript 写入故意不 await

这段注释非常值得学：

```ts
// Fire-and-forget for assistant messages.
// Awaiting here blocks ask()'s generator, so message_delta can't run
// until every block is consumed
```

直接结论：

- 如果这里 await，每个 assistant block 都会把流卡住。
- 一旦卡住，后面的 `message_delta` 拿不到，usage / stop_reason 也会乱。
- 所以它选择保序队列 + fire-and-forget，而不是同步等待。

### 4. `lastStopReason` 不是从 assistant message 本体拿的

代码明确写着：

```ts
if (message.event.type === 'message_delta') {
  ...
  if (message.event.delta.stop_reason != null) {
    lastStopReason = message.event.delta.stop_reason
  }
}
```

结论：

- assistant 内容块结束时，`stop_reason` 可能还是 `null`
- 真正可靠的 stop reason 在 streaming delta 里
- 所以必须监听 `stream_event`

### 5. 这份文件里大量注释其实是在讲“历史 bug”

例如：

- 为什么用户消息要在 query 前先写 transcript
- 为什么 compact boundary 前要先 flush preserved segment tail
- 为什么 progress / attachment 要 inline record
- 为什么不能简单 `last(messages)` 取结果

阅读建议：

- 不要把这些注释当噪音
- 这些注释其实在暴露系统最脆弱的边界条件
- 真正值得学的工程经验，大多都藏在这里

## 你现在可以怎么记这份文件

最稳的脑图不是逐行，而是四条主线：

1. 输入主线：`prompt -> processUserInput -> shouldQuery`
2. 会话主线：`mutableMessages / messages / transcript / compact`
3. 流处理主线：`query() -> message.type switch -> SDK output`
4. 收尾主线：`usage / budget / success check / result`

## 常见误区

- 误区 1：以为这是模型调用实现
  - 纠正：这是模型调用外面的 orchestration 层。
- 误区 2：以为 `ask()` 很重要
  - 纠正：`ask()` 只是薄包装。
- 误区 3：以为长函数一定是坏味道
  - 纠正：在这种“事件编排器”里，长函数有时是在集中表达状态转移。
- 误区 4：只看业务分支，不看注释
  - 纠正：这份文件最有价值的反而是注释里透露出的恢复性设计和 streaming 陷阱。

## 下一跳

- 下一步先读 `query()`，只回答一个问题：它到底会产出哪些 `Message`。
- 再补 `normalizeMessage()` 和 `isResultSuccessful()`，把 result 判定链路打通。
- 最后单独读 transcript / compact / resume，理解为什么这套系统这么重视“进程被杀后还能续上”。

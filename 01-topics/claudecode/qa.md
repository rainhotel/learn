# ClaudeCode 源码学习 Q&A

## Open Questions

- [ ] `query()` 在哪些条件下会 yield `attachment`、`progress`、`system`？
- [ ] `snipReplay` 与 `compact boundary` 的完整生命周期是什么？
- [ ] `normalizeMessage()` 具体把哪些内部消息变成哪些 SDK 消息？

## Error Patterns

- 最近反复出现的错误：一上来逐行看长函数，忘了先抓职责边界
- 最近最容易混淆的概念：`messages` 与 `mutableMessages` 的区别

## Answered Questions

### 为什么 `submitMessage()` 里同时有 `mutableMessages` 和 `messages`？

- 结论：前者是 engine 持久状态，后者是本 turn 的局部工作集与 transcript 视图。
- 依据：`this.mutableMessages` 挂在类字段上，`messages` 则是 `const messages = [...this.mutableMessages]` 创建出来的局部变量。
- 例子：compact boundary 时会裁剪两边，但语义并不相同。

### 为什么 `processUserInputContext` 要建两次？

- 结论：第一次服务预处理和 slash command 改历史，第二次服务正式 query。
- 依据：第一次的 `setMessages` 真正会改 `this.mutableMessages`，第二次则变成 no-op。
- 例子：如果 slash command 修改了模型或消息历史，正式 query 阶段必须基于更新后的上下文重新绑定。

### 为什么 transcript 要在 query 前先写一次？

- 结论：为了保证进程在 API 返回前被杀掉时仍能 resume。
- 依据：源码注释明确提到 “If the process is killed before that ... --resume fails”。
- 例子：用户刚发送消息就点 Stop，如果没先写 transcript，这轮输入就丢了。

### 为什么 `stop_reason` 要从 `stream_event.message_delta` 抓？

- 结论：assistant 内容块结束时 stop_reason 可能还是空，真实值晚一点才到。
- 依据：源码注释写明 “the real value only arrives here”。
- 例子：如果只看 `assistant.message.stop_reason`，最后 result 里很容易永远是 `null`。

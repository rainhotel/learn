# 第 18 章：Tool、Memory 与可靠 Agent Runtime

## 章节定位

- 类型：Agent Architecture + Java Backend + Reliability + Security + Lab Design + Interview
- 难度：深入
- 建议学习时间：28-36 小时
- 先修章节：第 08 章恢复控制面、第 09 章可观测性、第 14 章 LLM/RAG
- 对应项目：NotifyFlow Incident Agent 与安全工具执行平台

## 当前状态

- 阶段：八件套完整内容初稿，实验 Pending
- 调研日期：2026-07-15
- 已完成：Agent 状态机、Tool 合同、Memory、权限、幂等、人工审批、恢复和评测设计
- 未完成：Java Runtime、模型调用、工具沙箱、状态存储、评测集和故障实验

本章不能标记为 Lab Verified、Release Candidate 或 Released。

## 核心问题

1. Agent 与确定性工作流、RAG Chatbot 的边界是什么？
2. Tool schema、权限、幂等、side effect 等级和审计如何设计？
3. 模型超时、工具超时、UNKNOWN、重复调用和部分成功如何恢复？
4. 会话上下文、工作记忆、长期知识和业务状态如何分层？
5. 多 Agent 是否真正需要？任务委派、共享状态和冲突如何控制？
6. prompt injection、数据外泄、越权工具和恶意工具结果如何处理？
7. Java 后端如何实现状态机、SSE、取消、预算、Trace 和人工审批？
8. Agent 如何评测正确性、完成率、安全、成本和恢复能力？

## 退出标准

- 能把 Agent 画成有界状态机，而不是无限循环。
- 能为只读、低风险写、高风险写三类 Tool 设计不同控制面。
- 能实现幂等键、UNKNOWN 对账、审批、审计和安全重放设计。
- 能区分 Memory 与业务真实状态，避免只靠 prompt 保存事实。
- 能解释多 Agent 的收益、通信成本和错误放大。
- 能设计 Java Runtime 的模型网关、工具执行器、事件存储和 SSE。
- 能建立任务完成、安全、引用、成本和故障恢复评测集。

## 发布前缺口

- 固定模型 API、Java SDK、状态存储和工具协议版本。
- 完成 Agent Runtime 最小实现和可恢复事件日志。
- 完成只读/写入/高风险工具的权限与审批实验。
- 完成 timeout/UNKNOWN/重复调用/崩溃恢复和 prompt injection 实验。
- 完成单 Agent/工作流/多 Agent 的消融评测。
- 完成学习者项目答辩和 Teach-back。

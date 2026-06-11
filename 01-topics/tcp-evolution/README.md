# TCP 发展历史和发展历程

## Goal

- 围绕课程设计选题，系统梳理 TCP 从标准化到现代 Web 场景中的关键演进。
- 不只记“发生了什么”，还要讲清“为什么要改”和“这些改动解决了什么问题”。
- 最终形成一份可直接支撑实验报告的知识框架、对话脚本和知识点清单。

## Scope

- 覆盖 TCP 的关键历史阶段：标准化、拥塞控制、高性能扩展、丢包恢复、Web 优化、现代拥塞控制。
- 聚焦 Web 应用中与 TCP 强相关的演进：短连接到持久连接、HTTP/2 多路复用、连接建立时延优化。
- 聚焦分布式高可用高并发 Web 应用里 TCP 的作用与边界。

## Non-Scope

- 不展开 TCP 首部逐字段背诵。
- 不把 QUIC/TLS 细节作为主线，只在比较 TCP 局限时简要提到。
- 不做内核源码级实现分析。

## Outcome

- 能按时间线讲清 TCP 的关键发展阶段。
- 能说明每一次重要改进背后的网络问题和工程动机。
- 能解释 TCP 在现代 Web 系统中的核心作用、收益和局限。
- 能从 AI 对话中稳定提炼出不少于 10 个知识点用于实验报告。

## Status

- 阶段：Phase 3 - Core Mechanisms
- 优先级：High
- 最近一次更新：2026-06-11
- 当前学习模式：Course-design report support

## Core Resources

- RFC 793, *Transmission Control Protocol*, September 1981
  - https://www.rfc-editor.org/rfc/rfc793
- RFC 1122, *Requirements for Internet Hosts*, October 1989
  - https://www.rfc-editor.org/rfc/rfc1122
- RFC 1323, *TCP Extensions for High Performance*, May 1992
  - https://www.rfc-editor.org/rfc/rfc1323
- RFC 2018, *TCP Selective Acknowledgment Options*, October 1996
  - https://www.rfc-editor.org/rfc/rfc2018
- RFC 2616, *HTTP/1.1*, June 1999
  - https://www.rfc-editor.org/rfc/rfc2616
- RFC 5681, *TCP Congestion Control*, September 2009
  - https://www.rfc-editor.org/rfc/rfc5681
- RFC 6928, *Increasing TCP's Initial Window*, April 2013
  - https://www.rfc-editor.org/rfc/rfc6928
- RFC 7413, *TCP Fast Open*, December 2014
  - https://www.rfc-editor.org/rfc/rfc7413
- RFC 7540, *HTTP/2*, May 2015
  - https://www.rfc-editor.org/rfc/rfc7540
- RFC 9438, *CUBIC for Fast Long-Distance Networks*, August 2023
  - https://www.rfc-editor.org/rfc/rfc9438

## Next 3 Actions

1. 按 `outline.md` 把实验报告主线讲顺。
2. 从 `02-journal/2026/06/2026-06-11.md` 中截取 4 到 6 轮对话作为实验过程。
3. 用 `03-outputs/tcp-course-design-report.md` 直接改写成最终报告。

## Human And AI Views

- 给人看：`human-guide.md`
- 给 AI 看：`ai-context.md`
- 解题归档：`solved-problems.md`
- 进度跟踪：`progress.md`

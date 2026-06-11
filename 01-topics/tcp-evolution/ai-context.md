# TCP 发展历史和发展历程 AI Context

## Topic State

- Current phase: Report drafting
- Confidence estimate: 0.86
- Last updated: 2026-06-11

## Dependency Map

- 先修知识：
  - TCP 基本职责
  - HTTP/1.0、HTTP/1.1、HTTP/2 基本差异
  - 吞吐、RTT、带宽时延积基本概念
- 当前核心概念：
  - 标准化
  - 拥塞控制
  - 高性能扩展
  - 丢包恢复
  - Web 连接复用
  - 高并发系统中的 TCP 角色
- 后续高级主题：
  - QUIC vs TCP
  - CUBIC/BBR 拥塞控制比较
  - 长连接网关与连接池调优

## Knowledge Gaps

- 还没覆盖的基础：RFC 9293 对 TCP 主规范的整合可作为后续补充
- 还没打通的机制：CUBIC 与 BBR 的定量对比暂未展开
- 只会用但不会解释的点：HTTP/2 的 TCP 队头阻塞与 QUIC 的改进关系

## Extraction Backlog

- 可以从本次 journal 提升到 `notes.md` 的：
  - 对话式知识点提炼法
  - Web 场景下的 TCP 改进主线
- 可以提升到 `qa.md` 的：
  - Keep-Alive 是否等于 TCP 新机制
  - HTTP/2 是否解决所有阻塞
- 可以提升到 `projects.md` 的：
  - 本次课程设计报告

## Source Map

- 当前最值得参考的资料：
  - RFC 793
  - RFC 1323
  - RFC 2018
  - RFC 5681
  - RFC 6928
  - RFC 7413
  - RFC 7540
  - RFC 9438
- 哪些资料只是扫过：
  - TCP Wikipedia 条目，仅作时间线辅助，不作为主证据
- 哪些资料需要二次验证：
  - BBR 的历史描述如果写进正式报告，需要补官方论文或原始发布材料

## Next Best Edits

1. 如果老师要求更正式的“参考文献”格式，可把 RFC 整理成 GB/T 7714。
2. 如果老师要求截图更多，可继续扩展 `02-journal/2026/06/2026-06-11.md` 的对话轮次。
3. 如果老师要求“实验心得”更长，可在 `review.md` 基础上展开。

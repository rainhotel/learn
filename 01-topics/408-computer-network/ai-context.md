# 408 计算机网络 AI Context

## Topic State

- Current phase: exam-map-built-start-problem-training
- Confidence estimate: medium
- Last updated: 2026-06-20
- User intent: 参考王道体系和历年 408 真题高频题型，先列知识点大纲，再逐步整理可复习资料。

## Source Boundary

- 可以总结知识点、考法、题型模板和解题步骤。
- 不应复刻王道原文或真题原题大段内容。
- 若用户提供题目截图或文字，可按 `solved-problems.md` 模板整理来源、步骤、公式、适用条件、结论。

## Current Artifacts

- `README.md`: 主题目标、范围、入口。
- `outline.md`: 已重写为完整 408 计算机网络知识点大纲。
- `notes.md`: 已在开头新增考点整理总表，后面保留既有链路层学习笔记。
- `formula-sheet.md`: 已新增 408 高频公式索引。
- `qa.md`: 已补充高频易混问答。
- `solved-problems.md`: 已有较多链路层题目归档，网络层和运输层题目仍不足。
- `progress.md`: 用于跟踪下一阶段训练。

## Dependency Map

- 基础依赖：二进制、补码/按位与、对数、简单代数、单位换算。
- 体系结构依赖：协议/服务/接口、五层模型、PDU 与封装。
- 数据链路层依赖：帧、MAC、CRC、滑动窗口、冲突域/广播域。
- 网络层依赖：IPv4 地址、子网掩码、CIDR、路由表、MTU。
- 运输层依赖：端口、TCP 字节流、seq/ack、rwnd/cwnd。

## Exam Mapping

- 高频综合层：数据链路层 / 网络层 / 运输层。
- 当前最应推进：网络层题型归档，其次运输层题型归档。
- 选择题稳分层：体系结构、物理层、应用层。
- 公式高风险点：窗口利用率、子网数量、路由聚合、IP 分片、TCP 拥塞窗口。

## Knowledge Gaps

- 网络层代表题不足：子网划分、CIDR、最长前缀匹配、IP 分片需要补题。
- 运输层代表题不足：TCP 序号确认号、握手挥手、拥塞窗口变化需要补题。
- 应用层还未做成独立协议对比表，可在冲刺阶段补。
- 物理层只建立了公式索引，还缺奈奎斯特/香农/CDMA 代表题。

## Extraction Backlog

1. 从用户后续题目中抽取网络层题型，写入 `solved-problems.md`。
2. 把每次错因用问答形式追加到 `qa.md`。
3. 把新出现的公式或边界条件追加到 `formula-sheet.md`。
4. 若用户开始整套真题训练，按年份或题型在 `solved-problems.md` 建分组。

## Next Best Edits

1. 新增“网络层专项题组”：子网划分、CIDR、分片、最长前缀匹配。
2. 新增“运输层专项题组”：TCP ACK、握手挥手、拥塞控制。
3. 做一页“应用层协议端口对比表”的冲刺版。

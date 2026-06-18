# Computer Organization Course Design

## Goal

- 完成“计算机组成及系统结构课程设计”中选择的两个实验：
  - 实验 5-1：单总线 CPU 设计，定长指令周期 3 级时序。
  - 实验 5-3：单总线 CPU 设计，现代时序。
- 能看懂 Logisim 单总线 CPU 的关键连线，并能解释控制器为什么这样接。

## Scope

- 当前只覆盖课程设计任务书中的第 1、3 个处理器实验。
- 重点是 Logisim 接线、控制信号、微程序 ROM、报告可写内容。
- 暂不覆盖中断版本、RISC-V 版本和 MIPS 24 条指令单周期处理器。

## Outcome

- 能分清模板文件、完成文件和 Excel 自动生成表的用途。
- 能在 Logisim 中补齐实验 5-1 的定长 3 级时序控制器。
- 能在 Logisim 中补齐实验 5-3 的微程序控制器、入口逻辑和判别逻辑。
- 能把实验过程整理进课程设计报告。

## Status

- 阶段：In progress
- 优先级：High
- 最近一次更新：2026-06-18
- 当前学习模式：Course-design build guide

## Core Resources

- 课程任务书：`D:\moniC\project\learn\ver2-课程设计文件\ver2-课程设计文件\计组课设-题目与要求设计任务书.docx`
- 封面模板：`D:\moniC\project\learn\ver2-课程设计文件\ver2-课程设计文件\课程设计封面.docx`
- 单总线资料包：`D:\moniC\project\learn\hustzc\7.单总线CPU\单总线实验资料包(愚人节版)`

## Next 3 Actions

1. 按 `troubleshooting.md` 先确认 `ffffffff` 出现在 `$s0` 还是 `PC`。
2. 检查 `PC/IR/mAddr(or S)/PCin/内部总线/Rin` 六个探针。
3. 若核心取指正常，再查 `Clks/指令数` 计数逻辑。

# 图像直方图处理实验

## Goal

- 完成本次 Matlab 图像处理实验，并能独立解释每个任务背后的原理。
- 不只跑通函数，还能说明人工阈值、Otsu、均衡化、规定化、CLAHE 之间的区别。
- 产出一份可直接整理成实验报告的材料包。

## Scope

- 聚焦灰度图像的直方图分析与增强。
- 覆盖人工阈值二值化、`graythresh`/Otsu、手写直方图均衡化、直方图规定化、自适应直方图均衡化。
- 暂不展开彩色图像直方图、形态学后处理、多阈值分割。

## Outcome

- 能根据直方图大致判断前景和背景分布。
- 能解释 `graythresh` 返回值的意义，并和人工选阈值做对比。
- 能自己写出直方图均衡化核心映射，而不依赖 `histeq`。
- 能把实验结果整理成 6 页以内的实验报告。

## Status

- 阶段：Phase 1 - Lab execution prep
- 优先级：High
- 最近一次更新：2026-04-02
- 当前学习模式：Mastery roadmap

## Core Resources

- 资源 1：本主题 `human-guide.md`
- 资源 2：本主题 `notes.md`
- 资源 3：本主题 Matlab 脚本 `histogram_lab.m`

## Next 3 Actions

1. 把实验图像放到 Matlab 当前目录，或修改 `histogram_lab.m` 里的 `imageDir`。
2. 先跑实验 1 和 2，记录三幅图的人工阈值、Otsu 阈值和现象差异。
3. 再跑实验 3 到 5，截图后按 `projects.md` 和 `human-guide.md` 组装报告。

## Human And AI Views

- 给人看：`human-guide.md`
- 给 AI 看：`ai-context.md`
- 解题归档：`solved-problems.md`
- 进度跟踪：`progress.md`

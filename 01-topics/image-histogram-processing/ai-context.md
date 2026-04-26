# 图像直方图处理实验 AI Context

## Topic State

- Current phase: Lab scaffold ready, pending real image runs
- Confidence estimate: Medium
- Last updated: 2026-04-02

## Dependency Map

- 先修知识：灰度图像、像素值范围、概率统计基础、Matlab 基本图像函数
- 当前核心概念：直方图、二值化阈值、Otsu 判据、CDF 映射、直方图规定化、CLAHE
- 后续高级主题：局部阈值分割、形态学去噪、彩色图像增强、多尺度对比度增强

## Knowledge Gaps

- 还没覆盖的基础：为什么 Otsu 对双峰直方图效果最好
- 还没打通的机制：规定化为何只能近似匹配目标直方图
- 只会用但不会解释的点：CLAHE 中 `ClipLimit` 和 `NumTiles` 的影响

## Extraction Backlog

- 把真实实验结果整理进 `projects.md`
- 把人工阈值与 Otsu 的差异提升到 `qa.md`
- 把手写均衡化映射公式和实现细节沉淀进 `notes.md`

## Source Map

- 当前最值得参考的资料：实验题目说明、Matlab 文档 `graythresh` `histeq` `adapthisteq`
- 哪些资料只是扫过：课堂 PPT 或教材中关于直方图增强的章节
- 哪些资料需要二次验证：不同版本 Matlab 对 `histeq`/`adapthisteq` 参数说明

## Next Best Edits

1. 填入三幅图像真实的人工阈值和 Otsu 阈值
2. 把实验运行截图对应到 `projects.md`
3. 写一版 6 页以内的报告正文压缩稿

# 图像直方图处理实验 Review

## What I Understand Well

- 直方图描述的是灰度分布，不是空间结构。
- Otsu 属于全局自动阈值方法。
- 直方图均衡化和规定化本质上都是灰度映射。

## What Still Feels Fuzzy

- 为什么某些图像人工阈值比 Otsu 更符合任务目标。
- 规定化结果为什么通常只能近似匹配目标直方图。
- CLAHE 参数变化和噪声放大之间的关系。

## What Changed In My Understanding

- 以前更像是在背函数名，现在开始把这些函数放回“阈值选择”和“灰度映射”两个大框架里理解。

## Evidence

- 我能独立解释什么：`graythresh`、`histeq`、`adapthisteq` 分别在做什么
- 我能独立做什么：根据模板跑实验并整理出报告框架

## Next Improvement

- 用真实运行结果把结论从“概念理解”推进到“现象分析”。

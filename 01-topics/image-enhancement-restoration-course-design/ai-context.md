# AI Context

## Current Stage

- 阶段：MATLAB 实现已完成，报告草稿已形成。
- 任务：课程设计 3，图像增强与复原算法综合应用。
- 默认假设：学号尾数为偶数，因此复原报告正文选择图 1；脚本同时输出图 2 备选结果。

## Inputs

- `input-images/dogDistorted.bmp`
- `input-images/dogOriginal.bmp`
- `input-images/restoration_fig1_forest_blur.jpg`
- `input-images/restoration_fig2_waterfall_dark.jpg`
- `input-images/course-design3-task.pdf`

## Code

- MATLAB 主脚本：`matlab/run_course_design3.m`
- 输出目录：`03-outputs/image-enhancement-restoration-course-design/`

## Key Results

- 增强任务：MSE `0.071191 -> 0.006541`
- 增强任务：SNR `4.664 dB -> 15.032 dB`
- 增强任务：PSNR `11.476 dB -> 21.844 dB`
- 增强任务：SSIM `0.1039 -> 0.6558`
- 图1复原：平均梯度 `0.03489 -> 0.11841`
- 图1复原：拉普拉斯方差 `0.01926 -> 0.12463`

## Gaps

- 需要用户补充真实姓名、学号、班级。
- 如果学号尾数不是偶数，需要把报告正文切换到图 2。
- 最终提交前最好人工打开输出图检查视觉效果。

# 导学页

## 先看哪里

1. 先看 [README.md](/D:/moniC/project/learn/01-topics/mean-filter-image-smoothing/README.md) 了解这个主题里有哪些交付物。
2. 再看 [report-draft.md](/D:/moniC/project/learn/01-topics/mean-filter-image-smoothing/report-draft.md) 快速熟悉报告正文。
3. 如果要重跑实验，直接运行 [run_lab.py](/D:/moniC/project/learn/01-topics/mean-filter-image-smoothing/run_lab.py)；如果你要交 Matlab 程序，可参考 [mean_filter_lab.m](/D:/moniC/project/learn/01-topics/mean-filter-image-smoothing/mean_filter_lab.m)。

## 这次实验最重要的结论

- 双边滤波在这组电路板图像上比普通高斯滤波更能兼顾去噪和保边。
- 高斯噪声较强时，高斯均值滤波和加权均值滤波优于算术均值滤波。
- 模板不是越大越好，这组图像在 3x3 或 5x5 模板下更稳。
- 逆谐波均值滤波的 `Q` 必须和噪声类型匹配：正 `Q` 对椒噪声更合适，负 `Q` 对盐噪声更合适。

## 要改什么最常见

- 如果你要改姓名、学号、班级，改 [build_report.py](/D:/moniC/project/learn/01-topics/mean-filter-image-smoothing/build_report.py) 里的封面信息后重新生成报告。
- 如果你要换实验图目录，改 [run_lab.py](/D:/moniC/project/learn/01-topics/mean-filter-image-smoothing/run_lab.py) 顶部的路径常量。

# Matlab 脚本目录

当前目录包含实验 6 的 Matlab 主脚本：

- `run_lab6_image_restoration.m`

运行方式：

```powershell
& 'D:\matlab\bin\matlab.exe' -batch "addpath('D:/moniC/project/learn/01-topics/image-restoration-lab6/matlab'); run_lab6_image_restoration"
```

脚本会自动：

- 读取 `input-images/` 中的 6 张实验图片
- 生成任务 1 到任务 4 的结果图
- 输出 `summary.json`、`metrics.csv` 和 `report-notes.txt`

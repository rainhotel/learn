# Kafka 实验证据目录

## 规则

- `runs/` 由实验脚本按时间创建，不手工伪造运行输出。
- 每次运行保留 Topic、Consumer Group、参数和原始 CLI 输出。
- 只有脚本退出码为 0 且输出 `*_EXPERIMENT_PASSED`，对应实验才能标记通过。
- Docker 未运行、镜像未下载、命令超时或断言失败，都必须保留为失败证据。

## 当前状态

- 静态环境验收：等待运行 `scripts/verify-lab.ps1 -StaticOnly`。
- Kafka 运行态验收：Pending。
- 分区内顺序实验：Pending。
- offset 与 lag 实验：Pending。

## 运行命令

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-lab.ps1 -StaticOnly
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-lab.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-lab.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\experiments\01-partition-order.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\experiments\02-offset-and-lag.ps1
```

命令默认从 `lab/` 目录执行。停止环境：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\stop-lab.ps1
```

只有明确要删除 Kafka 实验数据时才使用 `-RemoveData`。


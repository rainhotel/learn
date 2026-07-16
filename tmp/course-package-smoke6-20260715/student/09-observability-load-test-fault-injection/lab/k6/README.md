# k6 开放/封闭负载与分阶段恢复脚本

## 当前状态

- 脚本状态：已准备，Node.js 语法检查通过
- k6 运行状态：Pending，本机未安装 `k6`
- 真实运行前必须记录：k6 版本、BASE_URL、服务提交版本、数据库/Redis/Kafka 配置、负载参数和 threshold 输出

这些脚本不把 `taskId`、`userId`、`traceId` 放入 k6 metric tag；它们只存在于请求体或响应检查中。

## 文件

- `open-load.js`：ramping-arrival-rate，固定目标到达率，观察 dropped iteration 和长尾。
- `closed-load.js`：constant-vus，模拟客户端等待上一次请求完成后再发下一次。
- `recovery-stages.js`：五个顺序执行的 constant-arrival-rate 场景，在 1%、5%、20%、50%、100% 各档稳定运行。

`recovery-stages.js` 只允许调用本地/隔离实验环境的 `/internal/lab/recovery/drain-one`。该接口每次最多领取并处理 1 条 mock backlog，必须具备租户隔离、幂等、审计、实验模式鉴权和停止开关；生产环境不得公开该端点。

## 运行

```powershell
k6 version
k6 run --summary-export=tmp\k6-open-summary.json .\01-topics\java-backend-big-tech-preparation\course\09-observability-load-test-fault-injection\lab\k6\open-load.js
k6 run --summary-export=tmp\k6-closed-summary.json .\01-topics\java-backend-big-tech-preparation\course\09-observability-load-test-fault-injection\lab\k6\closed-load.js
k6 run --summary-export=tmp\k6-recovery-summary.json .\01-topics\java-backend-big-tech-preparation\course\09-observability-load-test-fault-injection\lab\k6\recovery-stages.js
```

常用环境变量：

```powershell
$env:BASE_URL='http://127.0.0.1:8080'
$env:NOTIFY_PATH='/api/notifications'
$env:RATE='50'
$env:PREALLOCATED_VUS='20'
$env:MAX_VUS='200'
$env:RECOVERY_BASE_RATE='100'
```

## 证据要求

- open 与 closed 使用相同服务版本、数据集和时间窗口。
- 保存完整 stdout、summary JSON、threshold 退出码、服务日志、Metrics/Trace 时间线和数据库正确性查询。
- open 报告必须包含 offered rate、accepted rate、dropped iterations、P95/P99、错误率和 saturation。
- recovery 报告必须包含 backlog、消费速率、供应商错误率、P99、数据正确性和每个阶段的停止条件。
- 每个恢复档位的错误率、P99 或 dropped iteration threshold 失败时应中止整场实验，不能继续自动放量。
- 未运行 k6 前不得填写真实 QPS、P99 或容量结论。

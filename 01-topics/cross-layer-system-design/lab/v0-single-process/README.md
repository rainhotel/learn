# V0 Single-Process Inventory Service

## Purpose

这是贯穿项目的最小可测量版本，用于建立 Phase 0 基线。

设计刻意保持简单：

- JDK 21，零外部依赖。
- 单进程、单请求工作线程。
- 内存状态，不引入数据库、Redis 或消息队列。
- 内置不变量检查、结构化日志和基础指标。
- 自带并发负载生成器。

V0 的目标不是展示高性能，而是建立后续版本的可比较基线。

## Invariants

- 可用库存不小于零。
- `initialStock = availableStock + allocatedStock`。
- 同一个 Request ID 和相同数量只产生一次业务效果。
- 同一个 Request ID 携带不同数量时返回冲突。
- 每个成功订单数量必须为正。

## API

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/health` | 存活检查 |
| GET | `/inventory` | 当前库存与订单摘要 |
| POST | `/orders?requestId=...&quantity=1` | 创建订单 |
| GET | `/orders?requestId=...` | 查询请求结果 |
| GET | `/invariants` | 检查库存守恒和非负约束 |
| GET | `/metrics` | 请求、订单和队列指标 |
| POST | `/reset?stock=100000` | 重置实验状态 |

## Build And Self-Test

```powershell
.\build.ps1
java.exe -cp .\build dev.learn.systemdesign.v0.InventoryServer --self-test
```

## Start Server

```powershell
.\start-server.ps1 -Port 8080 -Stock 100000 -ServiceTimeMs 5
```

`ServiceTimeMs` 用来模拟稳定的业务服务时间，便于观察排队。另一终端执行：

```powershell
Invoke-WebRequest -UseBasicParsing -Method Post `
  -Uri "http://127.0.0.1:8080/orders?requestId=demo-1&quantity=1"
```

## Run Baseline

```powershell
.\run-baseline.ps1 -Requests 1000 -Concurrency 1,8,32 -ServiceTimeMs 5
```

结果写入 `results/baseline-c*.json`。

预期现象：

- 服务只有一个工作线程，吞吐受模拟服务时间限制。
- 并发增加不会持续增加吞吐。
- 并发越高，排队越长，P95/P99 越明显上升。
- 所有成功请求完成后，不变量仍应成立。

## What V0 Does Not Prove

- 没有测试多线程数据竞争。
- 没有持久化和崩溃恢复能力。
- 单进程内的 Request ID 去重不能跨实例工作。
- 无界工作队列尚未提供过载保护。
- 客户端测得的延迟包含本机 HTTP 和调度开销，不等于生产延迟。

这些限制会分别在 V1、V3、V5 和 V7 中处理。


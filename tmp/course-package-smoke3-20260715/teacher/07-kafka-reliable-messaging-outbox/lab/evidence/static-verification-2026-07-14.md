# Kafka Lab 静态验证记录

## 环境

- 日期：2026-07-14
- 操作系统：Windows
- Docker CLI：28.3.0
- Kafka 镜像：`apache/kafka:4.3.1`
- PowerShell：Windows PowerShell 5.1 兼容语法

## 官方版本证据

Apache Kafka 4.3 Quickstart 在 2026-07-14 显示最新 patch 版本为 4.3.1，并给出：

```text
docker pull apache/kafka:4.3.1
docker run -p 9092:9092 apache/kafka:4.3.1
```

来源：<https://kafka.apache.org/43/getting-started/quickstart/>

## TDD RED

首次运行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-lab.ps1 -StaticOnly
```

得到预期失败：

```text
STATIC_CHECKS_FAILED
- missing file: compose.yaml
- missing file: config\producer.properties
- missing file: config\consumer.properties
- missing file: scripts\start-lab.ps1
- missing file: scripts\stop-lab.ps1
- missing file: scripts\kafka-command.ps1
- missing file: experiments\01-partition-order.ps1
- missing file: experiments\02-offset-and-lag.ps1
- missing file: evidence\README.md
```

这证明验收脚本能发现实验包尚未实现。

## TDD GREEN

补齐 Compose、配置和脚本后重新运行：

```text
STATIC_CHECKS_PASSED
```

额外验证：

```text
COMPOSE_CONFIG_EXIT=0
POWERSHELL_PARSE_PASSED FILES=6
```

静态验证覆盖：

- Kafka 镜像固定为 4.3.1。
- 端口为 9092。
- Producer 使用 `acks=all` 和幂等生产。
- Consumer 关闭自动提交。
- Compose 可以被 Docker CLI 解析。
- 六个 PowerShell 脚本无语法错误。

## Windows Docker 配置回归

首次运行态验证时，Docker CLI 读取用户级 `C:\Users\rainhotle\.docker\config.json` 被拒绝，PowerShell 5.1 将 stderr 警告升级为脚本终止。

修复方式：

- 在实验目录提供隔离的 `.docker/config.json`。
- 所有脚本设置 `DOCKER_CONFIG` 指向实验目录。
- `verify-lab.ps1` 用受控探针捕获 Docker 原生命令的退出码和输出。

修复后，静态检查继续通过，运行态失败能够被正确分类。

## 当前运行态结果

```text
STATIC_CHECKS_PASSED
RUNTIME_CHECKS_FAILED
- Docker Engine is not reachable.
error during connect: ... open //./pipe/docker_engine: The system cannot find the file specified.
```

结论：

- 实验包静态结构已验证。
- Docker Engine 当前未运行。
- Kafka 容器、Topic、顺序和 lag 实验没有运行，不能标记为通过。


# Alpha 环境快照

## 1. 快照性质

采集时间：`2026-07-15T13:21:34+08:00`。

这是一份当前作者工作站的观察快照，不是完整的可复现环境锁。仓库在采集时存在未提交修改和未跟踪课程文件，因此 Git 提交 `2a0da6c` 不能单独标识本证据包的全部内容。

## 2. 当前可观察环境

| 组件 | 观察值 | 当前证明范围 |
|---|---|---|
| 操作系统 | Windows 11，版本 `10.0`，`amd64` | Maven 环境信息报告的宿主平台 |
| 时区 | Asia/Shanghai，UTC+08:00 | 本次证据日期解释 |
| PowerShell | `7.6.0` | 审计脚本执行环境 |
| Git | `2.47.1.windows.1` | 版本控制客户端存在 |
| 当前分支/HEAD | `main` / `2a0da6c` | 仅标识已提交基线，不包含当前脏工作区全部内容 |
| Java Runtime | Oracle JDK `21.0.6+8-LTS-188` | Java 21 本机可用 |
| javac | `21.0.6` | Java 源码可编译工具存在 |
| Maven | `3.9.9` | Maven 客户端存在；不证明第 05/09 章依赖已成功解析或测试已通过 |
| Node.js | `v22.17.0` | k6 JavaScript 的 Node 静态语法检查环境 |
| Python | `3.13.5` | Python 客户端存在；本次未以它证明课程实验 |
| MySQL client | `8.0.40` Community Server GPL client | 客户端版本；第 04 章既有记录另称 MySQL 8.0.40 实验曾运行，本次未重连数据库复验 |
| Docker client | `28.3.0`，API `1.51` | 仅客户端可执行 |
| kubectl client | `v1.32.2`，Kustomize `v5.5.0` | 仅客户端可执行；未证明集群存在 |

## 3. 当前不可用或未完成验证的组件

| 组件 | 本次观察 | 影响 |
|---|---|---|
| Docker daemon | 连接失败，`docker_engine` pipe 不存在；同时出现用户 Docker config 读取权限警告 | Redis、Kafka、Compose、容器和 Kubernetes 运行实验不能据此验证 |
| Redis CLI/runtime | `redis-cli` 命令未找到，未连接 Redis 服务 | 第 06 章保持 Pending |
| Kafka CLI/runtime | `kafka-topics` 命令未找到，未连接 broker | 第 07 章只有静态证据 |
| k6 | 命令未找到 | 第 09 章开放负载和 threshold 仍为 Pending |
| kind | 命令未找到 | 无本地 kind 集群证据 |
| minikube | 命令未找到 | 无本地 minikube 集群证据 |
| Kubernetes server | 本次未连接任何 server | 第 12/19 章集群实验保持 Pending |
| LLM/Embedding 服务 | 未固定模型、版本、硬件和服务端点 | 第 14-18 章模型运行与质量结论保持 Pending |
| 向量数据库 | 未固定并运行 pgvector 或 Milvus 环境 | 第 16-17 章检索与评测保持 Pending |

## 4. 复现缺口

进入 Release Candidate 前，必须把本快照升级为机器可执行的环境合同，至少固定 JDK、Maven、数据库、中间件、Docker、Kubernetes、模型和向量库版本，并在干净环境保存安装检查、启动命令、退出码、原始输出与清理步骤。

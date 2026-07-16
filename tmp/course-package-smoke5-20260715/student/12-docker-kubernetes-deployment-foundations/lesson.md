# 第 12 章讲义：从 Java 进程到可恢复部署

## 学习目标

本章不把 Docker/Kubernetes 当作命令清单，而是回答：一个 NotifyFlow Java 进程如何被可靠地构建、启动、发现、探活、限制、发布和恢复。

## 一、Docker 的真实边界

Docker 容器通常共享宿主机内核，通过 namespace 隔离进程、网络、挂载和用户视图，通过 cgroup 限制 CPU、内存和进程等资源。容器不是拥有独立内核的虚拟机。

工程含义：

- 容器内 PID 1 要正确接收信号并回收子进程。
- JVM 看到的 CPU/内存限制可能影响 GC、线程池和容器退出。
- 隔离不等于安全；内核、权限、挂载和 capability 仍需审查。

## 二、镜像、层和构建上下文

镜像由只读层和元数据组成，运行时再叠加 writable layer。Dockerfile 的每条指令可能形成缓存层；把变化频繁的源码复制在依赖下载前，会降低缓存命中。

推荐 Java 多阶段构建：

```dockerfile
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY pom.xml .
COPY src src
RUN ./mvnw -DskipTests package

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 notifyflow
WORKDIR /app
COPY --from=build /workspace/target/app.jar /app/app.jar
USER 10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

课程示意不能直接当成发布镜像：必须固定 digest、扫描漏洞、验证证书、非 root、只读文件系统和启动参数。

## 三、namespace、cgroup、overlayfs

### 3.1 namespace

PID namespace 让容器看到独立进程树；network namespace 提供独立网络设备和端口视图；mount namespace 隔离挂载；user namespace 可映射容器用户。它们提供视图隔离，不自动提供完整安全边界。

### 3.2 cgroup

cgroup 控制资源使用。内存 limit 触发 OOM kill 的对象可能是容器进程，应用只看到突然退出；CPU limit 会造成 throttling，使线程池、GC 和 P99 变化。必须同时看容器事件、JVM 指标和业务 SLI。

### 3.3 overlay filesystem

镜像层共享只读内容，容器写入进入上层。大量临时文件、日志和数据库写入 writable layer 会导致空间和性能问题；状态数据应使用明确的 volume 或外部数据库。

## 四、镜像工程化

### 4.1 可复现

- 固定基础镜像 digest、JDK patch、依赖 lock 和构建工具。
- `.dockerignore` 排除 `.git`、构建产物、密钥和本地缓存。
- 构建上下文中不放 `.env`、私钥和生产配置。

### 4.2 安全

- 非 root 用户运行。
- 去掉不需要的 capability，尽量只读 root filesystem。
- 不在镜像中写 Secret；运行时注入并限制读取范围。
- 扫描 CVE 后区分 base image、依赖和业务代码责任。

### 4.3 启停

应用必须响应 SIGTERM，先停止接收新任务，再等待有限时间完成正在执行的任务，最后关闭连接池和消费者。`docker stop` 的宽限期不能代替应用自己的 graceful shutdown。

## 五、Compose 复现 NotifyFlow

Compose 适合本地多服务复现，不等同于生产编排。一个最小环境包含：

```text
notifyflow-app
mysql
redis
kafka
```

每个服务要有固定镜像版本、网络、端口、环境变量、健康检查和持久化边界。`depends_on` 的启动顺序不能证明服务已经可用；应用仍要重试连接并通过 healthcheck 表达真实状态。

## 六、Kubernetes 对象模型

### 6.1 Pod

Pod 是调度和网络的最小单元；容器共享网络命名空间和 volume。Pod 是易失的，不应把本地文件当作可靠数据库。

### 6.2 Deployment

Deployment 管理 ReplicaSet 和滚动更新。发布时要设定 maxUnavailable/maxSurge、版本标签和回滚策略；新 Pod `Running` 不代表已经能接收流量。

### 6.3 Service 与 DNS

Service 为一组就绪 Endpoint 提供稳定虚拟地址。请求失败时依次检查 Service selector、EndpointSlice、端口名称、Pod readiness、NetworkPolicy 和应用监听地址。`127.0.0.1` 只指向当前 Pod。

### 6.4 ConfigMap 与 Secret

ConfigMap 适合非敏感配置；Secret 只是 Kubernetes API 中的敏感对象，不等于自动加密、轮换或防止日志泄露。应用应限制 RBAC 和读取范围。

## 七、探针：startup、readiness、liveness

- startup probe：启动慢时保护应用，成功前不执行其他探针。
- readiness probe：决定是否接收流量；依赖暂时不可用时可以不 ready，但不应因此无限重启。
- liveness probe：判断进程是否失去自我恢复能力；过于严格会制造重启风暴。

探针必须轻量、超时有界、与业务依赖分层。不要把“供应商暂时 503”直接写成 liveness 失败。

## 八、资源与扩缩容

requests 影响调度和 HPA 基线，limits 是运行上限。内存 limit 不是 JVM heap 上限，还要给线程栈、Metaspace、Direct Memory 和 native 开销留余量。

HPA 不能只根据 CPU 扩容通知消费者；应结合 backlog、lag、处理吞吐、下游配额、P99 和错误率。扩容消费者可能放大 Provider 负载，必须有速率和恢复预算。

## 九、发布、回滚和故障

滚动发布顺序：构建并扫描镜像 -> 部署新版本 -> startup/readiness 通过 -> 小流量验证 -> 观察 SLI/错误/lag -> 扩大流量。失败时回滚 Deployment，但要先判断数据库 schema、消息格式和状态迁移是否兼容。

常见故障：

| 症状 | 排查顺序 |
|---|---|
| Pod CrashLoopBackOff | `describe`、上一次日志、配置、退出码、OOM |
| Pod Running 但无流量 | readiness、Service selector、EndpointSlice、端口 |
| ImagePullBackOff | registry、tag/digest、Secret、网络 |
| 节点 OOM | requests/limits、Pod 排名、JVM heap 外内存 |
| Kafka lag 上升 | consumer readiness、分区、处理耗时、下游限流 |

## 十、NotifyFlow 部署合同

每个环境必须明确：

- 镜像版本和回滚版本。
- MySQL/Redis/Kafka 地址、超时、认证和 Secret 来源。
- 任务消费者并发、最大批量、重试预算和暂停开关。
- health/readiness 语义、优雅终止时间和数据正确性检查。
- 指标、日志、Trace、事件和告警的关联字段。

## 十一、Agent 边界

Agent 可以读取脱敏 Pod 状态、事件、日志、Metrics、Trace 和 Runbook，生成带引用的假设和只读命令建议。它不能直接删除 Pod、修改 Deployment、扩容消费者、读取 Secret 或执行回滚。所有动作走确定性控制面、RBAC、审批、审计和 kill switch。

## 十二、章节实验全部 Pending

真实实验要验证：非 root 镜像、镜像层缓存、Compose 健康、Kubernetes 探针、滚动回滚、资源/OOM、Service/DNS、节点故障和数据正确性。未运行前不能写“镜像已加固”“部署零停机”或容量数字。

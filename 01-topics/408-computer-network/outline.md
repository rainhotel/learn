# 408 计算机网络 Mastery Outline

## Exam Frame

- 模块定位：408 四门之一
- 近年常见分值：约 25 分
- 常见题型：8 道选择题 + 1 道综合题
- 复习目标：选择题高正确率 + 综合题稳定拿分

## Phase 0: Build The Exam Map

### Goal

- 先明确 408 计算机网络到底考什么，不考什么。

### Focus

- 五层体系结构总览
- 各层核心协议与功能
- 408 高频章节优先级
- 常见题型分布

### Output

- 能默写全书框架
- 能说出每层的代表协议、核心任务、常见题型

### Exit Criteria

- 能在 10 分钟内画出完整知识地图
- 能说出最该优先攻克的三章

## Phase 1: Network Architecture And Performance Metrics

### Goal

- 拿下体系结构、分层思想、性能指标这类基础选择题。

### Must Know

- 网络、互联网、互连网概念区别
- 资源子网/通信子网
- 电路交换、报文交换、分组交换
- 时延、发送时延、传播时延、往返时延、吞吐量、带宽
- 协议、服务、接口、分层思想
- ISO/OSI 与 TCP/IP 体系

### Practice

- 做概念辨析题
- 做时延/吞吐量小计算题

### High-Frequency Mistakes

- 混淆发送时延和传播时延
- 混淆协议与服务
- 混淆 OSI 与 TCP/IP 分层对应

## Phase 2: Physical Layer

### Goal

- 把物理层控制在“会选、少丢分”。

### Must Know

- 奈奎斯特定理、香农定理
- 码元、波特、比特率
- 基带传输、宽带传输
- 双绞线、同轴电缆、光纤、无线介质特点
- 电路交换与复用技术

### Practice

- 信道极限相关计算
- 介质/编码方式选择题

### High-Frequency Mistakes

- 奈奎斯特与香农适用场景混淆
- 比特率、波特率换算出错

## Phase 3: Data Link Layer

### Goal

- 这是综合题高频层，必须重点突破。

### Must Know

- 封装成帧、透明传输、差错检测
- CRC 计算
- 停止-等待、GBN、SR
- 信道划分介质访问控制
- CSMA/CD 工作过程与最小帧长
- 以太网、MAC 地址、交换机自学习
- PPP 协议

### Practice

- CRC 计算题
- 发送窗口/确认序号题
- 以太网最小帧长、争用期相关题
- 综合题中的链路层部分

### High-Frequency Mistakes

- GBN 和 SR 窗口范围混淆
- CRC 除法步骤不规范
- CSMA/CD 时序和最短帧长关系没吃透

## Phase 4: Network Layer

### Goal

- 这是 408 网络最核心章节，选择题和综合题都要强。

### Must Know

- IPv4 地址分类与 CIDR
- 子网划分与子网掩码
- 路由聚合
- ARP、DHCP、ICMP、NAT
- IP 数据报格式
- 分片与重组
- RIP、OSPF、BGP 基本思想
- IPv6 核心特点

### Practice

- 子网划分题
- 最长前缀匹配题
- 分片题
- 路由表转发表分析题
- 综合题中的网络层部分

### High-Frequency Mistakes

- 子网位数和主机位数算错
- 广播地址、网络地址判断错
- 分片偏移量不是 8 字节整数倍
- 路由聚合边界判断错

## Phase 5: Transport Layer

### Goal

- TCP/UDP 是综合题常客，要做到“能解释 + 能计算 + 能画过程”。

### Must Know

- 端口、复用与分用
- UDP 特点和适用场景
- TCP 报文段首部
- 三次握手、四次挥手
- 可靠传输机制
- 流量控制、滑动窗口
- 拥塞控制：慢开始、拥塞避免、快重传、快恢复

### Practice

- 握手挥手状态判断题
- 序号确认号窗口计算题
- 拥塞窗口变化过程题
- 综合题中的 TCP 部分

### High-Frequency Mistakes

- ACK、seq、确认号含义混淆
- 流量控制和拥塞控制混淆
- 握手/挥手状态切换漏步

## Phase 6: Application Layer

### Goal

- 这一层以记忆与协议比较为主，目标是选择题尽量全对。

### Must Know

- DNS 查询过程
- FTP 主动/被动模式
- SMTP、POP3、IMAP 基本作用
- HTTP 特点、请求响应结构、持久连接
- DHCP 工作过程
- 常见协议端口

### Practice

- 协议功能匹配题
- 端口号与应用场景题
- 请求过程排序题

### High-Frequency Mistakes

- 协议功能混淆
- 端口号记忆错误
- DNS 递归/迭代查询分不清

## Phase 7: Integrated Problem Solving

### Goal

- 把单章知识转成 408 真题作答能力。

### Practice Pack

- 专练网络层综合题
- 专练 TCP 综合题
- 专练数据链路层计算题
- 做近年真题第 47 题风格题

### Answering Standard

- 先写结论
- 再写依据
- 涉及计算要写公式和单位
- 涉及协议过程要写关键状态变化

## Phase 8: Sprint And Memory Consolidation

### Goal

- 冲刺阶段用最小时间维持得分点稳定。

### Focus

- 背高频协议对比表
- 背高频公式
- 回刷错题
- 限时训练综合题

### Final Checklist

- 体系结构图能默写
- IP 编址/子网划分/分片能稳算
- TCP 三次握手四次挥手能稳讲
- CSMA/CD 与滑动窗口相关题不再混
- 常见应用层协议与端口能快速匹配

## Suggested 4-Week Targeted Plan

### Week 1

- 体系结构
- 物理层
- 数据链路层前半

### Week 2

- 数据链路层后半
- 网络层前半
- 子网划分和分片专项

### Week 3

- 网络层后半
- 运输层
- TCP 综合题专项

### Week 4

- 应用层
- 全章节串联
- 真题与错题复盘

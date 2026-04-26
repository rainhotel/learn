# 408 计算机网络 Solved Problems

## Problem Template

### Problem Title

- Date:
- Source:
- Topic:
- Difficulty:

#### Problem

- 题目原文：

#### Final Answer

- 最终答案：

#### Solution

1. 
2. 
3. 

#### Formula Or Method Used

- 公式/定理/方法：
- 含义：
- 适用条件：

#### Sources

- 参考来源 1：
- 参考来源 2：

#### Knowledge Points

- 这道题对应的知识点：
- 这道题暴露出的薄弱点：

#### Reflection

- 这次为什么会做对/做错：
- 以后怎么更快识别这类题：

## Starter Problem Types

- 子网划分题
- 路由聚合题
- IP 分片题
- TCP 握手挥手题
- TCP 窗口与拥塞控制题
- CRC 计算题
- CSMA/CD 时序题

## Window And Data Link Layer Problem Set (2026-03-22)

### Problem 1

- Date: 2026-03-22
- Source: 用户提供截图
- Topic: GBN 重传
- Difficulty: Easy

#### Problem

- 数据链路层采用后退 N 帧协议（GBN）发送数据，已发送编号 0~7。计时器超时时，发送方只收到 0、2、3 号帧确认，问需重传多少帧。

#### Final Answer

- 4 帧。

#### Solution

1. GBN 采用累计确认。
2. 收到对 3 号帧的确认，等价于 0~3 号帧都已确认。
3. 当前最早未确认帧是 4，因此超时后要重传 4、5、6、7。

#### Formula Or Method Used

- 方法：累计确认 + 从最早未确认帧开始整段重传
- 含义：GBN 的典型判断模板
- 适用条件：GBN 协议题

#### Knowledge Points

- 这道题对应的知识点：GBN 累计确认
- 这道题暴露出的薄弱点：容易把“收到 3 号确认”和“只确认 3 号”混淆

#### Reflection

- 以后怎么更快识别这类题：先看是不是 GBN，再找最早未确认帧

### Problem 2

- Date: 2026-03-22
- Source: 用户提供截图
- Topic: SR 重传
- Difficulty: Easy

#### Problem

- 数据链路层采用选择重传协议（SR），已发送 0~3 号帧，只收到 1 号帧确认，而 0、2 号帧依次超时，问需重传多少帧。

#### Final Answer

- 2 帧。

#### Solution

1. SR 对每一帧单独确认。
2. 哪一帧超时，就只重传哪一帧。
3. 已知超时的是 0 和 2，因此只重传 0、2。

#### Formula Or Method Used

- 方法：单独确认 + 选择重传
- 含义：SR 的典型判断模板
- 适用条件：SR 协议题

#### Knowledge Points

- 这道题对应的知识点：SR 的单帧确认与单帧重传
- 这道题暴露出的薄弱点：容易把 SR 误当成 GBN

#### Reflection

- 以后怎么更快识别这类题：先判断确认是否累计

### Problem 3

- Date: 2026-03-22
- Source: 用户提供截图
- Topic: GBN 利用率与最小序号位数
- Difficulty: Medium

#### Problem

- GBN，16 kbps，单向传播时延 270 ms，数据帧 512 B，确认帧与数据帧等长。为使信道利用率最高，帧序号位数至少多少。

#### Final Answer

- 3 位。

#### Solution

1. 数据帧发送时延 `Td = 512×8 / 16000 = 0.256 s`。
2. 确认帧等长，所以 `Tack = 0.256 s`。
3. 第一帧确认返回时间 `= Td + 2Tp + Tack = 0.256 + 0.54 + 0.256 = 1.052 s`。
4. 要满利用率，窗口至少满足 `W × 0.256 >= 1.052`，得 `W >= 4.109375`，所以 `Wmin = 5`。
5. GBN 发送窗口最大值 `2^k - 1 >= 5`，得 `k = 3`。

#### Formula Or Method Used

- 公式：`W * Td >= Td + 2Tp + Tack`
- 公式：`W_T <= 2^k - 1`
- 适用条件：GBN 利用率与序号位数题

#### Knowledge Points

- 这道题对应的知识点：满利用率窗口、GBN 窗口上界
- 这道题暴露出的薄弱点：容易漏掉 ACK 发送时延

#### Reflection

- 以后怎么更快识别这类题：先算一帧发送时延，再算确认返回周期

### Problem 4

- Date: 2026-03-22
- Source: 用户提供截图
- Topic: 介质访问控制
- Difficulty: Easy

#### Problem

- 下列介质访问控制方法中，可能发生冲突的是：CDMA、CSMA、TDMA、FDMA。

#### Final Answer

- CSMA。

#### Solution

1. TDMA、FDMA、CDMA 都属于信道划分方法。
2. 信道划分是预先分配资源，原则上不发生冲突。
3. CSMA 属于随机访问方法，多个站点可能同时侦听并发送，因此可能冲突。

#### Formula Or Method Used

- 方法：介质访问控制分类
- 适用条件：随机访问 vs 信道划分题

#### Knowledge Points

- 这道题对应的知识点：介质访问控制分类
- 这道题暴露出的薄弱点：容易把 CSMA 当成信道划分法

#### Reflection

- 以后怎么更快识别这类题：先问它是“分配资源”还是“抢占信道”

### Problem 5

- Date: 2026-03-22
- Source: 用户提供截图
- Topic: HDLC 零比特填充
- Difficulty: Medium

#### Problem

- HDLC 对 `01111100 01111110` 组帧后，对应的比特串为何。

#### Final Answer

- `01111100 01111101 0`。

#### Solution

1. HDLC 规则：数据字段中每出现连续 5 个 1，就插入一个 0。
2. 第一段 `01111100` 中，`11111` 后插入 `0`，得到 `011111000`。
3. 第二段 `01111110` 中，同样在 `11111` 后插入 `0`，得到 `011111010`。
4. 合并后得到 `01111100011111010`，对应选项 C 的分组写法。

#### Formula Or Method Used

- 方法：零比特填充
- 适用条件：HDLC 组帧/拆帧题

#### Knowledge Points

- 这道题对应的知识点：HDLC 的比特填充规则
- 这道题暴露出的薄弱点：容易把标志字段或全部比特都拿去填充

#### Reflection

- 以后怎么更快识别这类题：只盯“连续 5 个 1 后插 0”这一条规则

### Problem 6

- Date: 2026-03-22
- Source: 用户提供截图
- Topic: GBN 平均数据传输速率
- Difficulty: Medium

#### Problem

- GBN，发送窗口 1000，数据帧 1000 B，带宽 100 Mbps，ACK 为短帧可忽略发送时延，单向传播时延 50 ms，求可达到的最大平均数据传输速率。

#### Final Answer

- 约 80 Mbps。

#### Solution

1. 数据帧发送时延 `Td = 1000×8 / 100000000 = 80 μs = 0.08 ms`。
2. 发送窗口为 1000，所以连续发送完整个窗口需要 `1000 × 0.08 = 80 ms`。
3. 第一帧确认返回时间近似 `Td + 2Tp = 0.08 + 100 = 100.08 ms`。
4. 利用率 `U = 80 / 100.08 ≈ 0.799`。
5. 平均传输速率 `= 100 Mbps × 0.799 ≈ 79.9 Mbps`，约为 `80 Mbps`。

#### Formula Or Method Used

- 公式：`吞吐率 = 发送速率 × 利用率`
- 思路：窗口发送总时长 / 第一帧确认返回周期
- 适用条件：ACK 很短可忽略其发送时延的 GBN 利用率题

#### Knowledge Points

- 这道题对应的知识点：窗口协议平均传输速率
- 这道题暴露出的薄弱点：容易把窗口总发送时间和 RTT 关系想错

#### Reflection

- 以后怎么更快识别这类题：先算窗口内帧全部发完要多久，再看 ACK 回来是否赶得上


## Problem Set 7-9 (2026-03-28)

### Problem 7

- Date: 2026-03-28
- Source: 用户提供截图
- Topic: CDMA 解码
- Difficulty: Medium

#### Problem

- 站点 A、B、C 通过 CDMA 共享链路，A、B、C 的码片序列分别为 `(1,1,1,1)`、`(1,-1,1,-1)`、`(1,1,-1,-1)`。若链路上收到的序列是 `(2,0,2,0,0,-2,0,-2,0,2,0,2)`，求 A 发送的数据。

#### Final Answer

- `101`

#### Solution

1. CDMA 中每个比特对应一组完整码片，因此先按长度 4 分段：`(2,0,2,0)`、`(0,-2,0,-2)`、`(0,2,0,2)`。
2. A 的码片序列是 `(1,1,1,1)`。
3. 第一段内积：`2+0+2+0=4`，除以 4 得 `+1`，表示发送 1。
4. 第二段内积：`0-2+0-2=-4`，除以 4 得 `-1`，表示发送 0。
5. 第三段内积：`0+2+0+2=4`，除以 4 得 `+1`，表示发送 1。
6. 所以 A 发送的数据是 `101`。

#### Formula Or Method Used

- 公式：`接收序列 · 本站码片 / 码片长度`
- 含义：CDMA 解码
- 适用条件：正交码片序列题

#### Knowledge Points

- 这道题对应的知识点：CDMA 内积解码
- 这道题暴露出的薄弱点：容易忘记先按码片长度分组

#### Reflection

- 以后怎么更快识别这类题：先分组，再做内积，再看正负

### Problem 8

- Date: 2026-03-28
- Source: 用户提供截图
- Topic: 滑动窗口利用率与序号位数
- Difficulty: Medium

#### Problem

- 128 kbps 链路，单向传播时延 250 ms，帧长 1000 字节，不考虑确认帧开销，为使链路利用率不小于 80%，帧序号比特数至少多少。

#### Final Answer

- `4`

#### Solution

1. 数据帧发送时延：`Td = 1000×8 / 128000 = 0.0625 s = 62.5 ms`。
2. 单向传播时延 `Tp = 250 ms`，忽略 ACK 发送时延，所以一个确认返回周期近似为 `Td + 2Tp = 62.5 + 500 = 562.5 ms`。
3. 若窗口为 `W`，利用率 `U = W×62.5 / 562.5 = W / 9`。
4. 要求 `U >= 0.8`，所以 `W / 9 >= 0.8`，得 `W >= 7.2`，故最小窗口 `W = 8`。
5. GBN 中发送窗口最大为 `2^k - 1`，需满足 `2^k - 1 >= 8`。
6. 解得 `k = 4`。

#### Formula Or Method Used

- 公式：`U = W * Td / (Td + 2Tp)`
- 公式：`W_T <= 2^k - 1`
- 适用条件：GBN/滑动窗口的利用率与序号位数题

#### Knowledge Points

- 这道题对应的知识点：窗口利用率、窗口下界、序号位数反推
- 这道题暴露出的薄弱点：容易把窗口大小和位数混淆

#### Reflection

- 以后怎么更快识别这类题：先算帧发送时延，再求最小窗口，最后再反推位数

### Problem 9

- Date: 2026-03-28
- Source: 用户提供截图
- Topic: 停等协议利用率
- Difficulty: Medium

#### Problem

- 停等协议，数据速率 3 kbps，单向传播时延 200 ms，忽略确认帧传输时延。当信道利用率等于 40% 时，数据帧长度为多少。

#### Final Answer

- `800 比特`

#### Solution

1. 停等协议利用率：`U = Td / (Td + 2Tp)`。
2. 题目给 `U = 0.4`，`Tp = 0.2 s`，所以：`Td / (Td + 0.4) = 0.4`。
3. 解方程：`Td = 0.4Td + 0.16`，得 `0.6Td = 0.16`，所以 `Td = 0.2667 s`。
4. 数据速率为 `3000 bit/s`，因此帧长 `L = 3000 × 0.2667 ≈ 800 bit`。

#### Formula Or Method Used

- 公式：`U = Td / (Td + 2Tp)`
- 公式：`L = R × Td`
- 适用条件：停等协议利用率反推帧长题

#### Knowledge Points

- 这道题对应的知识点：停等协议利用率与帧长反推
- 这道题暴露出的薄弱点：容易把 200 ms 误当往返时延

#### Reflection

- 以后怎么更快识别这类题：先把单向传播时延变成往返等待时间，再解利用率方程

## Problem Set 10-13 (2026-03-28)

### Problem 10

- Date: 2026-03-28
- Source: 用户提供截图
- Topic: 通用滑动窗口约束
- Difficulty: Easy

#### Problem

- 对于滑动窗口协议，如果分组序号采用 3 比特编号，发送窗口大小为 5，则接收窗口最大是多少。

#### Final Answer

- `3`

#### Solution

1. 3 比特编号说明序号空间大小为 `2^3 = 8`。
2. 通用滑动窗口约束：`W_T + W_R <= 2^n`。
3. 已知发送窗口 `W_T = 5`，所以 `W_R <= 8 - 5 = 3`。
4. 因此接收窗口最大是 `3`。

#### Formula Or Method Used

- 公式：`W_T + W_R <= 2^n`
- 含义：避免新旧帧混淆
- 适用条件：通用滑动窗口题

#### Knowledge Points

- 这道题对应的知识点：发送窗口与接收窗口的总约束
- 这道题暴露出的薄弱点：容易错把它当成 GBN 或 SR 特例

#### Reflection

- 以后怎么更快识别这类题：题目没说 GBN/SR 时，先用通用约束

### Problem 11

- Date: 2026-03-28
- Source: 用户提供截图
- Topic: GBN 累计确认与重传
- Difficulty: Easy

#### Problem

- 数据链路层采用后退 N 帧协议，已发送 0~6 号帧。超时时只收到对 1、3、5 号帧的确认，问需重传多少帧。

#### Final Answer

- `1`

#### Solution

1. GBN 使用累计确认。
2. 若已经收到对 5 号帧的确认，就表示 0~5 号帧都已确认。
3. 因此当前最早未确认帧是 6。
4. 超时后只需从 6 开始重传，即只重传 1 帧。

#### Formula Or Method Used

- 方法：累计确认 + 从最早未确认帧开始重传
- 含义：GBN 重传判断模板
- 适用条件：GBN 重传题

#### Knowledge Points

- 这道题对应的知识点：GBN 累计确认
- 这道题暴露出的薄弱点：容易把“收到了 1、3、5”误解成只确认这些帧本身

#### Reflection

- 以后怎么更快识别这类题：只看“最大的已确认帧”到哪

### Problem 12

- Date: 2026-03-28
- Source: 用户提供截图
- Topic: GBN 序号位数反推
- Difficulty: Easy

#### Problem

- 数据链路层采用 GBN 协议，若发送窗口大小为 32，则至少需要几位序号才能保证协议不出错。

#### Final Answer

- `6`

#### Solution

1. GBN 发送窗口上界：`W_T <= 2^n - 1`。
2. 已知 `W_T = 32`，则需满足 `2^n - 1 >= 32`。
3. 当 `n = 5` 时，`2^5 - 1 = 31`，不够。
4. 当 `n = 6` 时，`2^6 - 1 = 63`，满足。
5. 所以至少要 `6` 位序号。

#### Formula Or Method Used

- 公式：`W_T <= 2^n - 1`
- 含义：GBN 发送窗口上界
- 适用条件：GBN 位数反推题

#### Knowledge Points

- 这道题对应的知识点：GBN 窗口与序号位数关系
- 这道题暴露出的薄弱点：容易把 32 错看成 31 也够

#### Reflection

- 以后怎么更快识别这类题：直接试最小的 n，使 `2^n - 1` 刚好不小于窗口

### Problem 13

- Date: 2026-03-28
- Source: 用户提供截图
- Topic: GBN 最大发送窗口
- Difficulty: Easy

#### Problem

- 若采用后退 N 帧协议，帧编号字段为 7 位，则发送窗口最大长度为多少。

#### Final Answer

- `127`

#### Solution

1. GBN 发送窗口最大值公式：`W_T(max) = 2^n - 1`。
2. 题目给 `n = 7`。
3. 所以最大窗口 `= 2^7 - 1 = 128 - 1 = 127`。

#### Formula Or Method Used

- 公式：`W_T(max) = 2^n - 1`
- 含义：GBN 最大窗口
- 适用条件：已知位数求最大窗口题

#### Knowledge Points

- 这道题对应的知识点：GBN 最大发送窗口
- 这道题暴露出的薄弱点：容易误写成 `2^n`

#### Reflection

- 以后怎么更快识别这类题：只要是 GBN 最大窗口，立刻想到 `2^n - 1`

## Problem Set 14-15 (2026-03-28)

### Problem 14

- Date: 2026-03-28
- Source: 用户提供截图
- Topic: SR 最大接收窗口
- Difficulty: Easy

#### Problem

- 选择重传协议中，若采用 5 位帧序列号，则最大接收窗口是多少。

#### Final Answer

- `16`

#### Solution

1. 5 位序列号说明序号空间大小为 `2^5 = 32`。
2. SR 为避免新旧帧混淆，发送窗口和接收窗口最大都只能取序号空间的一半。
3. 因此最大接收窗口为 `32 / 2 = 16`。

#### Formula Or Method Used

- 公式：`W_R(max) = 2^(n-1)`
- 含义：SR 最大接收窗口上界
- 适用条件：选择重传协议题

#### Knowledge Points

- 这道题对应的知识点：SR 窗口上界
- 这道题暴露出的薄弱点：容易把 SR 和 GBN 的窗口公式混淆

#### Reflection

- 以后怎么更快识别这类题：只要看到“选择重传 SR”，立刻想到“一半序号空间”

### Problem 15

- Date: 2026-03-28
- Source: 用户提供截图
- Topic: 停等协议最大利用率
- Difficulty: Medium

#### Problem

- 停等协议，数据帧与确认帧均为 1000B，传输速率 10 kb/s，单项传播延时 200 ms，求最大信道利用率。

#### Final Answer

- `40%`

#### Solution

1. 数据帧发送时延：`Td = 1000×8 / 10000 = 0.8 s`。
2. 确认帧与数据帧等长，因此 `Tack = 0.8 s`。
3. 单向传播时延 `Tp = 0.2 s`，往返传播时延为 `2Tp = 0.4 s`。
4. 停等协议一个完整周期时间：`Td + 2Tp + Tack = 0.8 + 0.4 + 0.8 = 2.0 s`。
5. 利用率 `U = Td / 2.0 = 0.8 / 2.0 = 0.4 = 40%`。

#### Formula Or Method Used

- 公式：`U = Td / (Td + 2Tp + Tack)`
- 适用条件：ACK 不可忽略的停等协议利用率题

#### Knowledge Points

- 这道题对应的知识点：停等协议利用率
- 这道题暴露出的薄弱点：容易漏掉 ACK 发送时间，或把单向传播时延误当往返时延

#### Reflection

- 以后怎么更快识别这类题：先把一个完整周期拆成“发数据 + 去程传播 + 发ACK + 回程传播”

## Problem Set 1-3 (2026-03-29)

### Problem 1

- Date: 2026-03-29
- Source: 用户提供截图
- Topic: 交换机转发决策
- Difficulty: Easy

#### Problem

- 以太网交换机进行转发决策时使用的 PDU 地址是什么。

#### Final Answer

- `目的物理地址`

#### Solution

1. 交换机工作在数据链路层，处理的 PDU 是帧。
2. 帧中的物理地址就是 MAC 地址。
3. 交换机通过查询 MAC 地址表，根据帧的目的 MAC 地址决定把帧转发到哪个端口。
4. 因此它看的是目的物理地址，而不是 IP 地址，也不是源地址。

#### Formula Or Method Used

- 方法：链路层设备按目的 MAC 地址转发
- 适用条件：交换机 / 网桥题

#### Knowledge Points

- 这道题对应的知识点：交换机转发依据
- 这道题暴露出的薄弱点：容易把交换机和路由器混淆

#### Reflection

- 以后怎么更快识别这类题：先判断设备在哪一层工作

### Problem 2

- Date: 2026-03-29
- Source: 用户提供截图
- Topic: CSMA/CD 最小帧长与最大距离
- Difficulty: Medium

#### Problem

- CSMA/CD 网络中，传输速率 1Gbps，传播速度 200000 km/s。若最小帧长度减少 800 比特，则最远两站点距离至少需要怎样变化。

#### Final Answer

- `减少 80m`

#### Solution

1. CSMA/CD 要求最小帧发送时间不小于往返传播时延：`T帧 >= 2τ`。
2. 帧长减少 800 bit，速率 1Gbps，因此发送时间减少：`800 / 10^9 s = 8×10^-7 s = 0.8 μs`。
3. 由于这个减少的是“往返传播时延上限”，所以单程传播时延最多减少一半：`0.4 μs`。
4. 距离变化量 `= 传播速度 × 时间 = 200000 km/s × 0.4×10^-6 s = 0.08 km = 80 m`。
5. 因此最远距离需要减少 80m。

#### Formula Or Method Used

- 公式：`T帧 >= 2τ`
- 公式：`距离 = 速度 × 单程传播时延`
- 适用条件：CSMA/CD 最小帧长与距离题

#### Knowledge Points

- 这道题对应的知识点：冲突检测与最小帧长
- 这道题暴露出的薄弱点：容易把往返传播时间直接用于单程距离计算

#### Reflection

- 以后怎么更快识别这类题：先算发送时延变化，再除以 2 得单程传播变化

### Problem 3

- Date: 2026-03-29
- Source: 用户提供截图
- Topic: MAC 协议确认机制
- Difficulty: Easy

#### Problem

- 对正确接收到的数据帧进行确认的 MAC 协议是什么。

#### Final Answer

- `CSMA/CA`

#### Solution

1. 在无线环境中难以像 CSMA/CD 那样边发边检测冲突。
2. 因此 CSMA/CA 采用“避免冲突 + 接收方返回 ACK”的方式来确认是否正确接收。
3. 其他选项中，CSMA 与 CSMA/CD 都不以“正确接收后返回 ACK”作为核心机制。
4. 因此答案是 CSMA/CA。

#### Formula Or Method Used

- 方法：比较 MAC 协议的核心工作机制
- 适用条件：MAC 协议辨析题

#### Knowledge Points

- 这道题对应的知识点：CSMA/CA 的 ACK 机制
- 这道题暴露出的薄弱点：容易把 CSMA/CD 的碰撞检测和 CSMA/CA 的确认机制混在一起

#### Reflection

- 以后怎么更快识别这类题：记住“有线看 CD，无线看 CA”

## Problem Set 11-14 (2026-03-29)

### Problem 11

- Date: 2026-03-29
- Source: 用户提供截图
- Topic: 集线器的冲突域与广播域
- Difficulty: Easy

#### Problem

- 用集线器连接的工作站集合属于什么域。

#### Final Answer

- `同属一个冲突域，也同属一个广播域`

#### Solution

1. 集线器工作在物理层，不隔离冲突，也不隔离广播。
2. 多台主机共享同一条逻辑介质，因此属于同一个冲突域。
3. 广播信号也会被扩散到所有端口，因此同属一个广播域。

#### Formula Or Method Used

- 方法：根据设备层次判断是否隔离冲突域/广播域
- 适用条件：Hub、交换机、路由器对比题

#### Knowledge Points

- 这道题对应的知识点：Hub 的域属性
- 这道题暴露出的薄弱点：容易把 Hub 和交换机混淆

#### Reflection

- 以后怎么更快识别这类题：记住“Hub 什么都不隔离”

### Problem 12

- Date: 2026-03-29
- Source: 用户提供截图
- Topic: 集线器带宽共享
- Difficulty: Easy

#### Problem

- 5 台计算机连接到一台 10Mb/s 集线器，则每台计算机分得的平均带宽为多少。

#### Final Answer

- `2Mb/s`

#### Solution

1. 集线器下所有主机共享总带宽 10Mb/s。
2. 若平均分配给 5 台主机，则每台平均带宽为 `10 / 5 = 2 Mb/s`。

#### Formula Or Method Used

- 公式：平均带宽 = 总带宽 / 主机数
- 适用条件：共享式以太网题

#### Knowledge Points

- 这道题对应的知识点：Hub 带宽共享
- 这道题暴露出的薄弱点：容易把集线器端口误看成独享带宽

#### Reflection

- 以后怎么更快识别这类题：只要是 Hub，就想到“共享介质”

### Problem 13

- Date: 2026-03-29
- Source: 用户提供截图
- Topic: 集线器转发方式
- Difficulty: Easy

#### Problem

- 当集线器的一个端口收到数据后，将其如何处理。

#### Final Answer

- `从除输入端口外的所有端口广播出去`

#### Solution

1. 集线器不识别帧地址，不做目的端口选择。
2. 它只是把输入信号复制并发送到其他端口。
3. 因此会从除输入端口外的所有端口发出。

#### Formula Or Method Used

- 方法：物理层扩散转发
- 适用条件：Hub 工作方式题

#### Knowledge Points

- 这道题对应的知识点：Hub 不做智能转发
- 这道题暴露出的薄弱点：容易把 Hub 的行为误当交换机

#### Reflection

- 以后怎么更快识别这类题：看到 Hub，就想“不是选路，是扩散”

## Problem Set 15-18 (2026-03-29)

### Problem 15

- Date: 2026-03-29
- Source: 用户提供截图
- Topic: p-坚持 CSMA
- Difficulty: Easy

#### Problem

- 以下几种 CSMA 协议中，哪一种在监听到介质空闲时仍可能不发送。

#### Final Answer

- `p-坚持CSMA`

#### Solution

1. 1-坚持 CSMA：一旦空闲立即发送。
2. 非坚持 CSMA：再次监听到空闲后也会立即发送。
3. p-坚持 CSMA：即使空闲，也只是以概率 p 发送，否则继续等待一个时隙。
4. 所以答案是 p-坚持 CSMA。

#### Knowledge Points

- 这道题对应的知识点：CSMA 三种坚持策略
- 这道题暴露出的薄弱点：容易混淆“监听到空闲后”的行为差异

### Problem 16

- Date: 2026-03-29
- Source: 用户提供截图
- Topic: 二进制指数退避
- Difficulty: Easy

#### Problem

- 在以太网的二进制回退算法中，在 11 次碰撞之后，站点会在 0~() 之间选择一个随机数。

#### Final Answer

- `1023`

#### Solution

1. 二进制指数退避中，第 i 次冲突后取 `k = min(i, 10)`。
2. 当 i = 11 时，`k = 10`。
3. 随机范围为 `0 ~ (2^10 - 1)`。
4. 所以最大值是 `1023`。

#### Knowledge Points

- 这道题对应的知识点：二进制指数退避上限
- 这道题暴露出的薄弱点：容易继续把指数增长到 11

### Problem 17

- Date: 2026-03-29
- Source: 用户提供截图
- Topic: 网卡实现层次
- Difficulty: Easy

#### Problem

- 网卡实现的主要功能在()。

#### Final Answer

- `物理层和数据链路层`

#### Solution

1. 网卡负责比特流发送接收，对应物理层。
2. 网卡还负责成帧、MAC 地址识别、差错检测等链路层功能。
3. 因此它主要实现物理层和数据链路层。

#### Knowledge Points

- 这道题对应的知识点：网卡实现层次
- 这道题暴露出的薄弱点：容易把网卡单纯记成物理层设备

### Problem 18

- Date: 2026-03-29
- Source: 用户提供截图
- Topic: MAC 地址基本概念
- Difficulty: Easy

#### Problem

- 以下关于以太网地址的描述，错误的是()。

#### Final Answer

- `MAC地址是通过域名解析获得的`

#### Solution

1. 以太网地址就是常说的 MAC 地址，这一说法正确。
2. MAC 地址又称局域网硬件地址，这一说法正确。
3. 以太网地址通常存储在网卡中，这一说法正确。
4. 域名解析得到的是 IP 地址，不是 MAC 地址，因此该说法错误。

#### Knowledge Points

- 这道题对应的知识点：MAC 地址与 DNS/IP 的区别
- 这道题暴露出的薄弱点：容易把不同层次的地址混为一谈

## Correction Record: Problem 25 (2026-03-29)

### Problem 25

- Date: 2026-03-29
- Source: 用户截图更正
- Topic: 冲突域划分
- Difficulty: Easy

#### Problem

- 下列不能分割冲突域的设备是()。

#### Final Answer

- `集线器`

#### Solution

1. 集线器工作在物理层，只会把信号扩散到所有端口，因此所有端口仍处于同一个冲突域。
2. 网桥和交换机工作在数据链路层，可以按端口划分冲突域。
3. 路由器工作在网络层，也能分割冲突域。
4. 因此四个选项中，不能分割冲突域的只有集线器。

#### Knowledge Points

- 这道题对应的知识点：冲突域与设备层次
- 这道题暴露出的薄弱点：容易把“冲突域”和“广播域”混淆

## Problem Set: Ethernet, VLAN, Devices, Domains (2026-03-29)

### Problems 19-22

- Date: 2026-03-29
- Source: 用户提供截图
- Topic: IEEE 802、VLAN、MAC 服务、100BaseT
- Applicable Method: 分层判断 + VLAN 本质 + MAC 服务类型 + 以太网命名规则

#### Conclusions
- 19. IEEE 802 对应 `物理层和数据链路层`。
- 20. 错误说法是“虚拟局域网通过硬件方式实现逻辑分组与管理”。
- 21. 以太网 MAC 提供的是 `无连接的不可靠服务`。
- 22. 100BaseT 使用的导向传输介质是 `双绞线`。

#### Key Reasoning
- IEEE 802 同时规定物理传输与 MAC/LLC。
- VLAN 的本质是基于交换机的逻辑划分，重点是划分广播域。
- 以太网 MAC 不负责端到端可靠交付。
- `BaseT` 中的 `T` 表示 Twisted Pair。

### Problems 23-25

- Date: 2026-03-29
- Source: 用户提供截图
- Topic: 数据链路层设备、VLAN 广播域、冲突域
- Applicable Method: 设备分层 + 冲突域/广播域判断

#### Conclusions
- 23. 工作在数据链路层的是 `网桥和局域网交换机`。
- 24. 错误说法是“VLAN 可以隔离冲突域，但不能隔离广播域”。
- 25. 不能分割冲突域的设备是 `集线器`。

#### Key Reasoning
- 网桥与交换机都按 MAC 地址处理帧，所以属于数据链路层。
- VLAN 的核心作用之一就是隔离广播域。
- 集线器工作在物理层，只会扩散信号，因此不能分割冲突域。

### Problems 26-30

- Date: 2026-03-29
- Source: 用户提供截图
- Topic: Hub/Switch 的域划分、交换机部署、拓扑物理层接收
- Applicable Method: 域数量规则 + 交换机按 MAC 定向转发 + 物理层/链路层区分

#### Conclusions
- 26. 过交换机连接的一组工作站：`组成一个广播域，但不是一个冲突域`。
- 27. 16 端口集线器的冲突域和广播域分别是 `1,1`。
- 28. 16 端口交换机的冲突域和广播域分别是 `16,1`。
- 29. 交换机更适合放在 `以太网A`。
- 30. 除 H4 外，从物理层上能收到 ACK 帧的主机是 `H2、H3`。

#### Key Reasoning
- Hub 是共享介质，所以整个 Hub 下仍是一个冲突域、一个广播域。
- Switch 每端口一个冲突域，但默认整个交换网络仍是一个广播域。
- 内部通信量大的局域网更适合放交换机，以提升局域网内部并发转发效率。
- 在 Hub 一侧，其他主机会在物理层听到信号；进入交换机后，ACK 会被定向转发到 H2。

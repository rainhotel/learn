# 408 计算机网络 Projects

## Practice Ideas

- 做一份“408 网络高频易错点对照表”
- 做一份“TCP 全流程一页纸总结”

## Experiments

### Experiment 1

- 目标：把所有网络层计算题型归成固定模板
- 步骤：按子网划分、分片、路由聚合分类
- 结果：待补充
- 结论：待补充

### Packet Tracer 实验 09：三层交换机实现 VLAN 间通信

#### 实验目标

- 理解三层交换机如何通过 SVI 虚接口实现不同 VLAN 之间的三层转发。
- 掌握二层交换机 access 端口、trunk 端口、三层交换机 VLAN 虚接口和 `ip routing` 的配置。
- 能用 `ping`、`show vlan brief`、`show interfaces trunk`、`show ip route` 和模拟模式解释数据包变化。

#### 拓扑和 IP 规划

设备：

- 1 台 `3560-24PS` 三层交换机，命名为 `Multi-Switch`。
- 3 台 `2960-24TT` 二层交换机，命名为 `Switch0`、`Switch1`、`Switch2`。
- 8 台 PC，命名为 `PC0` 到 `PC7`。

连接关系：

- `Multi-Switch Fa0/1` 连接 `Switch0 Fa0/4`。
- `Multi-Switch Fa0/2` 连接 `Switch2 Fa0/3`。
- `Multi-Switch Fa0/3` 连接 `Switch1 Fa0/4`。
- `Switch0 Fa0/1` 连接 `PC0`，`Switch0 Fa0/2` 连接 `PC1`，`Switch0 Fa0/3` 连接 `PC2`。
- `Switch1 Fa0/1` 连接 `PC3`，`Switch1 Fa0/2` 连接 `PC4`，`Switch1 Fa0/3` 连接 `PC5`。
- `Switch2 Fa0/1` 连接 `PC6`，`Switch2 Fa0/2` 连接 `PC7`。

PC 地址：

| 设备 | IP 地址 | 子网掩码 | 默认网关 | 所连端口 | VLAN |
| --- | --- | --- | --- | --- | --- |
| PC0 | `192.1.1.1` | `255.255.255.0` | `192.1.1.254` | `Switch0 Fa0/1` | VLAN 2 |
| PC1 | `192.1.1.2` | `255.255.255.0` | `192.1.1.254` | `Switch0 Fa0/2` | VLAN 2 |
| PC2 | `192.1.3.1` | `255.255.255.0` | `192.1.3.254` | `Switch0 Fa0/3` | VLAN 4 |
| PC3 | `192.1.3.2` | `255.255.255.0` | `192.1.3.254` | `Switch1 Fa0/1` | VLAN 4 |
| PC4 | `192.1.2.2` | `255.255.255.0` | `192.1.2.254` | `Switch1 Fa0/2` | VLAN 3 |
| PC5 | `192.1.2.3` | `255.255.255.0` | `192.1.2.254` | `Switch1 Fa0/3` | VLAN 3 |
| PC6 | `192.1.1.3` | `255.255.255.0` | `192.1.1.254` | `Switch2 Fa0/1` | VLAN 2 |
| PC7 | `192.1.2.1` | `255.255.255.0` | `192.1.2.254` | `Switch2 Fa0/2` | VLAN 3 |

三层交换机 SVI 网关：

- `interface vlan 2`: `192.1.1.254/24`
- `interface vlan 3`: `192.1.2.254/24`
- `interface vlan 4`: `192.1.3.254/24`

#### 操作顺序

1. 按拓扑放置设备并连线。Packet Tracer 中交换机到交换机可以先用直通线，若链路一直不亮再换交叉线。
2. 给 8 台 PC 配 IP、子网掩码和默认网关。
3. 在三台二层交换机上创建 VLAN，并把连接 PC 的口设为 access。
4. 把二层交换机上联口设为 trunk，并限制允许通过的 VLAN。
5. 在三层交换机上创建 VLAN，把连接二层交换机的口设为 trunk。
6. 在三层交换机上创建 VLAN 虚接口，配置网关地址，执行 `ip routing`。
7. 用 `ping` 和 `show` 命令验证。
8. 进入 Simulation 模式，过滤 ICMP，观察 802.1Q 标签和 TTL 变化。

#### Switch0 配置

```text
enable
configure terminal
hostname Switch0
vlan 2
 name vlan2
exit
vlan 4
 name vlan4
exit
interface fastEthernet0/1
 switchport mode access
 switchport access vlan 2
exit
interface fastEthernet0/2
 switchport mode access
 switchport access vlan 2
exit
interface fastEthernet0/3
 switchport mode access
 switchport access vlan 4
exit
interface fastEthernet0/4
 switchport mode trunk
 switchport trunk allowed vlan 2,4
end
show vlan brief
show interfaces trunk
```

#### Switch1 配置

```text
enable
configure terminal
hostname Switch1
vlan 3
 name vlan3
exit
vlan 4
 name vlan4
exit
interface fastEthernet0/1
 switchport mode access
 switchport access vlan 4
exit
interface fastEthernet0/2
 switchport mode access
 switchport access vlan 3
exit
interface fastEthernet0/3
 switchport mode access
 switchport access vlan 3
exit
interface fastEthernet0/4
 switchport mode trunk
 switchport trunk allowed vlan 3,4
end
show vlan brief
show interfaces trunk
```

#### Switch2 配置

```text
enable
configure terminal
hostname Switch2
vlan 2
 name vlan2
exit
vlan 3
 name vlan3
exit
interface fastEthernet0/1
 switchport mode access
 switchport access vlan 2
exit
interface fastEthernet0/2
 switchport mode access
 switchport access vlan 3
exit
interface fastEthernet0/3
 switchport mode trunk
 switchport trunk allowed vlan 2,3
end
show vlan brief
show interfaces trunk
```

#### Multi-Switch 配置

如果 `switchport trunk encapsulation dot1q` 报错，就跳过这一行，保留 `switchport mode trunk`。有些 Packet Tracer 交换机型号默认只支持 dot1q，不需要手动指定封装。

```text
enable
configure terminal
hostname Multi-Switch
vlan 2
 name vlan2
exit
vlan 3
 name vlan3
exit
vlan 4
 name vlan4
exit
interface fastEthernet0/1
 switchport trunk encapsulation dot1q
 switchport mode trunk
 switchport trunk allowed vlan 2,4
exit
interface fastEthernet0/2
 switchport trunk encapsulation dot1q
 switchport mode trunk
 switchport trunk allowed vlan 2,3
exit
interface fastEthernet0/3
 switchport trunk encapsulation dot1q
 switchport mode trunk
 switchport trunk allowed vlan 3,4
exit
interface vlan 2
 ip address 192.1.1.254 255.255.255.0
 no shutdown
exit
interface vlan 3
 ip address 192.1.2.254 255.255.255.0
 no shutdown
exit
interface vlan 4
 ip address 192.1.3.254 255.255.255.0
 no shutdown
exit
ip routing
end
show ip interface brief
show interfaces trunk
show ip route
```

#### 验证方法

先测同 VLAN：

- `PC0` ping `PC1`: `192.1.1.2`
- `PC0` ping `PC6`: `192.1.1.3`
- `PC4` ping `PC5`: `192.1.2.3`

再测跨 VLAN：

- `PC0` ping `PC4`: `192.1.2.2`
- `PC0` ping `PC3`: `192.1.3.2`
- `PC7` ping `PC2`: `192.1.3.1`

三层交换机 `show ip route` 应出现三条直连路由：

```text
C    192.1.1.0/24 is directly connected, Vlan2
C    192.1.2.0/24 is directly connected, Vlan3
C    192.1.3.0/24 is directly connected, Vlan4
```

#### 模拟模式观察点

- PC 发到接入口时，是普通 `Ethernet II` 帧，没有 VLAN Tag。
- 从二层交换机走 trunk 发给三层交换机时，会变成 `Ethernet 802.1Q` 帧，带 VLAN ID。
- 三层交换机完成路由转发后，IP 包的 TTL 会减 1。
- 发到目标 PC 所在接入口时，交换机会去掉 802.1Q 标签，还原成普通以太网帧。

#### 常见错误排查

- 同 VLAN 都 ping 不通：先看 PC IP、子网掩码、接入口 VLAN、线缆状态。
- 同 VLAN 跨交换机不通：看上联口是不是 trunk，`allowed vlan` 有没有漏。
- 能 ping 网关但跨 VLAN 不通：看三层交换机有没有 `ip routing`。
- VLAN 虚接口是 down/down：确认对应 VLAN 已创建，并且至少有一个承载该 VLAN 的端口是 up。
- 只有某个 VLAN 不通：优先看该 VLAN 在每条 trunk 上是否被允许通过。

### Packet Tracer 实验 10：网络端口地址转换 PAT

#### 实验目标

- 理解 PAT 如何让多个内网主机共用一个公网地址访问外网。
- 掌握 `ip nat inside`、`ip nat outside`、地址池、ACL 和 `overload` 的配置。
- 能查看 NAT 转换表，解释 inside local、inside global、outside local、outside global。

#### 拓扑和 IP 规划

设备：

- 2 台 `2811` 路由器：公司出口路由器 `R0`，运营商路由器 `ISP`。
- 1 台 `2960-24TT` 交换机。
- 3 台 PC：`PC0`、`PC1`、`PC2`。
- 1 台服务器：`Web Server`。

连接关系：

- `PC0`、`PC1`、`PC2` 连接到 `Switch0`。
- `Switch0` 连接 `R0 FastEthernet0/0`。
- `R0 Serial0/3/0` 连接 `ISP Serial0/3/0`。
- `ISP FastEthernet0/0` 连接 `Web Server`。

如果 2811 没有串口，先关闭路由器电源，加装 `WIC-2T` 或同类 Serial 模块，再开机。实际端口号可能与教材不同，按 Packet Tracer 线缆两端显示的端口名配置即可。

地址规划：

| 设备/接口 | IP 地址 | 子网掩码 | 默认网关 |
| --- | --- | --- | --- |
| PC0 | `192.168.1.1` | `255.255.255.0` | `192.168.1.254` |
| PC1 | `192.168.1.2` | `255.255.255.0` | `192.168.1.254` |
| PC2 | `192.168.1.3` | `255.255.255.0` | `192.168.1.254` |
| R0 Fa0/0 | `192.168.1.254` | `255.255.255.0` | 无 |
| R0 S0/3/0 | `200.10.1.254` | `255.255.255.0` | 无 |
| ISP S0/3/0 | `200.10.1.253` | `255.255.255.0` | 无 |
| ISP Fa0/0 | `100.1.1.254` | `255.255.255.0` | 无 |
| Web Server | `100.1.1.1` | `255.255.255.0` | `100.1.1.254` |

公网 PAT 地址池：

- `200.10.1.1` 到 `200.10.1.1`

#### R0 配置

串口哪一端是 DCE，哪一端才需要 `clock rate 64000`。如果输入时报错，说明当前端口不是 DCE，跳过即可。

```text
enable
configure terminal
hostname R0
interface fastEthernet0/0
 ip address 192.168.1.254 255.255.255.0
 no shutdown
exit
interface serial0/3/0
 ip address 200.10.1.254 255.255.255.0
 clock rate 64000
 no shutdown
exit
ip route 0.0.0.0 0.0.0.0 200.10.1.253
interface fastEthernet0/0
 ip nat inside
exit
interface serial0/3/0
 ip nat outside
exit
ip nat pool pat-pool 200.10.1.1 200.10.1.1 netmask 255.255.255.0
access-list 1 permit 192.168.1.0 0.0.0.255
ip nat inside source list 1 pool pat-pool overload
end
show ip interface brief
show ip nat statistics
show ip nat translations
```

#### ISP 配置

```text
enable
configure terminal
hostname ISP
interface serial0/3/0
 ip address 200.10.1.253 255.255.255.0
 no shutdown
exit
interface fastEthernet0/0
 ip address 100.1.1.254 255.255.255.0
 no shutdown
exit
ip route 0.0.0.0 0.0.0.0 200.10.1.254
end
show ip interface brief
show ip route
```

#### Web Server 配置

- `Desktop > IP Configuration`
  - IP: `100.1.1.1`
  - Mask: `255.255.255.0`
  - Gateway: `100.1.1.254`
- `Services > HTTP`
  - 确认 HTTP 为 `On`。

#### PC 配置

分别进入 `Desktop > IP Configuration`：

- `PC0`: `192.168.1.1/24`，网关 `192.168.1.254`
- `PC1`: `192.168.1.2/24`，网关 `192.168.1.254`
- `PC2`: `192.168.1.3/24`，网关 `192.168.1.254`

#### 验证方法

先在路由器上测链路：

```text
R0#ping 200.10.1.253
R0#ping 100.1.1.1
ISP#ping 200.10.1.254
ISP#ping 192.168.1.254
```

再在 PC 上访问外网服务器：

- `PC0 > Desktop > Web Browser` 输入 `http://100.1.1.1`
- `PC1 > Desktop > Web Browser` 输入 `http://100.1.1.1`
- `PC2 > Desktop > Web Browser` 输入 `http://100.1.1.1`

访问后在 `R0` 查看 NAT 表：

```text
show ip nat translations
```

应看到类似结果：

```text
Pro  Inside global       Inside local        Outside local      Outside global
tcp  200.10.1.1:1025     192.168.1.1:1025    100.1.1.1:80      100.1.1.1:80
tcp  200.10.1.1:1026     192.168.1.2:1026    100.1.1.1:80      100.1.1.1:80
tcp  200.10.1.1:1027     192.168.1.3:1027    100.1.1.1:80      100.1.1.1:80
```

这里的重点：

- `Inside local`: 内网主机真实地址，例如 `192.168.1.1`。
- `Inside global`: 转换后的公网地址加端口，例如 `200.10.1.1:1025`。
- `Outside global`: 外部服务器真实地址，例如 `100.1.1.1:80`。
- `overload`: 多个内网地址复用同一个公网地址，通过端口号区分连接。

#### 模拟模式观察点

- 内网 PC 发出 HTTP 请求时，源地址最初是 `192.168.1.x`。
- 数据包经过 R0 出口后，源地址被改成 `200.10.1.1`，源端口也被记录到 NAT 表中。
- Web Server 回包时，目的地址是 `200.10.1.1:端口`。
- R0 根据 NAT 表把目的地址还原为对应的 `192.168.1.x:端口`。

#### 常见错误排查

- PC ping 不通 `192.168.1.254`：检查 PC 地址、网关、交换机到 R0 的线和 R0 `Fa0/0 no shutdown`。
- R0 ping 不通 ISP：检查串口 IP、掩码、`no shutdown`，以及 DCE 端是否设置 `clock rate`。
- R0 ping 不通 Web Server：检查 ISP 到服务器接口、服务器网关、ISP 路由。
- PC 能 ping R0 但打不开 Web：检查 R0 默认路由、ISP 回程路由、服务器 HTTP 服务是否开启。
- `show ip nat translations` 为空：先让 PC 访问 Web；再检查 ACL、`ip nat inside/outside`、`ip nat inside source ... overload`。
- NAT 表里没有端口复用效果：检查命令末尾是否有 `overload`。

#### 实验报告截图建议

- 实验 09：拓扑图、PC IP 配置、各交换机 `show vlan brief`、`show interfaces trunk`、三层交换机 `show ip route`、跨 VLAN ping 成功结果、Simulation 中 802.1Q 标签截图。
- 实验 10：拓扑图、PC 和 Server IP 配置、R0/ISP 接口配置、R0/ISP 静态路由、Web Browser 访问成功、R0 `show ip nat translations`、Simulation 中 R0 改写源地址的截图。

## Real Outputs

- 已完成的项目：当前无
- 下一步可做的项目：408 网络冲刺手册

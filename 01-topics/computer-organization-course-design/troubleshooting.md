# Logisim 故障排查：PC/Clks 不动或出现 ffffffff

## 先判断 `ffffffff` 是不是正常

测试程序 `sort-5.hex` 第一条指令是：

```text
2010ffff
```

对应汇编：

```asm
addi $s0, $0, -1
```

所以如果你看到的是寄存器 `$s0`，也就是寄存器编号 16，变成：

```text
ffffffff
```

这是正确现象，不是故障。它说明第一条 `addi` 已经执行了。

真正异常的是下面这些情况：

- `PC` 第一拍或很早变成 `ffffffff`。
- `IR` 一直不是 `2010ffff`。
- 微程序版本里 `mAddr/微地址` 一直不变。
- 3 级时序版本里状态寄存器 `S3 S2 S1 S0` 一直不变。
- `Clks` 或 `指令数` 永远不动，并且 CPU 内部状态也不动。

## 如果你跑的是实验 5-3：微程序/现代时序

### 第一轮取指应该看到的值

初始时，建议先复位仿真，再单步看：

| 微地址 | 应亮控制信号 | 预期现象 |
|---:|---|---|
| 0 | `PCout ARin Xin` | `AR=PC=0`，`X=0` |
| 1 | `Add4` | `Z=4` |
| 2 | `Zout PCin DREin READ` | `PC=4`，从内存读出第一条指令 |
| 3 | `DREout IRin P0` | `IR=2010ffff`，随后跳到 ADDI 入口 |
| 20 | `Rout Xin` | 读 `$0` 到 `X` |
| 21 | `IR(I)out Add` | `Z=ffffffff` |
| 22 | `Zout Rin` | `$s0=ffffffff`，然后回 0 取下一条 |

如果 `$s0=ffffffff` 出现在微地址 22 之后，这是正确的。

### 微地址不跳到 20 的排查

在 `◇微程序控制器` 中检查：

1. 控制存储器 ROM 的第 3 行是否是 `00810080`。
2. 这条微指令是否让 `P0=1`。
3. `◇微程序入口查找逻辑` 对 `ADDI` 的输出是否是 `10100`，也就是十进制 20。
4. 多路选择器是否能在 `P0=1` 时选择“微程序入口”这一路。

如果取指后不跳到 20，而是回 0 或跳到奇怪地址，问题几乎一定在 `P0`、入口查找逻辑、或微地址 MUX 选择线上。

### PC 变成 ffffffff 的排查

先看 `PCin` 在哪一拍亮：

- 微地址 2 时 `PCin=1`，内部总线应为 `00000004`。
- 微地址 16 时 `PCin=1`，内部总线应为 BEQ 分支地址。
- 其他时候 `PCin` 应为 0。

如果微地址 21 或 22 时 `PCin=1`，PC 会错误吃到 `ffffffff`。这说明控制总线位序接错了，尤其是 `PCin` 和 `Rin/Zout/IR(I)out` 附近的 Splitter 位序。

### ControlBus 位序快速检查

微程序主电路里，`ControlBus` 22 位应按以下顺序拆出：

```text
bit21 PCout
bit20 DRout
bit19 Zout
bit18 Rout
bit17 IR(I)out
bit16 IR(A)out
bit15 DREout
bit14 PCin
bit13 ARin
bit12 DREin
bit11 DRin
bit10 Xin
bit09 Rin
bit08 IRin
bit07 PSWin
bit06 RegTgt
bit05 RegDst
bit04 Add
bit03 Add4
bit02 Slt
bit01 READ
bit00 WRITE
```

最常见错误：把 `PCin` 所在位接成了 `Rin`、`Zout` 或 `IR(I)out`，导致执行 `addi -1` 时 PC 也被写成 `ffffffff`。

## 如果你跑的是实验 5-1：定长 3 级时序

### 先确认打开的是 CPU 顶层

文件打开后应位于：

```text
◆单总线CPU(3级时序)
```

如果停在 `◇硬布线控制器` 或 `◇指令译码器` 子电路里，很多输入 Pin 会显示 `X`，这只是子电路脱离顶层后的浮空状态，不代表 CPU 已经坏了。当前 `MipsOnBusCpu-3-exp5-1.circ` 已把主入口改为 CPU 顶层。

### 主电路探测层必须完整

当前已将 `MipsOnBusCpu-3-exp5-1-trace.circ` 里的完整主电路探测层合并回 `MipsOnBusCpu-3-exp5-1.circ`：

```text
pc ir imm32 ar dr x z bus ctrl halt
```

这些输出脚能直接观察关键运行状态，也和 `mips-probe.jar` 的自动暂停/追踪机制相关。若缺失这层，可能表现为 `ClockPause` 不变化、只能看到 RAM 首格被改坏、后续难以判断程序是否继续执行。

第一条真正写内存的 `sw $s0,512($s1)` 到来时，`ar` 应为：

```text
00000200
```

RAM 使用 `AR[11:2]` 作字地址，所以应写到行 `080`，不是行 `000`。

### 状态机必须先动

进入 `◇硬布线控制器`，看状态寄存器或探针：

```text
0 -> 1 -> 2 -> 3 -> 4 -> ... -> 11 -> 0
```

如果状态不变：

1. 检查状态寄存器有没有接 `CLK`。
2. 检查下一状态 ROM 是否输出 `1 2 3 ... b 0`。
3. 注意 3 级时序主电路里的计数器是下降沿触发，单击半拍时某些计数显示可能不动；用完整时钟周期观察。

### 第一轮取指应该看到的控制信号

| 阶段 | 应亮控制信号 | 预期现象 |
|---|---|---|
| `Mif T1` | `PCout ARin Xin` | `AR=PC=0` |
| `Mif T2` | `Add4` | `Z=4` |
| `Mif T3` | `Zout PCin DREin READ` | `PC=4`，读内存 |
| `Mif T4` | `DRout IRin` | `IR=2010ffff` |

如果 `Mif T3` 时内部总线不是 `00000004`，而是 `ffffffff` 或 `1000ffff`，说明总线源被接错或多个输出同时打开。

## 三个最高概率错误

1. **把正常现象误判成坏了**  
   第一条指令就是把 `$s0` 置为 `ffffffff`。

2. **控制总线位序反了**  
   症状：`PCin` 在 ADDI 写回阶段也亮，PC 被写成 `ffffffff`。

3. **只点了半个时钟沿**  
   症状：`Clks/指令数` 不动，但某些组合逻辑变了。3 级时序版本里计数器是下降沿触发，建议用完整周期或多点几次观察。

## 你现在应立刻看的 6 个探针

按这个顺序看，能最快定位：

1. `PC`
2. `IR`
3. 微程序版本看 `mAddr/微地址`；3 级时序版本看 `S3 S2 S1 S0`
4. `PCin`
5. `内部总线`
6. `Rin` 和寄存器 `$s0`

如果 `PC` 没坏、`IR=2010ffff`、`$s0=ffffffff`，说明已经跑过第一条，下一步要查 `Clks/指令数` 计数逻辑，不是 CPU 核心取指坏了。

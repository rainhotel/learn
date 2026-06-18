# Computer Organization Course Design Projects

## 先分清文件

资料包位置：

`D:\moniC\project\learn\hustzc\7.单总线CPU\单总线实验资料包(愚人节版)`

关键文件：

- `MipsOnBusCpu-3.circ`：3 级时序模板，很多子电路只有 Pin，适合从零补。
- `MipsOnBusCpu-3-exp5-1.circ`：实验 5-1 已补好的参考电路，能直接反推接法。
- `MipsOnBusCpu-1.circ`：微程序/现代时序模板，微程序 ROM 当前基本为空，需要补。
- `1.单总线MIPS三级时序产生器逻辑自动生成(2020-4-1).xlsx`：实验 5-1 时序状态机和输出函数。
- `2.单总线MIPS三级时序控制器控制信号逻辑自动生成(2020-4-3).xlsx`：实验 5-1 硬布线控制信号。
- `3.单总线MIPS现代时序微程序控制器设计(2020-4-5).xlsx`：实验 5-3 微程序表。
- `4.判别测试逻辑自动生成表达式(2020-4-1).xlsx`：实验 5-3 P 位判别逻辑。

## 实验 5-1：定长指令周期 3 级时序

### 最快理解

主数据通路已经搭好。你真正需要补的是 `◇硬布线控制器` 内部的三个部分：

1. `◇时序发生器状态机(定长指令周期)`
2. `◇时序发生器输出函数(定长指令周期)`
3. `◇硬布线控制器组合逻辑单元`

实验 5-1 不是让你重新画 CPU，而是让你让控制器在每个节拍产生正确控制信号。

### 可直接照做的接法

打开 `MipsOnBusCpu-3-exp5-1.circ`，进入对应子电路，对照补到自己的 `MipsOnBusCpu-3.circ`。

#### 1. 定长状态机

子电路：`◇时序发生器状态机(定长指令周期)`

用一个 ROM 实现下一状态：

- addrWidth：4
- dataWidth：4
- 输入：`S3 S2 S1 S0`
- 输出：`N3 N2 N1 N0`

ROM 内容：

```text
addr/data: 4 4
1 2 3 4 5 6 7 8
9 a b 0 0 0 0 0
```

意思是状态 0 到 11 顺序循环，状态 11 后回 0。定长指令周期就是每条指令固定走这一圈。

#### 2. 定长时序输出函数

子电路：`◇时序发生器输出函数(定长指令周期)`

用一个 ROM 实现当前状态到阶段信号：

- addrWidth：4
- dataWidth：7
- 输入：`S3 S2 S1 S0`
- 输出：`Mif Mcal Mex T1 T2 T3 T4`

ROM 内容：

```text
addr/data: 4 7
48 44 42 41 28 24 22 21
18 14 12 11 00 00 00 00
```

解释：

- 状态 0-3 是取指阶段 `Mif`，分别对应 `T1-T4`。
- 状态 4-7 是计算/译码阶段 `Mcal`，分别对应 `T1-T4`。
- 状态 8-11 是执行/访存阶段 `Mex`，分别对应 `T1-T4`。

#### 3. 硬布线控制器组合逻辑

子电路：`◇硬布线控制器组合逻辑单元`

这个子电路输入 13 个信号：

`SLT ADDI LW SW BEQ Mif Mcal Mex T1 T2 T3 T4 EQUAL`

输出 22 个控制信号：

`DRout PCout Rout Zout IR(A)out IR(I)out PCin DREout DREin ARin Xin DRin IRin Rin RegTgt PSWin Add RegDst Slt Add4 WRITE READ`

最稳操作：

1. 打开 `MipsOnBusCpu-3-exp5-1.circ`。
2. 进入 `◇硬布线控制器组合逻辑单元`。
3. 在你自己的模板里保持 Pin 区域不变。
4. 复制完成版中间的 ROM、Splitter、Tunnel、辅助 AND/OR 逻辑。

不要改 Pin 的名字、宽度、方向，否则平台自动测试可能识别不到。

### 控制总线位序

如果你用 ROM 方式生成 22 位控制总线，位序按高位到低位理解：

```text
DRout PCout Rout Zout IR(A)out IR(I)out PCin DREout DREin ARin Xin DRin IRin Rin RegTgt PSWin Add RegDst Slt Add4 WRITE READ
```

在 Logisim 的 Splitter 上，注意左右/上下方向会让视觉顺序和 bit 编号相反。不要只凭“看起来从左到右”判断，最好对照完成版。

当前已修正 `MipsOnBusCpu-3-exp5-1.circ` 的主电路入口：打开文件会直接进入 `◆单总线CPU(3级时序)`，而不是浮空输入很多的 `◇硬布线控制器` 子电路。

## 实验 5-3：现代时序/微程序控制器

### 你真正要补的地方

文件：`MipsOnBusCpu-1.circ`

重点子电路：

1. `◇微程序控制器`
2. `◇微程序入口查找逻辑`
3. `◇条件判别测试逻辑`

其中 `◇微程序控制器` 里的控制存储器 ROM 现在只有 `0`，所以必须补微程序。

### 微指令位序

ROM 是 30 位：

```text
22 位控制字段 + 3 位判别字段 + 5 位下址字段
```

从高位到低位：

```text
PCout DRout Zout Rout IR(I)out IR(A)out DREout PCin ARin DREin DRin Xin Rin IRin PSWin RegTgt RegDst Add Add4 Slt READ WRITE P0 P1 P2 N4 N3 N2 N1 N0
```

资料表前三行可以验证：

- `20240001` = `PCout + ARin + Xin + next 1`
- `00000802` = `Add4 + next 2`
- `08500203` = `Zout + PCin + DREin + READ + next 3`

### 控制存储器 ROM 内容

进入 `◇微程序控制器`，找到控制存储器 ROM：

- addrWidth：5
- dataWidth：30
- Select：high

把 ROM 内容改成：

```text
addr/data: 5 30
20240001 00000802 08500203 00810080 04040005 02001006 08200007 00100208
00820000 0404000a 0200100b 0820000c 0410400d 00000100 00000040 00000000
01400000 04040012 04004413 08022000 04040015 02001016 08020000 00000000
00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000000
```

### 微程序表

| 地址 | 微操作 | 十六进制 |
|---:|---|---:|
| 0 | `PCout, ARin, Xin`，取指地址送 AR，同时 PC 送 X | `20240001` |
| 1 | `Add4`，计算 PC+4 | `00000802` |
| 2 | `Zout, PCin, DREin, READ`，更新 PC 并读指令 | `08500203` |
| 3 | `DREout, IRin, P0=1`，指令装入 IR，并按指令入口转移 | `00810080` |
| 4 | `Rout, Xin`，LW 取基址寄存器 | `04040005` |
| 5 | `IR(I)out, Add`，LW 计算有效地址 | `02001006` |
| 6 | `Zout, ARin`，LW 地址送 AR | `08200007` |
| 7 | `DREin, READ`，LW 读存储器 | `00100208` |
| 8 | `DREout, Rin`，LW 写回 rt | `00820000` |
| 9 | `Rout, Xin`，SW 取基址寄存器 | `0404000a` |
| 10 | `IR(I)out, Add`，SW 计算有效地址 | `0200100b` |
| 11 | `Zout, ARin`，SW 地址送 AR | `0820000c` |
| 12 | `Rout, DREin, RegTgt`，SW 取 rt 数据送 DRE | `0410400d` |
| 13 | `WRITE`，写存储器 | `00000100` |
| 14 | `P1=1`，BEQ 条件判别，equal 为真转 16，否则回 0 | `00000040` |
| 16 | `IR(A)out, PCin`，BEQ 更新 PC | `01400000` |
| 17 | `Rout, Xin`，SLT 取 rs | `04040012` |
| 18 | `Rout, RegTgt, Slt`，SLT 取 rt 并比较 | `04004413` |
| 19 | `Zout, Rin, RegDst`，SLT 写回 rd | `08022000` |
| 20 | `Rout, Xin`，ADDI 取 rs | `04040015` |
| 21 | `IR(I)out, Add`，ADDI 加立即数 | `02001016` |
| 22 | `Zout, Rin`，ADDI 写回 rt | `08020000` |

### 微程序入口查找逻辑

子电路：`◇微程序入口查找逻辑`

输入：

`LW SW BEQ SLT ADDI`

输出：

`S4 S3 S2 S1 S0`

建议入口地址：

| 指令 | 入口地址十进制 | 二进制 |
|---|---:|---|
| LW | 4 | `00100` |
| SW | 9 | `01001` |
| BEQ | 14 | `01110` |
| SLT | 17 | `10001` |
| ADDI | 20 | `10100` |

可以用 OR 门直接生成每一位：

```text
S4 = SLT or ADDI
S3 = SW or BEQ
S2 = LW or BEQ or ADDI
S1 = BEQ
S0 = SW or SLT
```

`OtherInstr` 默认入口接 0，回到取指。

### 条件判别测试逻辑

子电路：`◇条件判别测试逻辑`

输入：

`P0 P1 equal`

输出：

`S1 S0`

建议逻辑：

```text
S0 = P0
S1 = P1 and equal
```

含义：

- `P0=0, P1=0`：微地址选择下址字段，顺序执行。
- `P0=1, P1=0`：微地址选择“微程序入口”，用于取指后按指令分派。
- `P0=0, P1=1, equal=1`：微地址选择 BEQ 分支入口。
- `P0=0, P1=1, equal=0`：仍选择下址字段，BEQ 不跳转，回取指。

在 `◇微程序控制器` 的多路选择器旁，`beq分支` 那一路接一个 5 位 Constant，值为 `0x10`，也就是十进制 16。

### 容易连错的点

- `RegTgt=0` 读 `rs`，`RegTgt=1` 读 `rt`。
- `RegDst=0` 写 `rt`，`RegDst=1` 写 `rd`。
- LW/ADDI 写 rt，所以不置 `RegDst`。
- SLT 写 rd，所以最后一步要置 `RegDst`。
- SW 需要把 rt 数据送到 `DRE`，所以 `SW4` 要置 `RegTgt`。
- BEQ 不能无条件 `PCin`，必须通过 `P1 and equal` 选择地址 16。

## 测试与截图建议

报告截图至少保留：

1. 实验 5-1 主 CPU 电路。
2. 实验 5-1 定长状态机 ROM。
3. 实验 5-1 控制器组合逻辑。
4. 实验 5-3 微程序控制器。
5. 实验 5-3 控制存储器 ROM。
6. 实验 5-3 入口查找逻辑。
7. 实验 5-3 判别测试逻辑。
8. 运行测试程序后的寄存器/内存结果。

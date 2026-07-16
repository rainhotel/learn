# JMM 实验运行说明

## 环境

- JDK 21
- 无第三方依赖

## 编译与运行

从仓库根目录执行：

```powershell
New-Item -ItemType Directory -Force '.\tmp\course-classes' | Out-Null
javac -d '.\tmp\course-classes' '.\01-topics\java-backend-big-tech-preparation\course\03-jmm-volatile-synchronized\lab\JmmLab.java'
java -cp '.\tmp\course-classes' JmmLab
```

成功标志：

```text
ALL_EXPERIMENTS_PASSED
```

## 实验

1. volatile 写-读建立发布关系，使读线程观察到此前普通字段写入。
2. 两个线程被屏障协调为“同时读、再同时写”，确定性展示 volatile `value++` 仍会丢失更新。
3. synchronized 保护复合递增。
4. monitor unlock 与后续 lock 建立 happens-before。
5. monitor 可重入，且 synchronized 块异常退出时自动解锁。

## 为什么没有使用“非 volatile 循环是否退出”实验

错误同步程序允许出现反直觉行为，但某一次运行恰好退出或不退出都不能证明普遍结论。课程使用规范定义的同步关系和可重复调度来验证，不把偶然现象包装成确定规律。

## 边界

- `CyclicBarrier` 自身带有同步语义；实验 2 使用它故意固定两个线程的读写交错，只用于证明 read-modify-write 不是单一原子动作。
- 实验不能替代 JLS 对合法执行的定义。
- 不要通过 `sleep` 企图建立可见性；JLS 明确说明 sleep/yield 没有同步语义。


# 练习答案与评分标准

## A. JMM

### 1

给定程序和执行轨迹，判断该执行是否是 Java 允许的行为；尤其规定共享变量读取可以观察哪些写入。

### 2

同一变量的两个访问至少一个为写时发生冲突；不同线程的冲突访问未被 happens-before 排序时存在 data race。

### 3

程序顺序、monitor unlock/lock、volatile write/read、start、线程终止/join、传递性。默认初始化也可作为补充。

### 4

不正确。happens-before 是规范定义的偏序关系，不是普通墙钟时间先后。

## B. volatile

### 1

普通写 program-order volatile 写；volatile 写 synchronizes-with volatile 读；volatile 读 program-order 普通读；传递得到普通写 hb 普通读。

### 2

递增是读-改-写复合动作，两个线程可读到相同旧值并覆盖更新。

### 3

适合：停止标记、不可变配置引用、单写多读独立状态。

不适合：递增、check-then-act、多字段不变量。

## C. synchronized

1. 实例方法锁 this；静态方法锁对应 Class 对象。
2. 同一线程可以对同一 monitor 多次 lock，每次对应一次 unlock。
3. 正常或异常完成都会自动 unlock。
4. 未持锁读取没有通过同一 monitor 建立可见性与一致性协议。
5. 放大临界区、阻塞其他线程，并可能形成锁与外部资源依赖。

## D. 推理

### 代码一

存在 data race，B 不保证退出或看到 value=42。修复：ready 使用 volatile；或对读写使用同一 synchronized/Lock。也可通过高层并发工具传递结果。

### 代码二

只在锁内构造但没有第二次 null 检查，多个已经通过外层判断的线程会依次构造。使用正确双重检查（引用 volatile）、静态初始化、枚举或依赖注入容器。

### 代码三

每次创建不同 monitor，不同线程互不竞争同一锁。使用稳定共享且通常 private final 的锁对象。

## E. 项目

### 1

构造不可变配置，完整校验后通过 volatile 引用一次替换；读任务一次获取快照。

### 2

指标使用 LongAdder；业务完成通过数据库状态/明细或强一致协议判断，不依赖瞬时 sum。

### 3

synchronized 只协调同 JVM 中对同一 monitor 的线程，不覆盖其他实例。

### 4

获取线程 dump，寻找 BLOCKED 线程、等待 monitor 和 owner，构建锁等待图，定位循环依赖；结合日志复现并统一锁顺序或减少嵌套。

## 加分题

正确构造且 this 不逸出时，final 字段有特殊初始化安全语义。final 引用不能重新赋值，但引用对象仍可能可变。

## 评分

- 90-100：能够进行 JMM 推理和项目答辩。
- 80-89：主体掌握，补做 volatile 或锁协议题。
- 60-79：重新画 happens-before 链并运行实验。
- 低于 60：返回 data race、volatile 与 monitor 基础。


# 章节练习

## A. JMM 基础（20 分）

1. JMM 描述的核心问题是什么？（5 分）
2. 什么是冲突访问和 data race？（5 分）
3. 列出六条常用 happens-before 规则。（6 分）
4. “A 在时间上先执行，因此 A happens-before B”是否正确？（4 分）

## B. volatile（20 分）

1. 推导 volatile 发布普通字段的 happens-before 链。（8 分）
2. 为什么 `volatile int count; count++` 不安全？（5 分）
3. 给出三个适合 volatile 和三个不适合的场景。（7 分）

## C. synchronized（20 分）

1. 实例 synchronized 方法和 static synchronized 方法分别锁什么？（4 分）
2. synchronized 为什么可重入？（4 分）
3. 异常退出时 monitor 是否释放？（4 分）
4. 为什么所有读写必须遵守同一锁协议？（4 分）
5. 为什么不建议持锁调用远程 API？（4 分）

## D. 推理题（20 分）

### 代码一

```java
int value;
boolean ready;

// A
value = 42;
ready = true;

// B
while (!ready) {}
System.out.println(value);
```

说明问题并给出两种修复。（8 分）

### 代码二

```java
if (instance == null) {
    synchronized (lock) {
        instance = new Service();
    }
}
```

说明为什么仍可能重复构造，以及正确选择。（6 分）

### 代码三

```java
synchronized (new Object()) {
    count++;
}
```

说明为什么没有形成线程间互斥。（6 分）

## E. 项目设计（20 分）

1. 设计 ProviderConfig 热更新。（6 分）
2. 选择成功计数的数据结构，并区分指标和业务完成状态。（5 分）
3. 解释 synchronized 为什么无法保证多实例通知配额。（4 分）
4. 为一个死锁案例给出诊断步骤。（5 分）

## 加分题

解释 final 字段安全语义、构造期间 `this` 逸出，以及 final 引用不代表深度不可变。


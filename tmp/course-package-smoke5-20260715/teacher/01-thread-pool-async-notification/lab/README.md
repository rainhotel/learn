# 线程池实验运行说明

## 环境

- JDK 21
- 不需要第三方依赖

## 编译与运行

在本目录执行：

```powershell
javac ThreadPoolLab.java
java ThreadPoolLab
```

成功标志：

```text
ALL_EXPERIMENTS_PASSED
```

## 实验内容

1. 验证 `corePoolSize -> queue -> maximumPoolSize -> reject` 的任务接收过程。
2. 验证 `CallerRunsPolicy` 会让提交线程执行任务，从而形成反馈式降速。
3. 验证 `shutdownNow()` 返回未启动任务，并以中断方式尝试停止运行任务。

## 注意

- 线程启动顺序不保证完全一致，不要把日志先后顺序当成规范承诺。
- `getActiveCount()` 等监控值是近似统计，实验仅在受控场景中使用它们等待稳定状态。
- `shutdownNow()` 只进行尽力中断；任务必须正确响应中断才能及时停止。


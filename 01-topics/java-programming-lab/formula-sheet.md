# Java Programming Lab Formula Sheet

## 连续和

- 题型：计算 `1 + 2 + ... + n`

### 循环写法

```java
int sum = 0;
for (int i = 1; i <= n; i++) {
    sum += i;
}
```

- 适用场景：
  - 教学实验
  - 需要展示逐步累加逻辑时

### 数学公式

```text
1 + 2 + ... + n = n * (n + 1) / 2
```

- 适用场景：
  - 只关心结果
  - 需要更高效率时

## Iterator 遍历删除规则

- 原则：
  - 遍历时删除当前元素，应使用 `Iterator.remove()`

## HashSet 使用判断

- 需要“去重”时优先考虑 `Set`
- 需要“去重且高效查找”时优先考虑 `HashSet`
- 需要“有序”时不要默认选 `HashSet`

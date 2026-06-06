# Meet in the Middle Q&A

## Open Questions

- [ ] 为什么 `cntAB[s] * cntCD[-s]` 恰好是不重不漏的计数？
- [ ] 当值域不小的时候，我能否立刻切换到排序二分或哈希版本？

## Error Patterns

- 最近反复出现的错误：看到四个数组就想直接枚举，没先找是否可拆成两个 pair sum。
- 最近最容易混淆的概念：计“存在性”和计“出现次数”。

## Answered Questions

### 为什么这题不能用 `int` 存答案？

- 结论：答案必须用 `long long`。
- 依据：最坏情况下四元组数量可以达到 `n^4` 量级，`n=2000` 时远超 `int`。
- 例子：如果很多 pair sum 都能互相匹配，答案可能达到万亿级。


# AI Context

## 当前阶段

已根据用户提供的 PPT PDF 完成数据库系统复习主题。

## 文件状态

主题目录：

```text
01-topics/database-systems-review/
```

已生成：

- `raw-ppt-ocr.md`
- `ocr-extract-ppts.py`
- `README.md`
- `human-guide.md`
- `outline.md`
- `notes.md`
- `formula-sheet.md`
- `qa.md`
- `solved-problems.md`
- `projects.md`
- `ai-context.md`

## OCR 状态

已下载：

```text
tmp/tessdata/chi_sim.traineddata
```

OCR 脚本：

```text
01-topics/database-systems-review/ocr-extract-ppts.py
```

输出：

```text
01-topics/database-systems-review/raw-ppt-ocr.md
```

总 OCR 页数：

- 第 1 章：99 页。
- 第 2 章：137 页。
- 第 3 章：234 页。
- 第 4 章：127 页。
- 第 5 章：157 页。
- 第 7 章：66 页。

总计 820 页。

## 识别出的课件范围

- 第 1 章：数据库概览。
- 第 2 章：关系数据模型。
- 第 3 章：关系数据库语言 SQL。
- 第 4 章：数据库设计。
- 第 5 章：关系规范化理论。
- 第 7 章：并发控制、数据库恢复。

## OCR 质量说明

Tesseract 中文 OCR 可用，但有少量错字：

- “冗余”可能识别为“宛余/元余”。
- “查询”可能识别为“碍询”。
- “可串行化”可能识别为“可让行化”。

整理后的复习资料已按数据库教材常用术语修正。

## 后续可继续补强

如果用户还提供第 6 章课件，需要补充：

- 数据库应用开发。
- 可能还包括安全性/完整性单章内容。

如果用户提供真题，可进一步生成：

- 高频题型统计。
- 预测卷。
- 按分值分配的冲刺版背诵提纲。

# 中国三次化债与楼市学习主题实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在仓库中建立一个可持续维护的“中国三次化债与楼市”学习主题，兼顾宏观机制、公开数据和家庭/投资判断。

**Architecture:** 采用主题目录与研究目录分离的结构。主题目录承载稳定知识、导学、练习和进度；研究目录承载政策原文、数据来源、证据链和不确定性。时间线与资金流向图作为入口可视化，所有时效性结论标注截止日期。

**Tech Stack:** Markdown、仓库现有 StudyOS 目录约定、公开政策与统计资料、浏览器可视化伴侣。

---

### Task 1: 建立主题骨架

**Files:**
- Create: `01-topics/china-debt-housing/README.md`
- Create: `01-topics/china-debt-housing/outline.md`
- Create: `01-topics/china-debt-housing/human-guide.md`
- Create: `01-topics/china-debt-housing/ai-context.md`
- Create: `01-topics/china-debt-housing/progress.md`

- [ ] **Step 1: 复制 `_template` 的栏目结构并改写为本主题**
  - `README.md` 写目标、范围、先修知识、资源入口和下一步。
  - `outline.md` 分为政策时间线、地方财政机制、楼市机制、资产负债表、投资判断五阶段。
  - `human-guide.md` 用短段落解释“为什么重要、现在学什么、最值得回看什么”。
  - `ai-context.md` 记录当前阶段、证据依赖、缺口和待提炼项。
  - `progress.md` 建立里程碑：口径核定、来源核验、机制笔记、可视化、案例练习、复盘。

- [ ] **Step 2: 检查主题文件是否符合仓库文件落盘规则**
  - Run: `Get-ChildItem '01-topics/china-debt-housing'`
  - Expected: 五个文件均存在，文件名为 ASCII，目录 slug 为小写连字符。

### Task 2: 编写稳定知识与问题清单

**Files:**
- Create: `01-topics/china-debt-housing/notes.md`
- Create: `01-topics/china-debt-housing/qa.md`
- Create: `01-topics/china-debt-housing/formula-sheet.md`

- [ ] **Step 1: 在 `notes.md` 写三次化债的分期表**
  - 每期固定记录：时间、官方工具、债务对象、资金来源、利率/期限变化、受益方、成本承担方、解决的问题、遗留问题。
  - 明确区分“地方政府显性债务”“城投隐性债务”“房地产企业债务”和“居民按揭”。

- [ ] **Step 2: 在 `notes.md` 写楼市反馈机制**
  - 用“土地出让下降 → 地方财政承压 → 基建/公共支出受限 → 城投现金流恶化”和“房价预期下降 → 销售下降 → 开发商现金流恶化 → 银行风险上升”的两条链解释因果关系。

- [ ] **Step 3: 在 `qa.md` 建立误区题**
  - 至少覆盖：“化债等于债务消失吗？”、“中央接手地方债吗？”、“降息是否必然推高房价？”、“房企债务和地方债是一回事吗？”、“银行不良率低是否代表没有风险？”

- [ ] **Step 4: 在 `formula-sheet.md` 写判断公式和指标定义**
  - 包含债务率、利息负担率、杠杆率、现金流覆盖、库存去化周期、按揭负担率，并为每个指标写分子、分母、适用条件和常见误读。

### Task 3: 建立研究来源和证据链

**Files:**
- Create: `06-research/china-debt-housing/README.md`
- Create: `06-research/china-debt-housing/human-brief.md`
- Create: `06-research/china-debt-housing/ai-context.md`
- Create: `06-research/china-debt-housing/source-log.md`
- Create: `06-research/china-debt-housing/working-notes.md`
- Create: `06-research/china-debt-housing/conclusion.md`
- Create: `06-research/china-debt-housing/review.md`

- [ ] **Step 1: 在 `source-log.md` 建立来源表**
  - 字段固定为：日期、发布机构、文件/数据名称、链接或文件位置、统计口径、支持的主张、限制。
  - 优先收集财政部、央行、国家统计局、国家金融监督管理总局、交易所和国务院政策文件。

- [ ] **Step 2: 在 `working-notes.md` 记录争议口径**
  - 将“市场称呼的三次化债”与“官方政策名称”并排列出，禁止把未经核验的规模数字直接写成事实。

- [ ] **Step 3: 在 `conclusion.md` 输出证据链**
  - 每条结论按“主张 → 证据 → 机制解释 → 不确定性 → 对家庭/投资者的含义”写成短段落。

### Task 4: 制作入口可视化与学习日志

**Files:**
- Create: `01-topics/china-debt-housing/projects.md`
- Create: `01-topics/china-debt-housing/solved-problems.md`
- Create: `02-journal/2026/07/2026-07-14.md`
- Create: `.superpowers/brainstorm/` 下的时间线和资金流向图素材（若可视化伴侣会话仍有效）

- [ ] **Step 1: 在 `projects.md` 定义两个项目**
  - 项目 A：按年份整理化债政策和楼市节点。
  - 项目 B：画出土地财政、城投、银行、房企和居民的资金流向，并为每条箭头写资产负债表含义。

- [ ] **Step 2: 在 `solved-problems.md` 写首个标准题**
  - 题目：地方债务置换为何可能降低短期风险，却不等于总债务下降？
  - 固定栏目：来源、步骤、公式/方法、适用条件、结论。

- [ ] **Step 3: 在当日日志记录本次学习主题启动**
  - 记录用户目标、已确认设计、当前缺口和下一步核验任务。

### Task 5: 验证与复盘

**Files:**
- Modify: `01-topics/china-debt-housing/progress.md`
- Modify: `01-topics/china-debt-housing/review.md`

- [ ] **Step 1: 做结构检查**
  - Run: `Get-ChildItem -Recurse '01-topics/china-debt-housing','06-research/china-debt-housing' | Select-Object FullName`
  - Expected: 主题和研究目录中的优先文件齐全，无重复职责的大文件。

- [ ] **Step 2: 做内容检查**
  - 搜索 `TODO|TBD|待补`，确保只出现在明确的后续任务清单中，不出现在已宣称完成的结论里。
  - 检查每个时效性数字是否带来源日期和统计口径。

- [ ] **Step 3: 写阶段复盘**
  - `review.md` 记录最有效的解释、仍不确定的政策口径、数据缺口和下一轮研究问题。

- [ ] **Step 4: 提交独立变更**
  - Run: `git add -- '01-topics/china-debt-housing' '06-research/china-debt-housing' '02-journal/2026/07/2026-07-14.md'`
  - Run: `git commit -m "docs: add China debt and housing study topic"`

---

## Self-review

- 设计文档中的时间线、资产负债表、楼市机制、投资判断、案例和可视化均有对应任务。
- 没有把“化债”写成债务凭空消失；计划要求记录债权人、债务人、期限和成本转移。
- 没有预设房价或资产必然涨跌；所有现实判断都要求条件、来源和不确定性。
- 研究来源和稳定知识分目录，符合仓库的主题/研究分离规则。

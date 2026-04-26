# AI Context

## Current Phase

- Phase 2 - Lab execution finished and report packaged

## Key Artifacts

- `run_lab.py`: 生成全部实验结果图和 `metrics.json`
- `mean_filter_lab.m`: 供课程提交使用的 Matlab 参考脚本
- `build_report.py`: 生成 `report-draft.md` 和 `mean-filter-report.docx`
- `output/figures/`: 各任务结果图
- `output/report-qa/`: 报告渲染页 PNG，用于版面检查

## Dependencies

- Python runtime from workspace dependencies
- Python packages actually used: `numpy`, `PIL`, `python-docx`

## Known Gaps

- Matlab 脚本未在当前环境执行验证
- 报告封面信息沿用了上一次实验的个人资料，提交前应人工复核

## Regeneration Steps

1. 运行 `run_lab.py`
2. 运行 `build_report.py`
3. 用 `render_docx.py --renderer artifact-tool` 重新渲染报告做 QA

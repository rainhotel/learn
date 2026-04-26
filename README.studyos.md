# StudyOS MVP

StudyOS is a local-first learning OS for two workflows:

- Chinese exam preparation
- Research-style question exploration

## What is included

- Workspace initialization
- Exam and research topics
- Built-in packs for `gaokao-math`, `kaoyan-408`, `generic-exam-pack`, and `generic-research-pack`
- Session execution with provider abstraction
- Provider config for `ollama` and `openai-compatible`
- Local Web UI for dashboard, topic detail, session runner, and provider settings
- Standard-library-only test suite

## Quick start

```powershell
python -m studyos --workspace .studyos init --name "My StudyOS"
python -m studyos --workspace .studyos topic create "高考数学" --kind exam --pack gaokao-math --slug gaokao-math
python -m studyos --workspace .studyos topic plan gaokao-math
python -m studyos --workspace .studyos provider add local --kind ollama --base-url http://127.0.0.1:11434 --model qwen2.5:7b --default
python -m studyos --workspace .studyos session start gaokao-math --mode learn --input "请帮我梳理导数应用的第一轮学习重点"
python -m studyos --workspace .studyos ui
```

## Workspace model

StudyOS writes YAML-compatible JSON for machine-readable state files and Markdown for learning artifacts.

```text
workspace/
  workspace.yaml
  topics/
    <slug>/
      topic.yaml
      README.md
      human-guide.md
      progress.md
      notes.md
      qa.md
      solved-problems.md
      review.md
      source-log.md
      conclusion.md
  sessions/
  reviews/weekly/
  packs/
  cache/
  logs/
```

## CLI commands

- `studyos init`
- `studyos topic create`
- `studyos topic list`
- `studyos topic plan`
- `studyos session start`
- `studyos session review`
- `studyos provider add`
- `studyos provider list`
- `studyos provider test`
- `studyos ui`

## Community extension points

- provider plugins
- exam packs
- research workflow packs
- session templates
- artifact exporters

See `docs/community.md` for contributor-facing guidance.


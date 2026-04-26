from __future__ import annotations

import json
from typing import Any

from .models import Artifact, ReviewSummary, SessionRequest, SessionResult
from .providers import BaseProvider, build_provider
from .utils import (
    append_markdown_entry,
    parse_json_object,
    read_text,
    replace_section,
    save_data_file,
    session_id,
    short_text,
    today_str,
    week_str,
    write_text,
)
from .workspace import WorkspaceManager


def generate_topic_plan(manager: WorkspaceManager, slug: str, weeks: int = 8) -> str:
    topic = manager.load_topic(slug)
    pack = manager.load_pack(topic.kind, topic.pack)
    if topic.kind == "exam":
        units = pack.syllabus or []
        rows = []
        for week in range(1, weeks + 1):
            unit = units[min(week - 1, len(units) - 1)]
            tasks = [
                f"梳理 {unit['title']} 的核心概念",
                f"完成 {', '.join(unit.get('topics', [])[:2])} 的针对性练习",
                f"按 {pack.review_cadence} 频率做一次错因复盘",
            ]
            rows.append(
                f"### Week {week}\n\n"
                f"- Focus: {unit['title']}\n"
                f"- Topics: {', '.join(unit.get('topics', []))}\n"
                f"- Tasks:\n  - " + "\n  - ".join(tasks) + "\n"
            )
        current_focus = units[0]["title"] if units else "基础知识梳理"
        next_action = f"完成 {current_focus} 的第一轮 learn session"
    else:
        phases = pack.phases or ["scoping", "reading", "synthesis", "conclusion"]
        rows = []
        for week in range(1, weeks + 1):
            phase = phases[min(week - 1, len(phases) - 1)]
            tasks = [
                f"推进 {phase} 阶段",
                f"补充 {pack.evidence_dimensions[0] if pack.evidence_dimensions else '证据'} 的记录",
                "输出一条新的结论或未解问题",
            ]
            rows.append(
                f"### Week {week}\n\n"
                f"- Focus: {phase}\n"
                f"- Tasks:\n  - " + "\n  - ".join(tasks) + "\n"
            )
        current_focus = phases[0] if phases else "scoping"
        next_action = f"完成 {current_focus} 阶段的第一轮 session"

    readme_path = manager.topic_dir(slug) / "README.md"
    readme = read_text(readme_path)
    plan_text = "\n".join(rows)
    updated_readme = replace_section(readme, "Eight Week Plan", plan_text)
    write_text(readme_path, updated_readme)

    topic.current_focus = current_focus
    topic.next_action = next_action
    manager.save_topic(topic)

    human_guide_path = manager.topic_dir(slug) / "human-guide.md"
    guide = read_text(human_guide_path)
    guide = replace_section(
        guide,
        "Start Here",
        f"- 当前阶段：{topic.stage}\n- 当前焦点：{topic.current_focus}\n- 下次第一步：{topic.next_action}",
    )
    write_text(human_guide_path, guide)
    return plan_text


def _session_schema() -> dict[str, Any]:
    return {
        "type": "object",
        "required": ["summary", "stable_insight", "weakness_or_question", "next_step"],
    }


def _build_prompt(topic, pack, snapshot: dict[str, str], request: SessionRequest) -> str:
    template = pack.session_templates.get(request.mode, "")
    return f"""
你是 StudyOS 的学习引擎。请围绕以下主题执行一次学习 session，并仅返回 JSON 对象。

主题标题：{topic.title}
主题类型：{topic.kind}
当前阶段：{topic.stage}
当前焦点：{topic.current_focus}
当前下一步：{topic.next_action}
Session 模式：{request.mode}
Pack：{topic.pack}

Pack 模板提示：
{template}

主题 README 摘要：
{snapshot['readme'][:1500]}

人类快速导引：
{snapshot['human_guide'][:1200]}

当前进度：
{snapshot['progress'][:1200]}

当前知识沉淀：
{snapshot['notes'][:1200]}

用户输入：
{request.user_input}

返回 JSON，字段固定为：
summary, stable_insight, weakness_or_question, next_step, current_focus, stage,
notes_markdown, qa_markdown, solved_problems_markdown, source_log_markdown, conclusion_markdown
""".strip()


def _normalize_response(raw: dict[str, Any] | None, fallback: str = "") -> dict[str, str]:
    response = raw or {}
    return {
        "summary": str(response.get("summary") or short_text(fallback or "完成一次学习会话。", 120)),
        "stable_insight": str(response.get("stable_insight") or "形成了一条稳定知识。"),
        "weakness_or_question": str(response.get("weakness_or_question") or "仍有一个薄弱点需要补强。"),
        "next_step": str(response.get("next_step") or "继续推进下一次 session。"),
        "current_focus": str(response.get("current_focus") or response.get("stable_insight") or "继续当前主题"),
        "stage": str(response.get("stage") or ""),
        "notes_markdown": str(response.get("notes_markdown") or ""),
        "qa_markdown": str(response.get("qa_markdown") or ""),
        "solved_problems_markdown": str(response.get("solved_problems_markdown") or ""),
        "source_log_markdown": str(response.get("source_log_markdown") or ""),
        "conclusion_markdown": str(response.get("conclusion_markdown") or ""),
    }


def run_session(
    manager: WorkspaceManager,
    request: SessionRequest,
    provider: BaseProvider | None = None,
) -> SessionResult:
    topic = manager.load_topic(request.topic_slug)
    pack = manager.load_pack(topic.kind, topic.pack)
    snapshot = manager.topic_snapshot(topic.slug)
    provider = provider or build_provider(manager.get_provider(request.provider_name))

    prompt = _build_prompt(topic, pack, snapshot, request)
    raw_response_text = ""
    try:
        structured = provider.send_structured(prompt, _session_schema())
        raw_response_text = json.dumps(structured, ensure_ascii=False, indent=2)
    except Exception:
        raw_response_text = provider.send_chat([{"role": "user", "content": prompt}])
        structured = parse_json_object(raw_response_text) or {}

    normalized = _normalize_response(structured, raw_response_text)
    sid = session_id()
    session_dir = manager.sessions_root / today_str()
    markdown_path = session_dir / f"{sid}.md"
    json_path = session_dir / f"{sid}.json"

    write_text(
        markdown_path,
        f"""# Session {sid}

## Topic

- Slug: {topic.slug}
- Title: {topic.title}
- Mode: {request.mode}

## User Input

{request.user_input}

## Summary

{normalized['summary']}

## Stable Insight

{normalized['stable_insight']}

## Weakness Or Question

{normalized['weakness_or_question']}

## Next Step

{normalized['next_step']}

## Raw Response

```json
{raw_response_text}
```
""",
    )
    save_data_file(
        json_path,
        {
            "request": {
                "topic_slug": request.topic_slug,
                "mode": request.mode,
                "provider_name": request.provider_name,
                "user_input": request.user_input,
            },
            "response": normalized,
        },
    )

    topic.current_focus = normalized["current_focus"]
    if normalized["stage"]:
        topic.stage = normalized["stage"]
    topic.next_action = normalized["next_step"]
    manager.save_topic(topic)

    topic_dir = manager.topic_dir(topic.slug)
    append_markdown_entry(
        topic_dir / "notes.md",
        f"Session {request.mode}",
        normalized["stable_insight"] + ("\n\n" + normalized["notes_markdown"] if normalized["notes_markdown"] else ""),
    )
    append_markdown_entry(
        topic_dir / "progress.md",
        f"Session {request.mode}",
        f"- Summary: {normalized['summary']}\n- Evidence: {normalized['stable_insight']}\n- Gap: {normalized['weakness_or_question']}\n- Next: {normalized['next_step']}",
    )
    guide = read_text(topic_dir / "human-guide.md")
    guide = replace_section(
        guide,
        "Start Here",
        f"- 当前阶段：{topic.stage}\n- 当前焦点：{topic.current_focus}\n- 下次第一步：{topic.next_action}",
    )
    write_text(topic_dir / "human-guide.md", guide)

    if topic.kind == "exam":
        append_markdown_entry(
            topic_dir / "qa.md",
            f"Session {request.mode}",
            normalized["weakness_or_question"] + ("\n\n" + normalized["qa_markdown"] if normalized["qa_markdown"] else ""),
        )
        if request.mode in {"solve", "review"} or normalized["solved_problems_markdown"]:
            append_markdown_entry(
                topic_dir / "solved-problems.md",
                f"Session {request.mode}",
                normalized["solved_problems_markdown"] or normalized["summary"],
            )
    else:
        append_markdown_entry(
            topic_dir / "source-log.md",
            f"Session {request.mode}",
            normalized["source_log_markdown"] or normalized["summary"],
        )
        append_markdown_entry(
            topic_dir / "conclusion.md",
            f"Session {request.mode}",
            normalized["conclusion_markdown"] or normalized["stable_insight"],
        )

    return SessionResult(
        session_id=sid,
        topic_slug=topic.slug,
        mode=request.mode,
        summary=normalized["summary"],
        stable_insight=normalized["stable_insight"],
        weakness_or_question=normalized["weakness_or_question"],
        next_step=normalized["next_step"],
        raw_response=raw_response_text,
        artifacts=[
            Artifact(name="session_markdown", path=str(markdown_path), summary="Session markdown log"),
            Artifact(name="session_json", path=str(json_path), summary="Structured session result"),
        ],
    )


def generate_review(manager: WorkspaceManager, slug: str) -> ReviewSummary:
    topic = manager.load_topic(slug)
    session_files = sorted(manager.sessions_root.rglob("*.json"), reverse=True)
    related = []
    for path in session_files:
        data = json.loads(path.read_text(encoding="utf-8"))
        if data.get("request", {}).get("topic_slug") == slug:
            related.append(data)
        if len(related) >= 5:
            break

    highlights = [item["response"]["stable_insight"] for item in related[:3]] or ["尚未有足够 session 数据。"]
    gaps = [item["response"]["weakness_or_question"] for item in related[:3]] or ["需要先启动学习 session。"]
    next_steps = [item["response"]["next_step"] for item in related[:3]] or [topic.next_action]

    summary = ReviewSummary(topic_slug=slug, highlights=highlights, gaps=gaps, next_steps=next_steps)
    weekly_path = manager.weekly_reviews_root / f"{week_str()}-{slug}.md"
    write_text(
        weekly_path,
        f"""# Weekly Review - {topic.title}

## Highlights

""" + "\n".join(f"- {item}" for item in highlights)
        + "\n\n## Gaps\n\n"
        + "\n".join(f"- {item}" for item in gaps)
        + "\n\n## Next Steps\n\n"
        + "\n".join(f"- {item}" for item in next_steps)
        + "\n",
    )
    append_markdown_entry(
        manager.topic_dir(slug) / "review.md",
        "Weekly Review",
        "\n".join(
            [
                "### Highlights",
                *[f"- {item}" for item in highlights],
                "",
                "### Gaps",
                *[f"- {item}" for item in gaps],
                "",
                "### Next Steps",
                *[f"- {item}" for item in next_steps],
            ]
        ),
    )
    return summary

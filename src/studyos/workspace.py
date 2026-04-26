from __future__ import annotations

from pathlib import Path

from .models import ProviderConfig, TopicMeta, WorkspaceConfig
from .packs import copy_builtin_packs, load_pack
from .utils import ensure_dir, load_data_file, now_iso, read_text, save_data_file, slugify, write_text


EXAM_PACK_DEFAULT = "generic-exam-pack"
RESEARCH_PACK_DEFAULT = "generic-research-pack"


class WorkspaceManager:
    def __init__(self, root: Path):
        self.root = root.resolve()

    @property
    def workspace_file(self) -> Path:
        return self.root / "workspace.yaml"

    @property
    def packs_root(self) -> Path:
        return self.root / "packs"

    @property
    def topics_root(self) -> Path:
        return self.root / "topics"

    @property
    def sessions_root(self) -> Path:
        return self.root / "sessions"

    @property
    def weekly_reviews_root(self) -> Path:
        return self.root / "reviews" / "weekly"

    def exists(self) -> bool:
        return self.workspace_file.exists()

    def require_workspace(self) -> None:
        if not self.exists():
            raise FileNotFoundError(f"Workspace not initialized: {self.workspace_file}")

    def init_workspace(self, name: str = "StudyOS Workspace") -> WorkspaceConfig:
        ensure_dir(self.root)
        for relative in [
            "topics",
            "sessions",
            "reviews/weekly",
            "packs/exams",
            "packs/research",
            "cache",
            "logs",
        ]:
            ensure_dir(self.root / relative)
        config = WorkspaceConfig(name=name)
        save_data_file(self.workspace_file, config.to_dict())
        copy_builtin_packs(self.packs_root)
        return config

    def load_config(self) -> WorkspaceConfig:
        self.require_workspace()
        return WorkspaceConfig.from_dict(load_data_file(self.workspace_file))

    def save_config(self, config: WorkspaceConfig) -> None:
        save_data_file(self.workspace_file, config.to_dict())

    def list_topics(self) -> list[TopicMeta]:
        self.require_workspace()
        return [TopicMeta.from_dict(load_data_file(path)) for path in sorted(self.topics_root.glob("*/topic.yaml"))]

    def topic_dir(self, slug: str) -> Path:
        return self.topics_root / slug

    def load_topic(self, slug: str) -> TopicMeta:
        topic_file = self.topic_dir(slug) / "topic.yaml"
        if not topic_file.exists():
            raise FileNotFoundError(f"Topic not found: {slug}")
        return TopicMeta.from_dict(load_data_file(topic_file))

    def save_topic(self, topic: TopicMeta) -> None:
        topic.updated_at = now_iso()
        save_data_file(self.topic_dir(topic.slug) / "topic.yaml", topic.to_dict())
        config = self.load_config()
        config.topics[topic.slug] = {"title": topic.title, "kind": topic.kind, "status": topic.status}
        self.save_config(config)

    def load_pack(self, kind: str, name: str):
        folder = "exams" if kind == "exam" else "research"
        return load_pack(self.packs_root, folder, name)

    def list_providers(self) -> list[ProviderConfig]:
        return self.load_config().providers

    def get_provider(self, name: str | None = None) -> ProviderConfig:
        config = self.load_config()
        provider_name = name or config.default_provider
        if provider_name is None:
            raise ValueError("No default provider configured.")
        for item in config.providers:
            if item.name == provider_name:
                return item
        raise ValueError(f"Provider not found: {provider_name}")

    def add_provider(self, provider: ProviderConfig, make_default: bool = False) -> ProviderConfig:
        config = self.load_config()
        providers = [item for item in config.providers if item.name != provider.name]
        providers.append(provider)
        config.providers = providers
        if make_default or config.default_provider is None:
            config.default_provider = provider.name
        self.save_config(config)
        return provider

    def create_topic(self, title: str, kind: str, slug: str | None = None, pack: str | None = None) -> TopicMeta:
        self.require_workspace()
        resolved_slug = slug or slugify(title)
        topic_dir = self.topic_dir(resolved_slug)
        if topic_dir.exists():
            raise FileExistsError(f"Topic already exists: {resolved_slug}")
        if kind not in {"exam", "research"}:
            raise ValueError("Topic kind must be 'exam' or 'research'.")

        pack_name = pack or (EXAM_PACK_DEFAULT if kind == "exam" else RESEARCH_PACK_DEFAULT)
        pack_data = self.load_pack(kind, pack_name)
        now = now_iso()
        topic = TopicMeta(
            slug=resolved_slug,
            title=title,
            kind=kind,
            status="active",
            stage=pack_data.default_stage,
            pack=pack_name,
            created_at=now,
            updated_at=now,
            current_focus="完成第一个学习会话",
            next_action="运行 studyos session start 启动第一次学习会话",
            tags=[kind, pack_name],
        )

        ensure_dir(topic_dir)
        self.save_topic(topic)
        self._write_topic_files(topic, pack_data.display_name, pack_data.description)
        return topic

    def _write_topic_files(self, topic: TopicMeta, pack_display_name: str, pack_description: str) -> None:
        topic_dir = self.topic_dir(topic.slug)
        write_text(
            topic_dir / "README.md",
            f"""# {topic.title}

## Overview

- Kind: {topic.kind}
- Pack: {pack_display_name}
- Status: {topic.status}
- Stage: {topic.stage}
- Current focus: {topic.current_focus}
- Next action: {topic.next_action}

## Pack Notes

{pack_description}

## Eight Week Plan

Pending. Run `studyos topic plan {topic.slug}` to generate the first version.
""",
        )
        write_text(
            topic_dir / "human-guide.md",
            f"""# {topic.title} Human Guide

## Start Here

- 当前阶段：{topic.stage}
- 当前焦点：{topic.current_focus}
- 下次第一步：{topic.next_action}

## This Week

- 完成 2 到 4 次 focused session
- 每次 session 至少沉淀一个稳定知识点
- 周末做一次 review

## Resume Fast

- 先看 `progress.md`
- 再看最近一次 session 记录
- 然后继续执行：{topic.next_action}
""",
        )
        write_text(
            topic_dir / "progress.md",
            f"""# {topic.title} Progress

## Snapshot

- Status: {topic.status}
- Stage: {topic.stage}
- Updated at: {topic.updated_at}

## Evidence Log

- 待开始第一轮学习证据沉淀

## Weak Spots

- 待识别

## Next Step

- {topic.next_action}
""",
        )
        write_text(topic_dir / "notes.md", f"# {topic.title} Notes\n")
        write_text(topic_dir / "qa.md", f"# {topic.title} QA / Error Log\n")
        write_text(topic_dir / "solved-problems.md", f"# {topic.title} Solved Problems\n")
        write_text(topic_dir / "review.md", f"# {topic.title} Review\n")
        write_text(topic_dir / "source-log.md", f"# {topic.title} Source Log\n")
        write_text(topic_dir / "conclusion.md", f"# {topic.title} Conclusion\n")

    def topic_snapshot(self, slug: str) -> dict[str, str]:
        topic_dir = self.topic_dir(slug)
        return {
            "readme": read_text(topic_dir / "README.md"),
            "human_guide": read_text(topic_dir / "human-guide.md"),
            "progress": read_text(topic_dir / "progress.md"),
            "notes": read_text(topic_dir / "notes.md"),
            "qa": read_text(topic_dir / "qa.md"),
            "solved_problems": read_text(topic_dir / "solved-problems.md"),
            "source_log": read_text(topic_dir / "source-log.md"),
            "conclusion": read_text(topic_dir / "conclusion.md"),
        }

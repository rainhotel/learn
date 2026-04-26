from __future__ import annotations

from dataclasses import asdict, dataclass, field
from typing import Any


@dataclass
class ProviderConfig:
    name: str
    kind: str
    base_url: str
    model: str
    api_key: str | None = None
    api_key_env: str | None = None

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> "ProviderConfig":
        return cls(**data)


@dataclass
class WorkspaceConfig:
    name: str
    default_provider: str | None = None
    providers: list[ProviderConfig] = field(default_factory=list)
    topics: dict[str, dict[str, str]] = field(default_factory=dict)

    def to_dict(self) -> dict[str, Any]:
        return {
            "name": self.name,
            "default_provider": self.default_provider,
            "providers": [item.to_dict() for item in self.providers],
            "topics": self.topics,
        }

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> "WorkspaceConfig":
        providers = [ProviderConfig.from_dict(item) for item in data.get("providers", [])]
        return cls(
            name=data["name"],
            default_provider=data.get("default_provider"),
            providers=providers,
            topics=data.get("topics", {}),
        )


@dataclass
class TopicMeta:
    slug: str
    title: str
    kind: str
    status: str
    stage: str
    pack: str
    created_at: str
    updated_at: str
    current_focus: str
    next_action: str
    tags: list[str] = field(default_factory=list)

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> "TopicMeta":
        return cls(**data)


@dataclass
class ExamPack:
    name: str
    kind: str
    display_name: str
    description: str
    default_stage: str
    review_cadence: str
    topic_taxonomy: list[str]
    problem_types: list[str]
    error_taxonomy: list[str]
    syllabus: list[dict[str, Any]]
    session_templates: dict[str, str] = field(default_factory=dict)

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)


@dataclass
class ResearchPack:
    name: str
    kind: str
    display_name: str
    description: str
    default_stage: str
    review_cadence: str
    phases: list[str]
    evidence_dimensions: list[str]
    question_prompts: list[str]
    syllabus: list[dict[str, Any]]
    error_taxonomy: list[str]
    session_templates: dict[str, str] = field(default_factory=dict)

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)


@dataclass
class SessionRequest:
    topic_slug: str
    mode: str
    user_input: str
    provider_name: str | None = None


@dataclass
class Artifact:
    name: str
    path: str
    summary: str


@dataclass
class SessionResult:
    session_id: str
    topic_slug: str
    mode: str
    summary: str
    stable_insight: str
    weakness_or_question: str
    next_step: str
    raw_response: str
    artifacts: list[Artifact] = field(default_factory=list)

    def to_dict(self) -> dict[str, Any]:
        return {
            "session_id": self.session_id,
            "topic_slug": self.topic_slug,
            "mode": self.mode,
            "summary": self.summary,
            "stable_insight": self.stable_insight,
            "weakness_or_question": self.weakness_or_question,
            "next_step": self.next_step,
            "raw_response": self.raw_response,
            "artifacts": [asdict(item) for item in self.artifacts],
        }


@dataclass
class ReviewSummary:
    topic_slug: str
    highlights: list[str]
    gaps: list[str]
    next_steps: list[str]

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)

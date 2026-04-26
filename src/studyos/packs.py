from __future__ import annotations

from pathlib import Path

from .models import ExamPack, ResearchPack
from .utils import load_data_file


def builtin_packs_root() -> Path:
    return Path(__file__).resolve().parent / "assets" / "packs"


def copy_builtin_packs(destination: Path) -> None:
    source_root = builtin_packs_root()
    destination.mkdir(parents=True, exist_ok=True)
    for source in source_root.rglob("*"):
        if source.is_dir():
            continue
        target = destination / source.relative_to(source_root)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(source.read_text(encoding="utf-8"), encoding="utf-8")


def _validate_pack_shape(pack_data: dict, syllabus_data: dict, error_data: dict) -> None:
    required_pack = {"name", "kind", "display_name", "description", "default_stage", "review_cadence"}
    required_syllabus = {"units"}
    required_error = {"categories"}
    missing_pack = required_pack - pack_data.keys()
    missing_syllabus = required_syllabus - syllabus_data.keys()
    missing_error = required_error - error_data.keys()
    if missing_pack or missing_syllabus or missing_error:
        raise ValueError(
            f"Invalid pack metadata. missing pack={missing_pack}, syllabus={missing_syllabus}, error={missing_error}"
        )


def _load_session_templates(pack_dir: Path) -> dict[str, str]:
    templates: dict[str, str] = {}
    templates_dir = pack_dir / "session-templates"
    if not templates_dir.exists():
        return templates
    for path in templates_dir.glob("*.md"):
        templates[path.stem] = path.read_text(encoding="utf-8")
    return templates


def load_pack(root: Path, kind: str, name: str) -> ExamPack | ResearchPack:
    pack_dir = root / kind / name
    if not pack_dir.exists():
        raise FileNotFoundError(f"Pack not found: {pack_dir}")

    pack_data = load_data_file(pack_dir / "pack.yaml")
    syllabus_data = load_data_file(pack_dir / "syllabus.yaml")
    error_data = load_data_file(pack_dir / "error-taxonomy.yaml")
    _validate_pack_shape(pack_data, syllabus_data, error_data)
    templates = _load_session_templates(pack_dir)

    if kind == "exams":
        return ExamPack(
            name=pack_data["name"],
            kind=pack_data["kind"],
            display_name=pack_data["display_name"],
            description=pack_data["description"],
            default_stage=pack_data["default_stage"],
            review_cadence=pack_data["review_cadence"],
            topic_taxonomy=pack_data.get("topic_taxonomy", []),
            problem_types=pack_data.get("problem_types", []),
            error_taxonomy=error_data["categories"],
            syllabus=syllabus_data["units"],
            session_templates=templates,
        )

    return ResearchPack(
        name=pack_data["name"],
        kind=pack_data["kind"],
        display_name=pack_data["display_name"],
        description=pack_data["description"],
        default_stage=pack_data["default_stage"],
        review_cadence=pack_data["review_cadence"],
        phases=pack_data.get("phases", []),
        evidence_dimensions=pack_data.get("evidence_dimensions", []),
        question_prompts=pack_data.get("question_prompts", []),
        syllabus=syllabus_data["units"],
        error_taxonomy=error_data["categories"],
        session_templates=templates,
    )

from __future__ import annotations

import json
import re
import unicodedata
from datetime import datetime
from pathlib import Path
from typing import Any


def now_iso() -> str:
    return datetime.now().replace(microsecond=0).isoformat()


def today_str() -> str:
    return datetime.now().strftime("%Y-%m-%d")


def week_str() -> str:
    year, week, _ = datetime.now().isocalendar()
    return f"{year}-W{week:02d}"


def session_id() -> str:
    return datetime.now().strftime("%Y%m%d-%H%M%S")


def slugify(value: str) -> str:
    normalized = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode("ascii")
    cleaned = re.sub(r"[^a-zA-Z0-9]+", "-", normalized).strip("-").lower()
    if not cleaned:
        codepoints = "-".join(f"{ord(char):x}" for char in value if not char.isspace())
        cleaned = f"item-{codepoints}".strip("-")
    if not cleaned:
        raise ValueError("Could not derive a valid slug.")
    return cleaned


def ensure_dir(path: Path) -> Path:
    path.mkdir(parents=True, exist_ok=True)
    return path


def load_data_file(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8-sig"))


def save_data_file(path: Path, data: dict[str, Any]) -> None:
    ensure_dir(path.parent)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def read_text(path: Path, default: str = "") -> str:
    if not path.exists():
        return default
    return path.read_text(encoding="utf-8-sig")


def write_text(path: Path, content: str) -> None:
    ensure_dir(path.parent)
    path.write_text(content.rstrip() + "\n", encoding="utf-8")


def append_markdown_entry(path: Path, title: str, body: str) -> None:
    content = read_text(path, default="# Log\n")
    stamp = datetime.now().strftime("%Y-%m-%d %H:%M")
    entry = f"\n## {title} ({stamp})\n\n{body.strip()}\n"
    if not content.endswith("\n"):
        content += "\n"
    write_text(path, content + entry)


def replace_section(markdown: str, heading: str, body: str) -> str:
    pattern = re.compile(
        rf"(^## {re.escape(heading)}\n)(.*?)(?=^## |\Z)",
        re.MULTILINE | re.DOTALL,
    )
    replacement = f"## {heading}\n\n{body.strip()}\n\n"
    if pattern.search(markdown):
        return pattern.sub(replacement, markdown)
    base = markdown.rstrip() + "\n\n"
    return base + replacement


def parse_json_object(raw_text: str) -> dict[str, Any] | None:
    raw_text = raw_text.strip()
    if not raw_text:
        return None
    try:
        parsed = json.loads(raw_text)
        if isinstance(parsed, dict):
            return parsed
    except json.JSONDecodeError:
        pass

    start = raw_text.find("{")
    end = raw_text.rfind("}")
    if start == -1 or end == -1 or end <= start:
        return None
    candidate = raw_text[start : end + 1]
    try:
        parsed = json.loads(candidate)
        if isinstance(parsed, dict):
            return parsed
    except json.JSONDecodeError:
        return None
    return None


def short_text(value: str, limit: int = 120) -> str:
    value = " ".join(value.split())
    if len(value) <= limit:
        return value
    return value[: limit - 3].rstrip() + "..."


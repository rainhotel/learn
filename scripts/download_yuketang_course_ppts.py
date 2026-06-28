from __future__ import annotations

import argparse
import json
import os
import re
import sys
import time
from pathlib import Path
from typing import Any

import requests


API_BASE = "https://www.yuketang.cn"
DEFAULT_CLASSROOM_ID = "30146201"
DEFAULT_OUTPUT_DIR = Path("03-outputs") / "yuketang" / "classroom-30146201-ppts"
USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/149.0.0.0 Safari/537.36"
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Download all visible PPT slide images for a Yuketang classroom."
    )
    parser.add_argument(
        "--classroom-id",
        default=DEFAULT_CLASSROOM_ID,
        help="Yuketang classroom id.",
    )
    parser.add_argument(
        "--cookie",
        default=os.environ.get("YUKETANG_COOKIE", ""),
        help="Full Cookie header value. Defaults to YUKETANG_COOKIE env var.",
    )
    parser.add_argument(
        "--csrftoken",
        default=os.environ.get("YUKETANG_CSRFTOKEN", ""),
        help="CSRF token. Defaults to YUKETANG_CSRFTOKEN env var.",
    )
    parser.add_argument(
        "--university-id",
        default=os.environ.get("YUKETANG_UNIVERSITY_ID", "0"),
        help="University id header value.",
    )
    parser.add_argument(
        "--uv-id",
        default=os.environ.get("YUKETANG_UV_ID", "0"),
        help="uv-id header value.",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=DEFAULT_OUTPUT_DIR,
        help="Directory to save downloaded slides.",
    )
    parser.add_argument(
        "--offset",
        type=int,
        default=20,
        help="Page size when fetching activity logs.",
    )
    parser.add_argument(
        "--timeout",
        type=int,
        default=30,
        help="HTTP timeout in seconds.",
    )
    parser.add_argument(
        "--sleep",
        type=float,
        default=0.15,
        help="Delay between slide downloads in seconds.",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="Re-download existing image files.",
    )
    return parser.parse_args()


def build_session(args: argparse.Namespace) -> requests.Session:
    if not args.cookie:
        raise SystemExit(
            "Missing cookie. Pass --cookie or set YUKETANG_COOKIE in the environment."
        )

    session = requests.Session()
    session.headers.update(
        {
            "accept": "application/json, text/plain, */*",
            "accept-language": "zh-CN,zh;q=0.9,en;q=0.8",
            "cache-control": "no-cache",
            "classroom-id": args.classroom_id,
            "pragma": "no-cache",
            "referer": (
                f"{API_BASE}/v2/web/studentLog/{args.classroom_id}"
                f"?university_id={args.university_id}"
                f"&platform_id=3&classroom_id={args.classroom_id}&content_url="
            ),
            "university-id": args.university_id,
            "user-agent": USER_AGENT,
            "uv-id": args.uv_id,
            "x-client": "web",
            "xt-agent": "web",
            "xtbz": "ykt",
            "Cookie": args.cookie,
        }
    )
    if args.csrftoken:
        session.headers["x-csrftoken"] = args.csrftoken
    return session


def sanitize_filename(name: str) -> str:
    name = re.sub(r"[<>:\"/\\\\|?*]", "_", name)
    name = re.sub(r"\s+", " ", name).strip()
    return name[:120].rstrip(" .") or "untitled"


def fetch_json(
    session: requests.Session,
    url: str,
    *,
    params: dict[str, Any] | None = None,
    timeout: int,
) -> dict[str, Any]:
    response = session.get(url, params=params, timeout=timeout)
    response.raise_for_status()
    data = response.json()
    if isinstance(data, dict) and data.get("code") not in (None, 0):
        raise RuntimeError(f"API error for {url}: {data}")
    if isinstance(data, dict) and data.get("errcode") not in (None, 0):
        raise RuntimeError(f"API error for {url}: {data}")
    return data


def fetch_all_activities(
    session: requests.Session,
    classroom_id: str,
    *,
    offset: int,
    timeout: int,
) -> list[dict[str, Any]]:
    activities: list[dict[str, Any]] = []
    page = 0
    while True:
        data = fetch_json(
            session,
            f"{API_BASE}/v2/api/web/logs/learn/{classroom_id}",
            params={"actype": -1, "page": page, "offset": offset, "sort": -1},
            timeout=timeout,
        )
        page_data = data["data"]
        activities.extend(page_data.get("activities", []))
        if not page_data.get("has_more"):
            break
        page += 1
    return activities


def fetch_lesson_info(
    session: requests.Session,
    lesson_id: str,
    *,
    timeout: int,
) -> dict[str, Any]:
    data = fetch_json(
        session,
        f"{API_BASE}/api/v3/classroom-report/student/lesson-info",
        params={"lesson_id": lesson_id},
        timeout=timeout,
    )
    return data["data"]


def fetch_presentation_slides(
    session: requests.Session,
    lesson_id: str,
    presentation_id: str,
    *,
    timeout: int,
) -> dict[str, Any]:
    data = fetch_json(
        session,
        f"{API_BASE}/api/v3/classroom-report/student/ppt",
        params={
            "lesson_id": lesson_id,
            "presentationId": presentation_id,
            "front_time": int(time.time() * 1000),
        },
        timeout=timeout,
    )
    return data["data"]


def download_file(
    session: requests.Session,
    url: str,
    target_path: Path,
    *,
    timeout: int,
) -> None:
    with session.get(url, stream=True, timeout=timeout) as response:
        response.raise_for_status()
        with target_path.open("wb") as file_obj:
            for chunk in response.iter_content(chunk_size=65536):
                if chunk:
                    file_obj.write(chunk)


def save_json(path: Path, payload: Any) -> None:
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def main() -> int:
    args = parse_args()
    session = build_session(args)
    output_dir = args.output_dir
    output_dir.mkdir(parents=True, exist_ok=True)

    print(f"[1/4] Fetching activities for classroom {args.classroom_id}...")
    activities = fetch_all_activities(
        session,
        args.classroom_id,
        offset=args.offset,
        timeout=args.timeout,
    )
    ppt_activities = [item for item in activities if item.get("type") == 14]
    save_json(output_dir / "activities.json", activities)
    print(
        f"Found {len(activities)} activities total, "
        f"{len(ppt_activities)} PPT-like activities."
    )

    manifest: list[dict[str, Any]] = []
    total_downloaded = 0
    total_skipped = 0

    for activity_index, activity in enumerate(ppt_activities, start=1):
        lesson_id = str(activity["courseware_id"])
        lesson_info = fetch_lesson_info(session, lesson_id, timeout=args.timeout)
        presentation_ids = [str(item) for item in lesson_info.get("presentationIds", [])]
        title = lesson_info.get("lessonName") or activity.get("title") or lesson_id
        slug = sanitize_filename(f"{activity_index:02d}-{title}")
        lesson_dir = output_dir / slug
        lesson_dir.mkdir(parents=True, exist_ok=True)
        save_json(lesson_dir / "lesson-info.json", lesson_info)

        print(
            f"[2/4] {activity_index}/{len(ppt_activities)} "
            f"{title} | lesson_id={lesson_id} | presentations={len(presentation_ids)}"
        )

        lesson_manifest = {
            "title": title,
            "lesson_id": lesson_id,
            "activity": activity,
            "presentation_ids": presentation_ids,
            "presentations": [],
        }

        for presentation_index, presentation_id in enumerate(presentation_ids, start=1):
            ppt_data = fetch_presentation_slides(
                session,
                lesson_id,
                presentation_id,
                timeout=args.timeout,
            )
            slides = ppt_data.get("slideList", [])
            visible_slides = [slide for slide in slides if slide.get("visible")]
            presentation_dir = lesson_dir / f"presentation-{presentation_index:02d}-{presentation_id}"
            presentation_dir.mkdir(parents=True, exist_ok=True)
            save_json(presentation_dir / "slides.json", slides)

            downloaded = 0
            skipped = 0
            for slide in visible_slides:
                cover_url = slide.get("cover")
                if not cover_url:
                    continue
                suffix = Path(cover_url.split("?", 1)[0]).suffix or ".jpg"
                target_path = presentation_dir / f"{int(slide['index']):03d}{suffix}"
                if target_path.exists() and not args.force:
                    skipped += 1
                    continue
                download_file(session, cover_url, target_path, timeout=args.timeout)
                downloaded += 1
                if args.sleep > 0:
                    time.sleep(args.sleep)

            total_downloaded += downloaded
            total_skipped += skipped
            lesson_manifest["presentations"].append(
                {
                    "presentation_id": presentation_id,
                    "slide_count": len(slides),
                    "visible_slide_count": len(visible_slides),
                    "downloaded": downloaded,
                    "skipped_existing": skipped,
                    "directory": str(presentation_dir),
                }
            )
            print(
                f"  - presentation {presentation_index}/{len(presentation_ids)} "
                f"visible={len(visible_slides)} downloaded={downloaded} skipped={skipped}"
            )

        save_json(lesson_dir / "manifest.json", lesson_manifest)
        manifest.append(lesson_manifest)

    save_json(output_dir / "manifest.json", manifest)
    print(f"[4/4] Done. Downloaded {total_downloaded} images, skipped {total_skipped}.")
    print(f"Output: {output_dir.resolve()}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except requests.HTTPError as exc:
        print(f"HTTP error: {exc}", file=sys.stderr)
        raise
    except requests.RequestException as exc:
        print(f"Request failed: {exc}", file=sys.stderr)
        raise

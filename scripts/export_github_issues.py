from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path
from typing import Any
from urllib import error, parse, request


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Export GitHub issues for a repository using the REST API."
    )
    parser.add_argument("--repo", required=True, help="Repository in owner/name format.")
    parser.add_argument(
        "--state",
        default="open",
        choices=["open", "closed", "all"],
        help="Issue state filter.",
    )
    parser.add_argument(
        "--label",
        action="append",
        default=[],
        help="Issue label filter. Repeat for multiple labels.",
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=100,
        help="Maximum number of issues to export.",
    )
    parser.add_argument(
        "--per-page",
        type=int,
        default=100,
        help="GitHub API page size. Maximum is 100.",
    )
    parser.add_argument(
        "--sort",
        default="updated",
        choices=["created", "updated", "comments"],
        help="Sort field.",
    )
    parser.add_argument(
        "--direction",
        default="desc",
        choices=["asc", "desc"],
        help="Sort direction.",
    )
    parser.add_argument(
        "--since",
        help="Only issues updated at or after this ISO 8601 timestamp.",
    )
    parser.add_argument(
        "--token",
        default=os.environ.get("GITHUB_TOKEN", ""),
        help="GitHub token. Defaults to GITHUB_TOKEN if present.",
    )
    parser.add_argument(
        "--output",
        required=True,
        help="Path to write normalized JSON output.",
    )
    parser.add_argument(
        "--markdown-output",
        help="Optional path to write a human-readable Markdown summary.",
    )
    parser.add_argument(
        "--include-body",
        action="store_true",
        help="Include full issue bodies in the JSON export.",
    )
    return parser.parse_args()


def build_url(args: argparse.Namespace, page: int) -> str:
    params = {
        "state": args.state,
        "page": page,
        "per_page": min(args.per_page, 100),
        "sort": args.sort,
        "direction": args.direction,
    }
    if args.label:
        params["labels"] = ",".join(args.label)
    if args.since:
        params["since"] = args.since
    return f"https://api.github.com/repos/{args.repo}/issues?{parse.urlencode(params)}"


def fetch_page(url: str, token: str) -> list[dict[str, Any]]:
    headers = {
        "Accept": "application/vnd.github+json",
        "User-Agent": "studyos-github-issue-exporter",
        "X-GitHub-Api-Version": "2022-11-28",
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"

    req = request.Request(url, headers=headers)
    with request.urlopen(req, timeout=30) as response:
        return json.loads(response.read().decode("utf-8"))


def normalize_issue(issue: dict[str, Any], include_body: bool) -> dict[str, Any]:
    body = issue.get("body") or ""
    normalized = {
        "number": issue["number"],
        "title": issue["title"],
        "state": issue["state"],
        "created_at": issue["created_at"],
        "updated_at": issue["updated_at"],
        "comments": issue["comments"],
        "author": issue["user"]["login"] if issue.get("user") else None,
        "assignees": [item["login"] for item in issue.get("assignees", [])],
        "labels": [item["name"] for item in issue.get("labels", [])],
        "html_url": issue["html_url"],
        "body_preview": " ".join(body.split())[:280],
    }
    if include_body:
        normalized["body"] = body
    return normalized


def collect_issues(args: argparse.Namespace) -> list[dict[str, Any]]:
    issues: list[dict[str, Any]] = []
    page = 1

    while len(issues) < args.limit:
        payload = fetch_page(build_url(args, page), args.token)
        if not payload:
            break

        for item in payload:
            if "pull_request" in item:
                continue
            issues.append(normalize_issue(item, args.include_body))
            if len(issues) >= args.limit:
                break

        page += 1

    return issues


def write_json(path: Path, payload: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def write_markdown(path: Path, payload: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    lines = [
        f"# GitHub Issues Export - {payload['repo']}",
        "",
        f"- State: `{payload['state']}`",
        f"- Labels: `{', '.join(payload['labels']) if payload['labels'] else 'none'}`",
        f"- Exported issues: `{len(payload['issues'])}`",
        "",
    ]

    for issue in payload["issues"]:
        labels = ", ".join(issue["labels"]) if issue["labels"] else "none"
        assignees = ", ".join(issue["assignees"]) if issue["assignees"] else "none"
        lines.extend(
            [
                f"## #{issue['number']} {issue['title']}",
                "",
                f"- URL: {issue['html_url']}",
                f"- Updated: {issue['updated_at']}",
                f"- Labels: {labels}",
                f"- Assignees: {assignees}",
                f"- Comments: {issue['comments']}",
            ]
        )
        if issue["body_preview"]:
            lines.append(f"- Body preview: {issue['body_preview']}")
        lines.append("")

    path.write_text("\n".join(lines), encoding="utf-8")


def main() -> int:
    args = parse_args()

    try:
        issues = collect_issues(args)
    except error.HTTPError as exc:
        message = exc.read().decode("utf-8", errors="replace")
        print(f"GitHub API request failed: HTTP {exc.code}\n{message}", file=sys.stderr)
        return 1
    except error.URLError as exc:
        print(f"Network error while requesting GitHub API: {exc}", file=sys.stderr)
        return 1

    payload = {
        "repo": args.repo,
        "state": args.state,
        "labels": args.label,
        "limit": args.limit,
        "sort": args.sort,
        "direction": args.direction,
        "issues": issues,
    }

    output_path = Path(args.output)
    write_json(output_path, payload)

    if args.markdown_output:
        write_markdown(Path(args.markdown_output), payload)

    print(f"Exported {len(issues)} issues from {args.repo} to {output_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

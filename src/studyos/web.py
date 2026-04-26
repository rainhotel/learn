from __future__ import annotations

import json
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

from .engine import generate_review, run_session
from .models import SessionRequest
from .workspace import WorkspaceManager


def _json_response(handler: BaseHTTPRequestHandler, payload: dict | list, status: int = 200) -> None:
    raw = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    handler.send_response(status)
    handler.send_header("Content-Type", "application/json; charset=utf-8")
    handler.send_header("Content-Length", str(len(raw)))
    handler.end_headers()
    handler.wfile.write(raw)


def _html_response(handler: BaseHTTPRequestHandler, html: str, status: int = 200) -> None:
    raw = html.encode("utf-8")
    handler.send_response(status)
    handler.send_header("Content-Type", "text/html; charset=utf-8")
    handler.send_header("Content-Length", str(len(raw)))
    handler.end_headers()
    handler.wfile.write(raw)


def _layout(title: str, body: str) -> str:
    return f"""<!doctype html>
<html lang=\"zh-CN\">
<head>
  <meta charset=\"utf-8\">
  <title>{title}</title>
  <style>
    body {{ font-family: \"Segoe UI\", sans-serif; margin: 0; background: #f5f7fb; color: #1c2430; }}
    header {{ background: linear-gradient(120deg, #0f172a, #1d4ed8); color: white; padding: 20px 24px; }}
    nav a {{ color: white; margin-right: 16px; text-decoration: none; font-weight: 600; }}
    main {{ padding: 24px; max-width: 1080px; margin: 0 auto; }}
    .card {{ background: white; border-radius: 16px; padding: 20px; box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08); margin-bottom: 20px; }}
    .grid {{ display: grid; gap: 16px; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); }}
    pre {{ white-space: pre-wrap; background: #0f172a; color: #e2e8f0; padding: 16px; border-radius: 12px; overflow-x: auto; }}
    button {{ background: #1d4ed8; color: white; border: none; border-radius: 10px; padding: 10px 14px; cursor: pointer; }}
    input, select, textarea {{ width: 100%; padding: 10px; margin-top: 6px; margin-bottom: 12px; border-radius: 10px; border: 1px solid #cbd5e1; box-sizing: border-box; }}
    textarea {{ min-height: 160px; }}
    .muted {{ color: #64748b; }}
  </style>
</head>
<body>
  <header>
    <h1 style=\"margin:0 0 12px 0;\">StudyOS</h1>
    <nav>
      <a href=\"/\">Dashboard</a>
      <a href=\"/session\">Session Runner</a>
      <a href=\"/settings\">Provider Settings</a>
    </nav>
  </header>
  <main>{body}</main>
</body>
</html>"""


def _dashboard_html(summary: dict) -> str:
    topic_cards = "".join(
        f"""
        <div class=\"card\">
          <h3 style=\"margin-top:0;\"><a href=\"/topics/{item['slug']}\">{item['title']}</a></h3>
          <p class=\"muted\">Kind: {item['kind']} | Stage: {item['stage']} | Status: {item['status']}</p>
          <p><strong>Focus:</strong> {item['current_focus']}</p>
          <p><strong>Next:</strong> {item['next_action']}</p>
        </div>
        """
        for item in summary["topics"]
    ) or '<div class="card">还没有主题，先用 CLI 创建一个 topic。</div>'

    recent = "".join(f"<li>{item}</li>" for item in summary["recent_sessions"]) or "<li>暂无 session</li>"
    body = f"""
    <div class=\"grid\">
      <div class=\"card\">
        <h2 style=\"margin-top:0;\">Workspace</h2>
        <p><strong>Name:</strong> {summary['workspace_name']}</p>
        <p><strong>Default provider:</strong> {summary['default_provider'] or '未配置'}</p>
        <p><strong>Topics:</strong> {len(summary['topics'])}</p>
      </div>
      <div class=\"card\">
        <h2 style=\"margin-top:0;\">Recent Sessions</h2>
        <ul>{recent}</ul>
      </div>
    </div>
    <div class=\"grid\">{topic_cards}</div>
    """
    return _layout("StudyOS Dashboard", body)


def _topic_html(topic: dict) -> str:
    body = f"""
    <div class=\"grid\">
      <div class=\"card\">
        <h2 style=\"margin-top:0;\">{topic['meta']['title']}</h2>
        <p class=\"muted\">Kind: {topic['meta']['kind']} | Stage: {topic['meta']['stage']} | Pack: {topic['meta']['pack']}</p>
        <p><strong>Current focus:</strong> {topic['meta']['current_focus']}</p>
        <p><strong>Next action:</strong> {topic['meta']['next_action']}</p>
      </div>
      <div class=\"card\">
        <h2 style=\"margin-top:0;\">Recent Sessions</h2>
        <ul>{''.join(f'<li>{item}</li>' for item in topic['recent_sessions']) or '<li>暂无</li>'}</ul>
      </div>
    </div>
    <div class=\"card\"><h2>README</h2><pre>{topic['readme']}</pre></div>
    <div class=\"card\"><h2>Progress</h2><pre>{topic['progress']}</pre></div>
    """
    return _layout(f"StudyOS Topic - {topic['meta']['title']}", body)


def _session_html(topics: list[dict], providers: list[dict], message: str = "") -> str:
    topic_options = "".join(f'<option value="{item["slug"]}">{item["title"]}</option>' for item in topics)
    provider_options = "".join(f'<option value="{item["name"]}">{item["name"]}</option>' for item in providers)
    body = f"""
    <div class=\"card\">
      <h2 style=\"margin-top:0;\">Run Session</h2>
      <p class=\"muted\">支持 exam 的 learn/solve/review 与 research 的 scope/read/synthesize。</p>
      <form method=\"post\" action=\"/session\">
        <label>Topic<select name=\"slug\">{topic_options}</select></label>
        <label>Mode<select name=\"mode\">
          <option value=\"learn\">learn</option>
          <option value=\"solve\">solve</option>
          <option value=\"review\">review</option>
          <option value=\"scope\">scope</option>
          <option value=\"read\">read</option>
          <option value=\"synthesize\">synthesize</option>
        </select></label>
        <label>Provider<select name=\"provider\">{provider_options}</select></label>
        <label>Input<textarea name=\"user_input\" placeholder=\"输入本次学习材料、题目或研究问题\"></textarea></label>
        <button type=\"submit\">Start Session</button>
      </form>
      <p>{message}</p>
    </div>
    """
    return _layout("StudyOS Session Runner", body)


def _settings_html(providers: list[dict]) -> str:
    body = """
    <div class=\"card\">
      <h2 style=\"margin-top:0;\">Configured Providers</h2>
      <pre>""" + json.dumps(providers, ensure_ascii=False, indent=2) + """</pre>
      <p class=\"muted\">新增 provider 建议先用 CLI：studyos provider add</p>
    </div>
    """
    return _layout("StudyOS Provider Settings", body)


def create_server(manager: WorkspaceManager, host: str = "127.0.0.1", port: int = 8765) -> ThreadingHTTPServer:
    class Handler(BaseHTTPRequestHandler):
        def do_GET(self) -> None:
            parsed = urlparse(self.path)
            if parsed.path == "/api/summary":
                _json_response(self, _workspace_summary(manager))
                return
            if parsed.path == "/api/topics":
                _json_response(self, [_topic_to_public(item) for item in manager.list_topics()])
                return
            if parsed.path.startswith("/api/topics/"):
                slug = parsed.path.split("/")[-1]
                _json_response(self, _topic_detail(manager, slug))
                return
            if parsed.path == "/api/providers":
                _json_response(self, _public_providers(manager))
                return
            if parsed.path.startswith("/topics/"):
                slug = parsed.path.split("/")[-1]
                _html_response(self, _topic_html(_topic_detail(manager, slug)))
                return
            if parsed.path == "/session":
                _html_response(self, _session_html(_workspace_summary(manager)["topics"], _public_providers(manager)))
                return
            if parsed.path == "/settings":
                _html_response(self, _settings_html(_public_providers(manager)))
                return
            if parsed.path == "/":
                _html_response(self, _dashboard_html(_workspace_summary(manager)))
                return
            self.send_error(HTTPStatus.NOT_FOUND)

        def do_POST(self) -> None:
            parsed = urlparse(self.path)
            length = int(self.headers.get("Content-Length", "0"))
            body = self.rfile.read(length).decode("utf-8")
            if parsed.path == "/api/session/start":
                payload = json.loads(body)
                result = run_session(
                    manager,
                    SessionRequest(
                        topic_slug=payload["slug"],
                        mode=payload["mode"],
                        user_input=payload["user_input"],
                        provider_name=payload.get("provider"),
                    ),
                )
                _json_response(self, result.to_dict(), 201)
                return
            if parsed.path == "/session":
                payload = parse_qs(body)
                result = run_session(
                    manager,
                    SessionRequest(
                        topic_slug=payload.get("slug", [""])[0],
                        mode=payload.get("mode", ["learn"])[0],
                        user_input=payload.get("user_input", [""])[0],
                        provider_name=payload.get("provider", [""])[0] or None,
                    ),
                )
                message = f"Session {result.session_id} completed. Next: {result.next_step}"
                _html_response(self, _session_html(_workspace_summary(manager)["topics"], _public_providers(manager), message))
                return
            if parsed.path.startswith("/api/review/"):
                slug = parsed.path.split("/")[-1]
                review = generate_review(manager, slug)
                _json_response(self, review.to_dict(), 201)
                return
            self.send_error(HTTPStatus.NOT_FOUND)

        def log_message(self, format: str, *args) -> None:
            return

    return ThreadingHTTPServer((host, port), Handler)


def serve(manager: WorkspaceManager, host: str = "127.0.0.1", port: int = 8765) -> None:
    server = create_server(manager, host=host, port=port)
    print(f"StudyOS UI available at http://{host}:{port}")
    try:
        server.serve_forever()
    finally:
        server.server_close()


def _topic_to_public(topic) -> dict:
    return {
        "slug": topic.slug,
        "title": topic.title,
        "kind": topic.kind,
        "status": topic.status,
        "stage": topic.stage,
        "pack": topic.pack,
        "current_focus": topic.current_focus,
        "next_action": topic.next_action,
    }


def _public_providers(manager: WorkspaceManager) -> list[dict]:
    providers = []
    for provider in manager.list_providers():
        providers.append(
            {
                "name": provider.name,
                "kind": provider.kind,
                "base_url": provider.base_url,
                "model": provider.model,
                "api_key_env": provider.api_key_env,
            }
        )
    return providers


def _workspace_summary(manager: WorkspaceManager) -> dict:
    config = manager.load_config()
    recent_sessions = [path.stem for path in sorted(manager.sessions_root.rglob("*.md"), reverse=True)[:5]]
    return {
        "workspace_name": config.name,
        "default_provider": config.default_provider,
        "topics": [_topic_to_public(item) for item in manager.list_topics()],
        "recent_sessions": recent_sessions,
    }


def _topic_detail(manager: WorkspaceManager, slug: str) -> dict:
    topic = manager.load_topic(slug)
    recent_sessions = []
    for path in sorted(manager.sessions_root.rglob("*.json"), reverse=True):
        data = json.loads(path.read_text(encoding="utf-8"))
        if data.get("request", {}).get("topic_slug") == slug:
            recent_sessions.append(f"{path.stem} - {data['response']['summary']}")
        if len(recent_sessions) >= 5:
            break
    snapshot = manager.topic_snapshot(slug)
    return {
        "meta": _topic_to_public(topic),
        "readme": snapshot["readme"],
        "progress": snapshot["progress"],
        "recent_sessions": recent_sessions,
    }

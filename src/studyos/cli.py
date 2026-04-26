from __future__ import annotations

import argparse
import getpass
import json
from pathlib import Path

from .engine import generate_review, generate_topic_plan, run_session
from .models import ProviderConfig, SessionRequest
from .providers import ProviderError, build_provider
from .web import serve
from .workspace import WorkspaceManager


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(prog="studyos", description="Local-first learning OS.")
    parser.add_argument("--workspace", default=".", help="Workspace path. Defaults to current directory.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    init_parser = subparsers.add_parser("init", help="Initialize a workspace.")
    init_parser.add_argument("path", nargs="?", default=None, help="Target path. Defaults to --workspace.")
    init_parser.add_argument("--name", default="StudyOS Workspace", help="Workspace display name.")

    topic_parser = subparsers.add_parser("topic", help="Topic management.")
    topic_sub = topic_parser.add_subparsers(dest="topic_command", required=True)
    topic_create = topic_sub.add_parser("create", help="Create a topic.")
    topic_create.add_argument("title")
    topic_create.add_argument("--kind", choices=["exam", "research"], required=True)
    topic_create.add_argument("--pack", default=None)
    topic_create.add_argument("--slug", default=None)

    topic_list = topic_sub.add_parser("list", help="List topics.")
    topic_list.add_argument("--json", action="store_true")

    topic_plan = topic_sub.add_parser("plan", help="Generate or refresh topic plan.")
    topic_plan.add_argument("slug")
    topic_plan.add_argument("--weeks", type=int, default=8)

    session_parser = subparsers.add_parser("session", help="Session commands.")
    session_sub = session_parser.add_subparsers(dest="session_command", required=True)
    session_start = session_sub.add_parser("start", help="Start a learning session.")
    session_start.add_argument("slug")
    session_start.add_argument("--mode", required=True)
    session_start.add_argument("--input", default=None)
    session_start.add_argument("--provider", default=None)

    session_review = session_sub.add_parser("review", help="Generate a weekly review for a topic.")
    session_review.add_argument("slug")

    provider_parser = subparsers.add_parser("provider", help="Provider management.")
    provider_sub = provider_parser.add_subparsers(dest="provider_command", required=True)
    provider_add = provider_sub.add_parser("add", help="Add a provider config.")
    provider_add.add_argument("name")
    provider_add.add_argument("--kind", choices=["ollama", "openai-compatible", "mock"], required=True)
    provider_add.add_argument("--base-url", required=True)
    provider_add.add_argument("--model", required=True)
    provider_add.add_argument("--api-key", default=None)
    provider_add.add_argument("--api-key-env", default=None)
    provider_add.add_argument("--default", action="store_true")

    provider_list = provider_sub.add_parser("list", help="List providers.")
    provider_list.add_argument("--json", action="store_true")

    provider_test = provider_sub.add_parser("test", help="Test provider connectivity.")
    provider_test.add_argument("name")

    ui_parser = subparsers.add_parser("ui", help="Run local Web UI.")
    ui_parser.add_argument("--host", default="127.0.0.1")
    ui_parser.add_argument("--port", type=int, default=8765)

    return parser


def _manager(args: argparse.Namespace) -> WorkspaceManager:
    path = args.path if getattr(args, "path", None) else args.workspace
    return WorkspaceManager(Path(path))


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    manager = _manager(args)

    try:
        if args.command == "init":
            config = manager.init_workspace(name=args.name)
            print(f"Initialized workspace at {manager.root}")
            print(f"Workspace name: {config.name}")
            return 0

        if args.command == "topic":
            if args.topic_command == "create":
                topic = manager.create_topic(args.title, args.kind, slug=args.slug, pack=args.pack)
                print(f"Created topic {topic.slug} ({topic.kind}) using pack {topic.pack}")
                return 0
            if args.topic_command == "list":
                topics = manager.list_topics()
                data = [item.to_dict() for item in topics]
                if args.json:
                    print(json.dumps(data, ensure_ascii=False, indent=2))
                else:
                    for item in topics:
                        print(
                            f"{item.slug}\t{item.kind}\t{item.stage}\t{item.status}\t{item.current_focus}\t{item.next_action}"
                        )
                return 0
            if args.topic_command == "plan":
                plan = generate_topic_plan(manager, args.slug, weeks=args.weeks)
                print(f"Generated {args.weeks}-week plan for {args.slug}")
                print(plan)
                return 0

        if args.command == "session":
            if args.session_command == "start":
                user_input = args.input
                if not user_input:
                    user_input = input("Session input: ").strip()
                result = run_session(
                    manager,
                    SessionRequest(
                        topic_slug=args.slug,
                        mode=args.mode,
                        user_input=user_input,
                        provider_name=args.provider,
                    ),
                )
                print(f"Session {result.session_id} completed.")
                print(f"Stable insight: {result.stable_insight}")
                print(f"Gap: {result.weakness_or_question}")
                print(f"Next: {result.next_step}")
                return 0
            if args.session_command == "review":
                review = generate_review(manager, args.slug)
                print(f"Review generated for {args.slug}")
                print("Highlights:")
                for item in review.highlights:
                    print(f"- {item}")
                return 0

        if args.command == "provider":
            if args.provider_command == "add":
                api_key = args.api_key
                if args.kind == "openai-compatible" and not api_key and not args.api_key_env:
                    api_key = getpass.getpass("API key (leave blank to skip): ").strip() or None
                provider = ProviderConfig(
                    name=args.name,
                    kind=args.kind,
                    base_url=args.base_url,
                    model=args.model,
                    api_key=api_key,
                    api_key_env=args.api_key_env,
                )
                manager.add_provider(provider, make_default=args.default)
                print(f"Provider {provider.name} saved.")
                return 0
            if args.provider_command == "list":
                providers = [
                    {
                        "name": item.name,
                        "kind": item.kind,
                        "base_url": item.base_url,
                        "model": item.model,
                        "api_key_env": item.api_key_env,
                    }
                    for item in manager.list_providers()
                ]
                if args.json:
                    print(json.dumps(providers, ensure_ascii=False, indent=2))
                else:
                    for item in providers:
                        print(
                            f"{item['name']}\t{item['kind']}\t{item['model']}\t{item['base_url']}\t{item['api_key_env'] or '-'}"
                        )
                return 0
            if args.provider_command == "test":
                provider_config = manager.get_provider(args.name)
                provider = build_provider(provider_config)
                result = provider.check_health()
                print(json.dumps(result, ensure_ascii=False, indent=2))
                return 0

        if args.command == "ui":
            serve(manager, host=args.host, port=args.port)
            return 0
    except (FileNotFoundError, FileExistsError, ValueError, ProviderError) as exc:
        print(f"Error: {exc}")
        return 1

    parser.print_help()
    return 1


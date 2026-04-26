import unittest

from studyos.engine import generate_review, generate_topic_plan, run_session
from studyos.models import ProviderConfig, SessionRequest
from studyos.providers import StaticProvider
from studyos.workspace import WorkspaceManager
from tests.helpers import workspace_tempdir


class EngineTests(unittest.TestCase):
    def test_plan_and_session_outputs(self) -> None:
        with workspace_tempdir() as root:
            manager = WorkspaceManager(root)
            manager.init_workspace()
            manager.create_topic("408 网络", "exam", slug="kaoyan-408-cn", pack="kaoyan-408")
            plan = generate_topic_plan(manager, "kaoyan-408-cn")
            self.assertIn("Week 1", plan)

            provider = StaticProvider(ProviderConfig(name="static", kind="ollama", base_url="http://local", model="fake"))
            result = run_session(
                manager,
                SessionRequest(topic_slug="kaoyan-408-cn", mode="learn", user_input="帮我梳理网络层重点"),
                provider=provider,
            )
            self.assertTrue(result.session_id)
            self.assertTrue(any(root.joinpath("sessions").rglob("*.json")))
            notes = (root / "topics" / "kaoyan-408-cn" / "notes.md").read_text(encoding="utf-8")
            self.assertIn("掌握了一个核心概念", notes)

            review = generate_review(manager, "kaoyan-408-cn")
            self.assertTrue(review.highlights)

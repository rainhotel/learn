import unittest

from studyos.workspace import WorkspaceManager
from tests.helpers import workspace_tempdir


class WorkspaceTests(unittest.TestCase):
    def test_init_and_create_topic(self) -> None:
        with workspace_tempdir() as root:
            manager = WorkspaceManager(root)
            config = manager.init_workspace(name="Demo")
            self.assertEqual(config.name, "Demo")
            self.assertTrue((root / "workspace.yaml").exists())
            self.assertTrue((root / "packs" / "exams" / "gaokao-math" / "pack.yaml").exists())

            topic = manager.create_topic("高考数学", "exam", slug="gaokao-math-demo", pack="gaokao-math")
            self.assertEqual(topic.slug, "gaokao-math-demo")
            self.assertTrue((root / "topics" / "gaokao-math-demo" / "topic.yaml").exists())
            self.assertIn("gaokao-math-demo", manager.load_config().topics)

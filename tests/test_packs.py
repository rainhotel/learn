import unittest

from studyos.workspace import WorkspaceManager
from tests.helpers import workspace_tempdir


class PackTests(unittest.TestCase):
    def test_pack_schema_and_loading(self) -> None:
        with workspace_tempdir() as root:
            manager = WorkspaceManager(root)
            manager.init_workspace()
            pack = manager.load_pack("exam", "gaokao-math")
            self.assertEqual(pack.kind, "exam")
            self.assertTrue(pack.syllabus)
            self.assertTrue(pack.error_taxonomy)

            research_pack = manager.load_pack("research", "generic-research-pack")
            self.assertEqual(research_pack.kind, "research")
            self.assertTrue(research_pack.phases)

import unittest

from studyos.cli import main
from tests.helpers import workspace_tempdir


class CliTests(unittest.TestCase):
    def test_cli_init_create_and_list(self) -> None:
        with workspace_tempdir() as root:
            self.assertEqual(main(["--workspace", str(root), "init", "--name", "CLI Demo"]), 0)
            self.assertEqual(
                main([
                    "--workspace",
                    str(root),
                    "topic",
                    "create",
                    "数学研究",
                    "--kind",
                    "research",
                    "--slug",
                    "math-research",
                ]),
                0,
            )
            self.assertEqual(main(["--workspace", str(root), "topic", "list", "--json"]), 0)

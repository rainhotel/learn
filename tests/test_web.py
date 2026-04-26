from http.client import HTTPConnection
import threading
import time
import unittest

from studyos.web import create_server
from studyos.workspace import WorkspaceManager
from tests.helpers import workspace_tempdir


class WebTests(unittest.TestCase):
    def test_web_api_smoke(self) -> None:
        with workspace_tempdir() as root:
            manager = WorkspaceManager(root)
            manager.init_workspace()
            manager.create_topic("通用应试", "exam", slug="generic-exam", pack="generic-exam-pack")

            server = create_server(manager, host="127.0.0.1", port=8876)
            thread = threading.Thread(target=server.serve_forever, daemon=True)
            thread.start()
            time.sleep(0.2)
            try:
                conn = HTTPConnection("127.0.0.1", 8876, timeout=5)
                conn.request("GET", "/api/summary")
                response = conn.getresponse()
                body = response.read().decode("utf-8")
                self.assertEqual(response.status, 200)
                self.assertIn("workspace_name", body)
            finally:
                server.shutdown()
                server.server_close()
                thread.join(timeout=2)

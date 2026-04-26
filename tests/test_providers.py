import unittest

from studyos.models import ProviderConfig
from studyos.providers import StaticProvider


class ProviderTests(unittest.TestCase):
    def test_static_provider_contract(self) -> None:
        provider = StaticProvider(ProviderConfig(name="static", kind="ollama", base_url="http://local", model="fake"))
        self.assertIn("summary", provider.send_structured("test"))
        self.assertEqual(provider.get_models(), ["fake"])
        self.assertTrue(provider.check_health()["ok"])

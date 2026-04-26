from __future__ import annotations

import json
import os
import urllib.error
import urllib.request
from abc import ABC, abstractmethod
from typing import Any

from .models import ProviderConfig
from .utils import parse_json_object


class ProviderError(RuntimeError):
    pass


class BaseProvider(ABC):
    def __init__(self, config: ProviderConfig):
        self.config = config

    @abstractmethod
    def send_chat(self, messages: list[dict[str, str]], options: dict[str, Any] | None = None) -> str:
        raise NotImplementedError

    @abstractmethod
    def send_structured(
        self, prompt: str, schema: dict[str, Any] | None = None, options: dict[str, Any] | None = None
    ) -> dict[str, Any]:
        raise NotImplementedError

    @abstractmethod
    def get_models(self) -> list[str]:
        raise NotImplementedError

    @abstractmethod
    def check_health(self) -> dict[str, Any]:
        raise NotImplementedError

    def _api_key(self) -> str | None:
        if self.config.api_key:
            return self.config.api_key
        if self.config.api_key_env:
            return os.getenv(self.config.api_key_env)
        return None

    def _request_json(
        self,
        method: str,
        url: str,
        payload: dict[str, Any] | None = None,
        headers: dict[str, str] | None = None,
    ) -> dict[str, Any]:
        data = None
        merged_headers = {"Content-Type": "application/json"}
        if headers:
            merged_headers.update(headers)
        if payload is not None:
            data = json.dumps(payload).encode("utf-8")
        request = urllib.request.Request(url, data=data, method=method, headers=merged_headers)
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                return json.loads(response.read().decode("utf-8"))
        except urllib.error.HTTPError as exc:
            body = exc.read().decode("utf-8", errors="replace")
            raise ProviderError(f"Provider request failed: {exc.code} {body}") from exc
        except urllib.error.URLError as exc:
            raise ProviderError(f"Provider is unavailable: {exc.reason}") from exc


class OllamaProvider(BaseProvider):
    def send_chat(self, messages: list[dict[str, str]], options: dict[str, Any] | None = None) -> str:
        payload = {"model": self.config.model, "messages": messages, "stream": False}
        response = self._request_json("POST", f"{self.config.base_url.rstrip('/')}/api/chat", payload)
        return response.get("message", {}).get("content", "")

    def send_structured(
        self, prompt: str, schema: dict[str, Any] | None = None, options: dict[str, Any] | None = None
    ) -> dict[str, Any]:
        payload = {
            "model": self.config.model,
            "messages": [{"role": "user", "content": prompt}],
            "stream": False,
            "format": "json",
        }
        response = self._request_json("POST", f"{self.config.base_url.rstrip('/')}/api/chat", payload)
        content = response.get("message", {}).get("content", "")
        parsed = parse_json_object(content)
        if parsed is None:
            raise ProviderError("Ollama structured response was not valid JSON.")
        return parsed

    def get_models(self) -> list[str]:
        response = self._request_json("GET", f"{self.config.base_url.rstrip('/')}/api/tags")
        return [item.get("name", "") for item in response.get("models", [])]

    def check_health(self) -> dict[str, Any]:
        return {"ok": True, "provider": self.config.name, "models": self.get_models()}


class OpenAICompatibleProvider(BaseProvider):
    def _headers(self) -> dict[str, str]:
        api_key = self._api_key()
        if not api_key:
            return {}
        return {"Authorization": f"Bearer {api_key}"}

    def send_chat(self, messages: list[dict[str, str]], options: dict[str, Any] | None = None) -> str:
        payload = {"model": self.config.model, "messages": messages}
        response = self._request_json(
            "POST",
            f"{self.config.base_url.rstrip('/')}/chat/completions",
            payload,
            headers=self._headers(),
        )
        return response.get("choices", [{}])[0].get("message", {}).get("content", "")

    def send_structured(
        self, prompt: str, schema: dict[str, Any] | None = None, options: dict[str, Any] | None = None
    ) -> dict[str, Any]:
        payload = {
            "model": self.config.model,
            "messages": [{"role": "user", "content": prompt}],
            "response_format": {"type": "json_object"},
        }
        response = self._request_json(
            "POST",
            f"{self.config.base_url.rstrip('/')}/chat/completions",
            payload,
            headers=self._headers(),
        )
        content = response.get("choices", [{}])[0].get("message", {}).get("content", "")
        parsed = parse_json_object(content)
        if parsed is None:
            raise ProviderError("OpenAI-compatible structured response was not valid JSON.")
        return parsed

    def get_models(self) -> list[str]:
        response = self._request_json("GET", f"{self.config.base_url.rstrip('/')}/models", headers=self._headers())
        return [item.get("id", "") for item in response.get("data", [])]

    def check_health(self) -> dict[str, Any]:
        return {"ok": True, "provider": self.config.name, "models": self.get_models()}


class StaticProvider(BaseProvider):
    def __init__(self, config: ProviderConfig, structured_response: dict[str, Any] | None = None):
        super().__init__(config)
        self.structured_response = structured_response or {
            "summary": "完成一次学习会话。",
            "stable_insight": "掌握了一个核心概念。",
            "weakness_or_question": "对边界条件仍不稳定。",
            "next_step": "继续做一组针对性练习。",
            "notes_markdown": "- 稳定知识点：核心概念已经形成。\n",
            "qa_markdown": "- 未解问题：边界条件题仍需要额外验证。\n",
            "solved_problems_markdown": "- 已完成一题样例归档。\n",
            "source_log_markdown": "- 新增一条资料来源记录。\n",
            "conclusion_markdown": "- 暂定结论已经更新。\n",
            "stage": "review",
            "current_focus": "查漏补缺",
        }

    def send_chat(self, messages: list[dict[str, str]], options: dict[str, Any] | None = None) -> str:
        return json.dumps(self.structured_response, ensure_ascii=False)

    def send_structured(
        self, prompt: str, schema: dict[str, Any] | None = None, options: dict[str, Any] | None = None
    ) -> dict[str, Any]:
        return dict(self.structured_response)

    def get_models(self) -> list[str]:
        return [self.config.model]

    def check_health(self) -> dict[str, Any]:
        return {"ok": True, "provider": self.config.name, "models": [self.config.model]}


def build_provider(config: ProviderConfig) -> BaseProvider:
    if config.kind == "ollama":
        return OllamaProvider(config)
    if config.kind == "openai-compatible":
        return OpenAICompatibleProvider(config)
    if config.kind == "mock":
        return StaticProvider(config)
    raise ProviderError(f"Unsupported provider kind: {config.kind}")



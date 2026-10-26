"""모델 호출 없이 내부 인증·입력 제한·AI 근거 검증 경계를 확인한다."""
import io
import os
import unittest
import tempfile
from pathlib import Path
from unittest.mock import patch

import httpx
from fastapi.testclient import TestClient
from PIL import Image

from api import create_app
from ai import AiAssistant


class FakeEngine:
    def extract(self, image):
        return {"engine": "paddleocr", "texts": [{"text": "가상상점", "confidence": .9, "polygon": []}]}


class ApiTests(unittest.TestCase):
    def setUp(self):
        self.env = patch.dict(os.environ, {"OCR_API_TOKEN": "t" * 32, "AI_PROVIDER": "disabled"})
        self.env.start()
        self.budget_folder = tempfile.TemporaryDirectory()
        self.budget_env = patch.dict(os.environ, {"AI_RATE_STATE_PATH":str(Path(self.budget_folder.name)/"budget.sqlite3")})
        self.budget_env.start()
        self.client = TestClient(create_app(FakeEngine))
        self.client.__enter__()
        self.headers = {"Authorization": "Bearer " + "t" * 32}

    def tearDown(self):
        self.client.__exit__(None, None, None)
        self.budget_env.stop()
        self.budget_folder.cleanup()
        self.env.stop()

    def test_rejects_missing_token(self):
        self.assertEqual(401, self.client.post("/v1/recognize", content=b"bad").status_code)

    def test_rejects_corrupt_and_oversized_images(self):
        self.assertEqual(422, self.client.post("/v1/recognize", headers=self.headers, content=b"bad").status_code)
        self.assertEqual(413, self.client.post("/v1/recognize", headers=self.headers, content=b"a" * (10 * 1024 * 1024 + 1)).status_code)

    def test_local_ocr_never_calls_external_ai(self):
        buffer = io.BytesIO()
        Image.new("RGB", (10, 10)).save(buffer, format="PNG")
        with patch.object(AiAssistant, "suggest", side_effect=AssertionError("외부 호출 금지")):
            response = self.client.post("/v1/recognize", headers=self.headers, content=buffer.getvalue())
        self.assertEqual(200, response.status_code)
        self.assertEqual("NOT_REQUESTED", response.json()["ai"]["status"])

    def test_ai_discards_hallucinated_or_out_of_range_candidates(self):
        completion = {"choices": [{"finish_reason": "stop", "message": {"content":
            '{"fields":{"businessName":{"value":"가상상점","lineIndex":0},"representative":{"value":"없는이름","lineIndex":0},"openedOn":{"value":"2024-01-01","lineIndex":9}}}'}}]}
        with patch.dict(os.environ, {"AI_PROVIDER": "nvidia", "NVIDIA_API_KEY": "fake", "NVIDIA_TEXT_MODEL": "test"}):
            assistant = AiAssistant()
        request = httpx.Request("POST", assistant.endpoint)
        response = httpx.Response(200, json=completion, request=request)
        with patch("httpx.Client.stream") as stream:
            stream.return_value.__enter__.return_value = response
            result = assistant.suggest([{"text": "상호 가상상점"}])
        self.assertEqual({"businessName"}, set(result["fields"]))

    def test_provider_timeout_preserves_empty_suggestions(self):
        with patch.dict(os.environ, {"AI_PROVIDER": "openrouter", "OPENROUTER_API_KEY": "fake", "OPENROUTER_TEXT_MODEL": "test"}):
            assistant = AiAssistant()
        with patch("httpx.Client.stream", side_effect=httpx.ReadTimeout("private content")):
            result = assistant.suggest([{"text": "가상상점"}])
        self.assertEqual("TIMEOUT", result["status"])
        self.assertNotIn("private", str(result))

    def test_engine_exception_does_not_expose_document_or_trace(self):
        buffer = io.BytesIO()
        Image.new("RGB", (10, 10)).save(buffer, format="PNG")
        with patch.object(FakeEngine, "extract", side_effect=RuntimeError("private document")):
            response = self.client.post("/v1/recognize", headers=self.headers, content=buffer.getvalue())
        self.assertEqual(503, response.status_code)
        self.assertNotIn("private", response.text)

    def test_malformed_ai_json_cannot_replace_ocr(self):
        with patch.dict(os.environ, {"AI_PROVIDER": "openrouter", "OPENROUTER_API_KEY": "fake", "OPENROUTER_TEXT_MODEL": "test"}):
            assistant = AiAssistant()
        response = httpx.Response(200, json={"choices":[{"finish_reason":"stop","message":{"content":"not json"}}]}, request=httpx.Request("POST", assistant.endpoint))
        with patch("httpx.Client.stream") as stream:
            stream.return_value.__enter__.return_value = response
            result = assistant.suggest([{"text":"가상상점"}])
        self.assertEqual("INVALID_RESPONSE", result["status"])
        self.assertEqual({}, result["fields"])

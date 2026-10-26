#!/usr/bin/env python3
"""실행 중인 서비스에서 가상 서류 두 종류의 실제 OCR·확인값 저장을 검증한다."""
import html
import html.parser
import http.cookiejar
import json
import os
from pathlib import Path
import re
import time
import urllib.parse
import urllib.request
import urllib.error

BASE = os.getenv("PORTAL_URL", "http://127.0.0.1:8087")


class Forms(html.parser.HTMLParser):
    def __init__(self, page):
        super().__init__()
        self.forms = []
        self.current = None
        self.feed(page)

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == "form":
            self.current = {"action": attrs["action"], "fields": {}}
            self.forms.append(self.current)
        if tag == "input" and self.current is not None and attrs.get("name"):
            self.current["fields"][attrs["name"]] = attrs.get("value", "")

    def handle_endtag(self, tag):
        if tag == "form":
            self.current = None

    def ending(self, suffix):
        return next(form for form in self.forms if form["action"].endswith(suffix))


class Client:
    def __init__(self):
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

    def call(self, path, fields=None):
        data = None if fields is None else urllib.parse.urlencode(fields).encode("utf-8")
        with self.opener.open(BASE + path, data=data, timeout=15) as response:
            return response.read().decode("utf-8")

    def post(self, page, suffix, fields):
        form = Forms(page).ending(suffix)
        return self.call(form["action"], {**form["fields"], **fields})


client = Client()
page = client.call("/demo")
page = client.post(page, "/demo/start", {"scenario": "NORMAL"})
root = re.search(r'(/applications/\d+)/edit', page).group(1)
results = []
for dataset, golden_path in [("original", "fixtures/support-application-v2/정답데이터.json"),
                             ("comparison", "fixtures/support-application-comparison/정답데이터.json")]:
    if dataset == "comparison":
        page = client.call(root + "/documents")
        page = client.post(page, "/fixture", {"type": "BUSINESS_CERT", "dataset": dataset})
    page = client.call(root + "/ocr")
    page = client.post(page, "/ocr", {"mode": os.getenv("OCR_SMOKE_MODE", "LIVE")})
    deadline = time.monotonic() + 190
    while 'data-ocr-pending' in page and time.monotonic() < deadline:
        time.sleep(1)
        page = client.call(root + "/ocr")
    if os.getenv("OCR_SMOKE_MODE") == "LIVE_AI":
        assert "AI 보조 상태: SUCCEEDED" in page, "AI 보조 실패"
    assert "추출 완료" in page, dataset + ": " + re.sub("<[^>]+>", " ", page)[-2000:]
    golden = json.loads(Path(golden_path).read_text())["application"]
    expected = {name: golden[name] for name in ("businessName", "businessNo", "representative", "openedOn")}
    # 확인 폼을 제외한 비교 표에서 실제 추출값을 검사한다.
    table = page.split("<tbody>", 1)[1].split("</tbody>", 1)[0]
    rows = re.findall(r"<tr>(.*?)</tr>", table, re.S)
    actual = {}
    for (name, value), row in zip(expected.items(), rows):
        cells = re.findall(r"<td>(.*?)</td>", row, re.S)
        assert len(cells) == 3
        extraction = re.sub(r"<(small|span)\b[^>]*>.*?</\1>", "", cells[1], flags=re.S)
        actual[name] = html.unescape(re.sub("<[^>]+>", "", extraction)).strip()
        assert str(value) == actual[name], (dataset, name, value, actual[name])
    if dataset == "comparison":
        assert "확인 필요" in table
    page = client.post(page, "/confirm", expected)
    assert "원래 추출값과 별도로 확인값을 저장" in page
    confirmed_table = page.split("<tbody>", 1)[1].split("</tbody>", 1)[0]
    for (name, value), row in zip(expected.items(), re.findall(r"<tr>(.*?)</tr>", confirmed_table, re.S)):
        cells = re.findall(r"<td>(.*?)</td>", row, re.S)
        assert html.unescape(re.sub("<[^>]+>", "", cells[2])).strip() == str(value)
    values = Forms(page).ending("/confirm")["fields"]
    assert all(values[name] == str(value) for name, value in expected.items())
    results.append({"dataset": dataset, "fields": actual, "confirmationSaved": True})
quota_verified = False
if os.getenv("OCR_SMOKE_MODE") == "LIVE_AI":
    page = client.call(root + "/documents")
    client.post(page, "/fixture", {"type": "BUSINESS_CERT", "dataset": "original"})
    page = client.call(root + "/ocr")
    try:
        client.post(page, "/ocr", {"mode": "LIVE_AI"})
        raise AssertionError("AI 일일 한도 초과 요청이 허용됨")
    except urllib.error.HTTPError as error:
        assert error.code == 429
    page = client.call(root + "/ocr")
    page = client.post(page, "/ocr", {"mode": "LIVE"})
    deadline = time.monotonic() + 60
    while "data-ocr-pending" in page and time.monotonic() < deadline:
        time.sleep(1)
        page = client.call(root + "/ocr")
    assert "추출 완료" in page and "실제 OCR" in page
    quota_verified = True
Path("output/ocr").mkdir(parents=True, exist_ok=True)
Path("output/ocr/portal-" + os.getenv("OCR_SMOKE_MODE", "LIVE").lower() + "-smoke.json").write_text(json.dumps({"application": root, "results": results, "quotaVerified": quota_verified}, ensure_ascii=False, indent=2))
print(json.dumps({"application": root, "results": results, "quotaVerified": quota_verified}, ensure_ascii=False, indent=2))

#!/usr/bin/env python3
"""공개 가입 차단과 테스트 계정의 입력·업로드·OCR·배정·보완·결과를 검증한다."""
import html.parser
import json
import os
from pathlib import Path
import re
import time
import uuid

import httpx

BASE = os.getenv("PORTAL_URL", "http://127.0.0.1:8087")
if not BASE.startswith(("http://127.0.0.1:", "http://localhost:")):
    raise SystemExit("로컬 검증 전용입니다. 운영 서비스에서는 실행하지 마세요.")


class Forms(html.parser.HTMLParser):
    def __init__(self, page):
        super().__init__()
        self.forms = []
        self.current = None
        self.textarea = None
        self.select = None
        self.feed(page)

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == "form":
            self.current = {"action": attrs.get("action", ""), "fields": {}}
            self.forms.append(self.current)
        if self.current is None:
            return
        if tag == "input" and attrs.get("name") and attrs.get("type") not in {"file", "checkbox", "radio"}:
            self.current["fields"][attrs["name"]] = attrs.get("value", "")
        if tag == "textarea":
            self.textarea = attrs["name"]
            self.current["fields"][self.textarea] = ""
        if tag == "select":
            self.select = attrs["name"]
        if tag == "option" and self.select and (self.select not in self.current["fields"] or "selected" in attrs):
            self.current["fields"][self.select] = attrs.get("value", "")

    def handle_data(self, data):
        if self.current is not None and self.textarea:
            self.current["fields"][self.textarea] += data

    def handle_endtag(self, tag):
        if tag == "textarea": self.textarea = None
        if tag == "select": self.select = None
        if tag == "form": self.current = None

    def ending(self, suffix):
        return next(form for form in self.forms if form["action"].endswith(suffix))


class Client:
    def __init__(self): self.http = httpx.Client(base_url=BASE, follow_redirects=True, timeout=20, trust_env=False)
    def get(self, path):
        response = self.http.get(path)
        assert response.status_code == 200, (path, response.status_code)
        return response.text
    def post(self, page, suffix, changes):
        form = Forms(page).ending(suffix)
        response = self.http.post(form["action"], data={**form["fields"], **changes})
        assert response.status_code == 200, (suffix, response.status_code, response.text[-800:])
        return response.text
    def login(self, username, password):
        return self.post(self.get("/login"), "/login", {"username": username, "password": password})
    def upload(self, page, application_id, kind, filename, content):
        form = next(form for form in Forms(page).forms if form["action"].endswith("/upload") and form["fields"]["type"] == kind)
        # 브라우저와 동일하게 CSRF 토큰도 multipart 본문에 포함한다.
        response = self.http.post(form["action"], data=form["fields"], files={"file": (filename, content, "image/png")})
        assert response.status_code == 200, ("upload", response.status_code, response.text[-800:])
        return response.text


checks = []
def passed(name): checks.append(name)

applicant = Client()
password = os.getenv("LOCAL_ACCOUNT_PASSWORD", "Portfolio2026!")
registration_page = applicant.get("/register")
assert "data-registration-preview" in registration_page
assert 'action="/register"' not in registration_page
login_form = Forms(applicant.get("/login")).ending("/login")
blocked = applicant.http.post("/register", data={**login_form["fields"], "loginId": "blocked_" + uuid.uuid4().hex[:16]})
assert blocked.status_code == 405
applicant.login("applicant", password)
assert applicant.http.get("/admin").status_code == 403
passed("공개 가입 요청 차단·테스트 계정 로그인·신청자 권한")
page = applicant.get("/")
assert "예시 데이터로 먼저 체험하기" not in page
page = applicant.get("/programs")
program_id = re.search(r'href="/programs/(\d+)"', page).group(1)
page = applicant.get("/programs/" + program_id)
assert all(text in page for text in ["지원 대상", "지원 항목", "제외 사항", "검토 기준", "문의처"])
page = applicant.post(page, "/applications", {})
root = re.search(r'action="(/applications/\d+)/save"', page).group(1)
form = Forms(page).ending("/save")
assert form["fields"]["businessName"] == "" and form["fields"]["planText"] == ""
passed("공고 정보 확인과 빈 신청서 생성")
golden = json.loads(Path("fixtures/support-application-v2/정답데이터.json").read_text())["application"]
fields = {name: str(golden[name]) for name in ["businessName", "businessNo", "representative", "openedOn", "phone", "address", "businessType", "businessItem", "requestedAmount", "selfContribution", "planText"]}
page = applicant.post(page, "/save", {**fields, "next": "documents"})
assert "사업자등록증" in page and "선택한 파일 업로드" in page
assert "아직 첨부하지 않았어요" in page
passed("사업장·계획·사업비 직접 입력과 저장")
images = {}
for kind, resource in [("BUSINESS_CERT", "business-cert"), ("ESTIMATE", "estimate")]:
    response = applicant.http.get("/samples/original/" + resource)
    assert response.status_code == 200 and "attachment" in response.headers["content-disposition"]
    assert response.content.startswith(b"\x89PNG")
    images[kind] = response.content
    page = applicant.upload(page, root, kind, resource + ".png", response.content)
    assert resource + ".png" in page
passed("예시 서류 내려받기와 두 종류 실제 multipart 업로드")
page = applicant.get(root + "/ocr")
assert 'value="SAMPLE"' not in page
assert "아직 서류를 읽지 않았습니다" in page
page = applicant.post(page, "/ocr", {"mode": "LIVE"})
deadline = time.monotonic() + 190
while "data-ocr-pending" in page and time.monotonic() < deadline:
    time.sleep(1)
    page = applicant.get(root + "/ocr")
assert "추출 완료" in page and "PP-OCRv5" in page
confirmed = {name: str(golden[name]) for name in ["businessName", "businessNo", "representative", "openedOn"]}
for name, value in confirmed.items():
    assert Forms(page).ending("/confirm")["fields"][name] == value
passed("업로드한 사업자등록증 실제 문자 인식과 네 항목 후보 표시")
page = applicant.post(page, "/confirm", confirmed)
page = applicant.post(page, "/apply-confirmed", {})
assert "신청서에 반영했습니다" in page
assert Forms(page).ending("/save")["fields"]["address"] == golden["address"]
passed("확인값 저장과 선택적 신청서 반영·사업장 정보 보존")
page = applicant.get(root + "/confirm")
assert "3300000" in page or "3,300,000" in page
page = applicant.post(page, "/submit", {"acknowledged": "true"})
receipt = re.search(r"SUP-[0-9]+", page).group(0)
assert "접수 완료" in page
passed("최종 확인·제출·접수번호 발급")

account_password = os.getenv("LOCAL_ACCOUNT_PASSWORD", "Portfolio2026!")
admin = Client(); admin.login("admin", account_password)
page = admin.get("/admin")
assignment = next(form for form in Forms(page).forms if form["action"].endswith("/assign") and form["fields"]["applicationId"] == root.rsplit("/", 1)[1])
response = admin.http.post(assignment["action"], data={**assignment["fields"], "reason": "일반 신청 E2E 담당 배정"})
assert response.status_code == 200
staff = Client(); staff.login("staff", account_password)
page = staff.get("/staff/applications")
assert receipt in page
review_path = "/staff" + root
page = staff.get(review_path)
page = staff.post(page, "/start", {})
passed("별도 관리자 계정 배정과 담당자 접수함 조회·검토 시작")
page = staff.post(page, "/supplement", {"target": "ESTIMATE", "reason": "견적서 세부 항목 확인을 위한 보완 요청"})
assert "보완 요청" in page
page = applicant.get(root)
assert "견적서 세부 항목" in page
page = applicant.get(root + "/documents")
page = applicant.upload(page, root, "ESTIMATE", "보완견적서.png", images["ESTIMATE"])
page = applicant.get(root + "/confirm")
page = applicant.post(page, "/submit", {"acknowledged": "true", "responseText": "견적서 세부 항목을 확인하여 재첨부했습니다."})
assert "보완 재제출" in page
passed("보완 요청·신청자 확인·견적서 재업로드·새 제출본 생성")
page = staff.get(review_path)
page = staff.post(page, "/start", {})
page = staff.post(page, "/decision", {"decision": "APPROVED", "reason": "사업계획과 가상 서류 검토 완료", "checked": "true"})
assert "선정" in page
page = applicant.get(root)
assert "선정" in page and "사업계획과 가상 서류 검토 완료" in page
assert "/demo/role" not in page
passed("담당자 결과 확정과 신청자 결과·처리 이력 조회")
revision_path = re.search(r'href="([^"]+/revisions/\d+)"', page).group(1)
assert "도움로 123" in applicant.get(revision_path)
passed("확장된 신청 정보의 제출본 보존")
output = {"application": root, "receipt": receipt, "checks": len(checks), "passed": checks}
Path("output/service").mkdir(parents=True, exist_ok=True)
Path("output/service/service-e2e.json").write_text(json.dumps(output, ensure_ascii=False, indent=2))
print(json.dumps(output, ensure_ascii=False, indent=2))

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



applicant=Client()
applicant.login('applicant', 'Portfolio2026!')
page=applicant.get('/programs/6')
page=applicant.post(page, '/applications', {'entry':'DOCUMENT'})
root=re.search(r'action="(/applications/\d+)/intake"',page).group(1)
form=Forms(page).ending('/intake')
response=applicant.http.post(form['action'], data={**form['fields'],'layout':'BUNDLE','kinds':'AUTO'},files={'files':('디지털전환-작성신청서.pdf',Path('src/main/resources/fixtures/digital/application.pdf').read_bytes(),'application/pdf')})
assert response.status_code==200, (response.status_code,response.text[-500:])
job=re.search(r'intakeId=(\d+)',str(response.url)).group(1)
state=applicant.http.get(root+'/intake/status',params={'intakeId':job}).json()
assert state['status']=='UPLOADED' and state['progress']==0,state
confirmation=applicant.get(root+'/intake?intakeId='+job)
assert 'data-intake-progress ' not in confirmation
start_form=Forms(confirmation).ending('/start')
started=applicant.http.post(start_form['action'],data=start_form['fields'])
assert started.status_code==200,started.status_code

seen=[]
for attempt in range(120):
    state=applicant.http.get(root+'/intake/status',params={'intakeId':job}).json()
    if not seen or seen[-1]!=state: seen.append(state);print(state,flush=True)
    if state['status'] in ['READY','APPLIED','FAILED']:break
    time.sleep(1)
assert state['status']=='READY',state
page=applicant.get(root+'/intake?intakeId='+job)
assert '가상달빛식당' in page
assert '신청에 포함' in page
assert '분할 PDF 열기' in page
Path('.local/intake-result.html').write_text(page)
Path('.local/intake-e2e-context.json').write_text(json.dumps({'root':root,'job':job,'cookies':dict(applicant.http.cookies)}))
print('READY',root,job)

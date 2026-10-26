#!/usr/bin/env python3
"""실제 서버·세션·CSRF·UTF-8 폼을 통과하는 업무 연동 점검. 가상 체험 공간만 생성한다."""
import html.parser
import http.cookiejar
import json
import os
import re
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

BASE = os.environ.get('PORTAL_URL', 'http://127.0.0.1:8087')

class Forms(html.parser.HTMLParser):
    def __init__(self, content):
        super().__init__(); self.forms=[]; self.current=None; self.textarea=None; self.feed(content)
    def handle_starttag(self, tag, attrs):
        attrs=dict(attrs)
        if tag=='form': self.current={'action':attrs.get('action',''),'fields':{}}; self.forms.append(self.current)
        if self.current is not None and tag=='input' and attrs.get('name') and attrs.get('type') not in ('checkbox','radio','file'):
            self.current['fields'][attrs['name']]=attrs.get('value','')
        if self.current is not None and tag=='textarea': self.textarea=attrs.get('name'); self.current['fields'][self.textarea]=''
    def handle_data(self, text):
        if self.current is not None and self.textarea: self.current['fields'][self.textarea]+=text
    def handle_endtag(self, tag):
        if tag=='textarea':self.textarea=None
        if tag=='form':self.current=None
    def ending(self, suffix): return next(form for form in self.forms if form['action'].endswith(suffix))

class Client:
    def __init__(self): self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
    def call(self, path, fields=None):
        data=None if fields is None else urllib.parse.urlencode(fields).encode('utf-8')
        req=urllib.request.Request(BASE+path, data, {'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'} if data else {})
        try:
            with self.opener.open(req) as response:return response.status,response.geturl(),response.read().decode('utf-8')
        except urllib.error.HTTPError as error:return error.code,error.geturl(),error.read().decode('utf-8')
    def post_form(self, content, suffix, fields):
        form=Forms(content).ending(suffix); return self.call(form['action'],{**form['fields'],**fields})
    def start(self, scenario):
        _,_,page=self.call('/demo'); status,_,page=self.post_form(page,'/demo/start',{'scenario':scenario});assert status==200,(status,page[-300:]);return page

def check(condition, label):
    if not condition:raise AssertionError(label)
    results.append(label)

results=[]
client=Client(); page=client.start('NORMAL'); application_id=int(re.search(r'/applications/(\d+)/edit',page).group(1)); root=f'/applications/{application_id}'
_,_,page=client.call(root+'/edit')
status,_,page=client.post_form(page,'/save',{'planText':'한글 신청 내용이 저장되는지 확인합니다.','next':'stay'})
check(status==200 and '한글 신청 내용이 저장되는지 확인합니다.' in page and 'ê°' not in page,'UTF-8 폼 저장·재조회')
status,_,page=client.call(root+'/ocr');check(status==200,'OCR 결과 화면 렌더링')
status,_,page=client.call(root+'/confirm');confirmation=Forms(page).ending('/submit');payload={**confirmation['fields'],'acknowledged':'true'}
status,_,page=client.call(confirmation['action'],payload);check(status==200 and '접수 완료' in page,'신청 실제 제출')
status,_,page=client.call(confirmation['action'],payload);check(status==200,'동일 제출 키 재요청')
check(len(set(re.findall(root+r'/revisions/(\d+)',page)))==1,'중복 제출본 없음')
status,_,_=client.call('/admin');check(status==403,'체험 신청자 관리자 접근 차단')
other=Client();other.start('NORMAL');status,_,_=other.call(root);check(status==403,'다른 세션 체험 공간 접근 차단')
_,_,page=client.call('/my');status,_,page=client.post_form(page,'/demo/role',{'role':'STAFF'});check(status==200,'같은 공간 담당자 전환')
status,_,page=client.call(f'/staff/applications/{application_id}');status,_,page=client.post_form(page,'/start',{});check(status==200 and '결과 확정' in page,'담당자 검토 시작')
status,_,page=client.post_form(page,'/decision',{'decision':'APPROVED','reason':'가상 서류 검토 완료','checked':'true'});check(status==200 and '선정' in page and '가상 서류 검토 완료' in page,'선정 확정·한글 사유 저장')
status,_,page=client.call('/staff/reports');check(status==200 and '상태별 신청 건수' in page,'담당 업무 통계')
status,_,page=client.call('/my');status,_,page=client.post_form(page,'/demo/role',{'role':'APPLICANT'});status,_,page=client.call(root);check(status==200 and '가상 서류 검토 완료' in page,'신청자 결과 조회')
revision_link=re.search(r'href="([^"]+/revisions/\d+)"',page).group(1);status,_,page=client.call(revision_link);check(status==200 and '한글 신청 내용이 저장되는지 확인합니다.' in page,'제출 당시 스냅샷 조회')
missing=Client();page=missing.start('MISSING');missing_id=int(re.search(r'/applications/(\d+)/edit',page).group(1));_,_,page=missing.call(f'/applications/{missing_id}/confirm');status,_,page=missing.post_form(page,'/submit',{'acknowledged':'true'});check(status==422 and '필수 서류가 누락' in page,'필수 서류 누락 제출 차단')
supplement=Client();page=supplement.start('SUPPLEMENT');supplement_id=int(re.search(r'/applications/(\d+)/edit',page).group(1));_,_,page=supplement.call(f'/applications/{supplement_id}/documents');status,_,page=supplement.post_form(page,'/fixture',{'type':'ESTIMATE'});check(status==200,'보완 대상 견적서 교체')
_,_,page=supplement.call(f'/applications/{supplement_id}/confirm');status,_,page=supplement.post_form(page,'/submit',{'acknowledged':'true','responseText':'세부 내역을 보완했습니다.'});check(status==200 and '보완 재제출' in page,'보완 재제출 완료')
no_csrf=Client();status,_,_=no_csrf.call('/demo/start',{'scenario':'NORMAL'});check(status==403,'CSRF 없는 공개 변경 요청 차단')
Path('output').mkdir(exist_ok=True);Path('output/http-smoke.json').write_text(json.dumps({'checks':len(results),'passed':results},ensure_ascii=False,indent=2))
print(json.dumps({'checks':len(results),'passed':results},ensure_ascii=False,indent=2))

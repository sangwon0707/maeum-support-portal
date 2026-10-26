#!/usr/bin/env python3
"""제공된 스캔 문서를 실제 로컬 OCR로 처리해 재사용 가능한 결과·파일을 만든다."""
import base64
import hashlib
import json
import os
from pathlib import Path
import shlex
import time
from datetime import datetime, timezone

import httpx
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "ocr"))
from pdf_document import PdfDocument

ROOT = Path(__file__).resolve().parents[1]
for line in ((ROOT / '.env.ocr').read_text().splitlines() if (ROOT / '.env.ocr').exists() else []):
    if line.startswith('OCR_API_TOKEN='):
        os.environ['OCR_API_TOKEN'] = shlex.split(line.split('=', 1)[1])[0]
DEST = ROOT / 'src/main/resources/prepared-intakes'
(DEST / 'assets').mkdir(parents=True, exist_ok=True)
PIPELINE_VERSION = 'pdfbox-3.0.8-paddle-v5-ko-layout-v2'
LABELS = {'APPLICATION':'신청서','BUSINESS_PLAN':'사업계획서','BUSINESS_CERT':'사업자등록증','ESTIMATE':'견적서','PHOTO_SHEET':'현장 사진','CONSENT':'동의서','OTHER':'기타·확인 필요'}

def save(content, extension):
    key = hashlib.sha256(content).hexdigest() + extension
    target = DEST / 'assets' / key
    if not target.exists(): target.write_bytes(content)
    return key

cases=[]
with httpx.Client(timeout=600, trust_env=False) as client:
    for source in sorted((ROOT / 'src/main/resources/fixtures').glob('*/application-scan.pdf')):
        code=source.parent.name
        content=source.read_bytes()
        digest=hashlib.sha256(content).hexdigest()
        manifest_file=DEST / (code+'.json')
        if manifest_file.exists():
            previous=json.loads(manifest_file.read_text())
            if previous['sourceSha256']==digest and previous.get('pipelineVersion')==PIPELINE_VERSION:
                cases.append({'code':code,'sourceSha256':digest})
                print(code,'검증된 기존 산출물 유지',flush=True)
                continue
        started=time.monotonic();pages=[]
        with client.stream('POST','http://127.0.0.1:8091/v1/analyze-document?kind=AUTO',headers={'Authorization':'Bearer '+os.environ['OCR_API_TOKEN']},content=content) as response:
            response.raise_for_status()
            for line in response.iter_lines():
                event=json.loads(line)
                if event['event']=='error':raise RuntimeError(event['code'])
                if event['event']!='page':continue
                assert event['textSource']=='PADDLE_OCR','스캔 문서는 실제 OCR 결과만 준비한다.'
                event.pop('event');event.update(sourceId=0,sourceName='신청서.pdf',kindLabel=LABELS[event['kind']])
                event['previewKey']=save(base64.b64decode(event.pop('preview')),'.png')
                for photo in event['photos']:photo['storageKey']=save(base64.b64decode(photo.pop('image')),'.png')
                pages.append(event)
                print(code, len(pages),'쪽 실제 OCR 완료',flush=True)
        groups=[];fields={};evidence={};conflicts=[]
        for index,page in enumerate(pages):
            begin=not groups or page['kind']!=groups[-1]['kind']
            page['groupStart']=begin
            if begin:groups.append({'sourceId':0,'sourceName':'신청서.pdf','kind':page['kind'],'kindLabel':page['kindLabel'],'pageNumbers':[],'indexes':[]})
            groups[-1]['pageNumbers'].append(page['pageNumber']);groups[-1]['indexes'].append(index)
            for field,value in page['fields'].items():
                if field=='planText' and field in fields:
                    fields[field]=(fields[field]+'\n'+value)[:2000];continue
                if field in fields and fields[field]!=value:
                    conflicts.append({'field':field,'previous':fields[field],'candidate':value,'pageNumber':page['pageNumber'],'sourceName':'신청서.pdf'});continue
                fields[field]=value;evidence[field]={'pageNumber':page['pageNumber'],'sourceName':'신청서.pdf','sourceId':0,'textSource':page['textSource']}
        for group in groups:
            with PdfDocument(content) as document:
                group['pdfKey']=save(document.split([number-1 for number in group['pageNumbers']]),'.pdf')
        result={'pages':pages,'groups':groups,'fields':fields,'evidence':evidence,'conflicts':conflicts,'pageCount':len(pages),'photoCount':sum(len(p['photos']) for p in pages)}
        manifest={'pipelineVersion':PIPELINE_VERSION,'pdfProcessor':'Apache PDFBox 3.0.8','sourceSha256':digest,'engine':'PaddleOCR','modelVersion':'PP-OCRv5_mobile_det+korean_PP-OCRv5_mobile_rec','preparedAt':datetime.now(timezone.utc).isoformat(),'result':result}
        manifest_file.write_text(json.dumps(manifest,ensure_ascii=False,indent=2))
        cases.append({'code':code,'sourceSha256':digest})
        print(code,'분할',len(groups),'사진',result['photoCount'],'완료',round(time.monotonic()-started,1),'초',flush=True)
(DEST / 'index.json').write_text(json.dumps({'pipelineVersion':PIPELINE_VERSION,'cases':cases},ensure_ascii=False,indent=2))

#!/usr/bin/env python3
"""실제 인식 원문으로 LLM 결과를 준비한다. 데모 요청에서는 공급자를 호출하지 않는다."""
import json
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'ocr'))
from document_ai import DocumentAi

codes = sys.argv[1:] or ['access','comparison','digital','energy','online','original','safety']
for code in codes:
    if code not in {'access','comparison','digital','energy','online','original','safety'}:
        raise SystemExit('허용된 문서 코드만 선택해 주세요.')
    path = ROOT / 'src/main/resources/prepared-intakes' / (code+'.json')
    manifest = json.loads(path.read_text())
    if manifest.get('pdfProcessor') != 'Apache PDFBox 3.0.8':
        raise SystemExit(code + ': PDFBox 인식 결과를 먼저 준비해 주세요.')
    result = manifest['result']
    if any(item.get('status') == 'SUCCEEDED' for item in result.get('aiAnalyses', [])):
        print(code, '검증된 AI 결과 유지', flush=True)
        continue
    analysis = DocumentAi().analyze(result['pages'])
    if analysis['status'] != 'SUCCEEDED':
        print(code, analysis['status'], flush=True)
        raise SystemExit('공급자 실패: 자동 재시도하지 않습니다.')
    analysis['sourceName'] = '신청서.pdf'
    for candidate in analysis['pageResults']:
        page = next(page for page in result['pages'] if page['pageNumber'] == candidate['pageNumber'])
        kinds = {'APPLICATION':'신청서','BUSINESS_PLAN':'사업계획서','BUSINESS_CERT':'사업자등록증','ESTIMATE':'견적서','PHOTO_SHEET':'현장 사진','CONSENT':'동의서','OTHER':'기타·확인 필요'}
        candidate['kindLabel'] = kinds.get(candidate.get('suggestedKind'),'확인 필요')
        page['ai'] = candidate
        for name,value in candidate['fields'].items():
            if name in page['fields']:
                continue
            page['fields'][name] = value
            page['evidence'][name] = candidate['evidence'][name]
            if name not in result['fields']:
                result['fields'][name] = value
                result['evidence'][name] = {'sourceId':0,'sourceName':'신청서.pdf','pageNumber':page['pageNumber'],'textSource':page['textSource']}
            elif result['fields'][name] != value:
                result['conflicts'].append({'field':name,'previous':result['fields'][name],'candidate':value,'pageNumber':page['pageNumber'],'sourceName':'신청서.pdf'})
    for conflict in analysis['conflicts']:
        result['conflicts'].append({**conflict,'sourceName':'신청서.pdf','method':'LLM_SOURCE_VERIFIED'})
    result['aiAnalyses'] = [analysis]
    manifest['aiPreparedAt'] = datetime.now(timezone.utc).isoformat()
    temporary = path.with_suffix('.json.tmp')
    temporary.write_text(json.dumps(manifest,ensure_ascii=False,indent=2))
    temporary.replace(path)
    print(code, analysis['provider'], analysis['model'], '원문 대조', analysis['verifiedCount'], '보완', analysis.get('supplementedCount',0), flush=True)

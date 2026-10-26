#!/usr/bin/env python3
"""동일 서식에 대체 입력값을 적용한 정상 비교 사례를 페이지 이미지로 만든다."""
import copy
import hashlib
import importlib.util
import json
from pathlib import Path

from pypdf import PdfReader
from reportlab.platypus import PageBreak, SimpleDocTemplate

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('approved_form',ROOT/'scripts/build-application-v2.py')
form=importlib.util.module_from_spec(spec)
spec.loader.exec_module(form)
DIRECTORY=ROOT/'fixtures/support-application-comparison'
OUTPUT=ROOT/'output/images/지원사업_비교용'

# 서식 제목·항목·정책은 유지하고 작성된 값만 교체한다.
REPLACEMENTS={
 'TEST-APP-2026-001':'TEST-APP-2026-002','TEST-Q-2026-001':'TEST-Q-2026-002',
 '가상상점':'가상문구점','가상대표':'예시대표','가상시공자':'예시설치자','가상간판공방':'예시간판제작소',
 '000-00-00000':'000-00-00002','000-00-00001':'000-00-00003',
 '2026.10.23':'2026.11.02','2026년 10월 23일':'2026년 11월 02일',
 '2026.10.20':'2026.10.29','2026년 10월 20일':'2026년 10월 29일',
 '2026.11.20':'2026.11.29','2024.01.01':'2023.06.15','2024년 01월 01일':'2023년 06월 15일',
 '000-0000-0000':'000-0000-0002','가상시 예시구 도움로 123, 1층':'예시시 가상구 배움로 45, 1층',
 '생활잡화 소매':'문구용품 소매','생활잡화와 주방·청소용품':'문구류와 학습·미술용품',
 '매장 간판·조명 및 배선 환경개선':'문구점 간판·조명 및 배선 환경개선',
 '3,300,000':'2,640,000','3,000,000':'2,400,000','300,000':'240,000',
 'LED 조명 10개':'LED 조명 8개',
 '2026 소상공인 매장 환경개선 지원사업':'2026 소상공인 매장 환경개선 지원사업',
 '선정 통보 후 1~7일':'선정 통보 후 1~5일','8~21일':'6~18일','22~30일':'19~25일',
 '30일':'25일',
 '기존 간판 철거 및 LED 간판 설치':'기존 간판 철거 및 문구점 LED 간판 설치',
 '표면 노후 및 글자 퇴색':'표면 변색 및 안내 글자 식별 저하',
 '출입구 정면 / 기존 간판이 보이는 구도':'도로 맞은편 정면 / 문구점 간판 확인 구도',
 '출입구에서 진열 통로·천장 조명을 보는 구도':'문구 진열 통로에서 천장 조명을 확인하는 구도',
 '방문 고객이 출입구를 쉽게 찾고':'학생과 보호자가 입구를 쉽게 찾고',
 '출입구 식별과 진열 상품 확인':'문구점 입구 식별과 학습용품 확인',
}

REPLACEMENTS.update({
 '가상상점은 생활잡화와 주방·청소용품을 판매하는 가상 소매점입니다. 고객이 입구를 쉽게 찾고 진열 상품을 확인할 수 있는 매장 환경을 목표로 합니다. 상호·소재지·개업일은 신청서 및 가상 증빙과 동일하게 작성했습니다.':
 '가상문구점은 학생과 지역 주민을 대상으로 학습 준비물, 필기구, 미술 재료를 판매하는 가상 매장입니다. 품목별 진열 위치를 쉽게 찾고 색상과 규격을 비교할 수 있는 구매 환경을 마련하고자 합니다. 신청 사업자 정보는 첨부 증빙과 동일하게 기재했습니다.',
 '외부 간판은 표면이 낡고 글자가 바래 시인성 개선이 필요합니다. 내부는 천장 조명과 진열 통로의 밝기가 고르지 않은 상황을 가정했습니다.':
 '도로에서 문구점 위치를 알아보기 어려운 낡은 간판과 어두운 학습용품 진열 구역을 개선 대상으로 정했습니다. 야간 방문 고객의 입구 식별과 작은 상품의 표시 확인을 돕는 것이 목적입니다.',
 '기존 간판 철거 및 LED 간판 설치, 천장 LED 조명 10개 교체를 계획합니다. 배선은 사진으로 상태를 판정하지 않고 시공 전 별도 점검 후 정비 범위를 확인합니다. 모든 시공 내용은 실제 작업이 없는 테스트 계획입니다.':
 '출입구 간판을 새 LED 간판으로 바꾸고 진열 통로의 조명 8개를 교체합니다. 전기 회로는 작업 전 점검한 뒤 필요한 부분을 정비할 계획입니다. 실제 공사는 진행하지 않는 비교용 테스트 사례입니다.',
 '출입구 식별과 진열 상품 확인이 쉬워지는 것을 목표로 합니다. 매출 증가율은 임의로 제시하지 않습니다.':
 '학생과 보호자가 점포를 쉽게 찾고 필기구·미술용품의 표시를 확인하기 편해지는 것을 기대합니다. 매출액이나 방문객 수의 증가 수치는 가정하지 않습니다.',
 '완료 확인은 간판 점등 상태, 조명 교체 수량, 점검·정비 내역 및 동일 위치의 전·후 사진으로 진행하도록 계획합니다. 완료 증빙은 향후 결과 보고 단계의 예시이며 이번 자료에 실제 시공 결과를 포함하지 않습니다.':
 '설치 확인 항목은 간판 글자와 점등, 조명 8개 교체 여부, 전기 회로 점검 기록입니다. 같은 위치의 전·후 사진과 품목별 작업 내역으로 확인하도록 계획하며 실제 완료 자료는 포함하지 않습니다.',
 '노후 간판 교체, LED 조명 10개 교체, 배선 점검·정비를 통해 매장 이용 환경을 개선하고자 합니다. 사업자 정보와 견적 내역을 확인하고 사업계획서·사진·증빙을 함께 제출합니다.':
 '문구점 입구의 간판과 학습용품 진열 구역의 조명을 개선하고 전기 회로를 점검하고자 합니다. 조명 8개를 포함한 견적과 사업계획, 현황 사진을 첨부하여 신청합니다.',
 '외부 간판 교체':'출입구 간판 교체', '매장 조명 개선':'진열 통로 조명 개선', '전기 배선 정비':'전기 회로 점검·정비',
})


def transform(text):
    # 긴 표현을 먼저 매칭하고 한 번만 치환해 숫자·명칭이 연쇄 변경되지 않게 한다.
    import re
    pattern='|'.join(re.escape(k) for k in sorted(REPLACEMENTS,key=len,reverse=True))
    return re.sub(pattern,lambda match:REPLACEMENTS[match.group()],str(text))


def main():
    OUTPUT.mkdir(parents=True,exist_ok=True)
    DIRECTORY.mkdir(parents=True,exist_ok=True)
    base=form.base
    base.WORK.mkdir(parents=True,exist_ok=True)
    for name in ['SampleRegular','SampleBold']:
        if not (base.WORK/f'{name}.ttf').exists():base.setup_fonts();break
    for name in ['SampleRegular','SampleBold']:
        form.pdfmetrics.registerFont(form.TTFont(name,str(base.WORK/f'{name}.ttf')))
    base.setup_styles()
    for style in base.STYLES.values():style.textColor=form.INK
    base.STYLES['body'].fontSize=9;base.STYLES['body'].leading=15
    base.STYLES['heading'].fontSize=11;base.STYLES['heading'].leading=18
    base.STYLES['formTitle']=form.ParagraphStyle('formTitle',fontName='SampleBold',fontSize=18,leading=24,alignment=form.TA_CENTER)
    original=copy.deepcopy(base.DATA)
    alternative=copy.deepcopy(original)
    alternative['datasetVersion']='support-application-comparison'
    app=alternative['application']
    app.update(applicationDate='2026-11-02',businessName='가상문구점',businessNo='0000000002',representative='예시대표',openedOn='2023-06-15',
               phone='000-0000-0002',address='예시시 가상구 배움로 45, 1층',businessItem='문구용품 소매',requestedAmount=2400000,selfContribution=240000,totalProjectCost=2640000)
    app['planText']='문구점 간판 교체, LED 조명 8개 교체 및 배선 점검·정비를 통해 학습용품 구매 환경을 개선합니다. (가상 신청)'
    quote=alternative['estimate']
    quote.update(quoteNo='TEST-Q-2026-002',issuedOn='2026-10-29',validUntil='2026-11-29',supplierName='예시간판제작소',supplierBusinessNo='0000000003',supplierRepresentative='예시설치자',supplyAmount=2400000,taxAmount=240000,totalAmount=2640000)
    for item,unit_price,quantity in zip(quote['items'],[1400000,75000,400000],[1,8,1]):
        item['unitPrice']=unit_price;item['quantity']=quantity;item['amount']=unit_price*quantity
        item['specification']=transform(item['specification'])
        item['name']=transform(item['name'])
    alternative['documents']['businessCertificate']['expectedFields']={k:app[k] for k in ['businessName','businessNo','representative','openedOn']}
    alternative['note']='같은 서식의 대체 정상 사례. 원본과 섞지 말고 각 사례 안의 일관성을 확인한다. 공통 정책·항목명은 변경하지 않는다.'
    base.DATA=alternative;base.APP=app;base.QUOTE=quote;base.SOURCE=DIRECTORY
    original_p=form.p
    form.p=lambda text,style='body':original_p(transform(text),style)
    original_chrome=form.chrome
    def chrome(canvas,doc):
        original_chrome(canvas,doc)
    pdf=ROOT/'output/pdf/지원사업_비교용_대체입력.pdf'
    story=[]
    for index,builder in enumerate([form.checklist,form.application,form.plan,form.budget,form.photos,form.certificate,form.quote]):
        if index:story.append(PageBreak())
        story.extend(builder())
    SimpleDocTemplate(str(pdf),pagesize=base.A4,leftMargin=44,rightMargin=44,topMargin=58,bottomMargin=48,title='지원사업 비교용 대체 입력 사례',author='마음이음 포트폴리오').build(story,onFirstPage=chrome,onLaterPages=chrome)
    reader=PdfReader(pdf);assert len(reader.pages)==7
    assert sum(i['unitPrice']*i['quantity'] for i in quote['items'])==2400000
    for index in range(7):base.render_page(pdf,index+1,OUTPUT/f'비교용-{index+1:02}.png',dpi=200)
    (DIRECTORY/'정답데이터.json').write_text(json.dumps(alternative,ensure_ascii=False,indent=2)+'\n')
    changes={key:{'원본':original['application'][key],'비교용':value} for key,value in app.items() if original['application'].get(key)!=value}
    (DIRECTORY/'입력값비교.json').write_text(json.dumps(changes,ensure_ascii=False,indent=2)+'\n')
    manifest={'pages':7,'dpi':200,'sameForm':True,'files':{}}
    for path in [pdf,*sorted(OUTPUT.glob('*.png'))]:
        manifest['files'][str(path.relative_to(ROOT))]={'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'bytes':path.stat().st_size}
    (DIRECTORY/'생성목록.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
    print('비교용 PNG 7장 생성 완료')


if __name__=='__main__':main()

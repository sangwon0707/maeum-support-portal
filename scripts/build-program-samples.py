#!/usr/bin/env python3
"""사업별 정답 JSON에서 신청서 묶음·이미지 PDF·업로드 PNG를 함께 만든다."""
from __future__ import annotations

import hashlib
import importlib.util
import json
import shutil
from pathlib import Path

from PIL import Image as PillowImage
from pypdf import PdfReader
from reportlab.pdfgen import canvas
from reportlab.lib.styles import ParagraphStyle
from reportlab.platypus import Image, PageBreak, SimpleDocTemplate, Spacer

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('approved_form', ROOT / 'scripts/build-application-v2.py')
form = importlib.util.module_from_spec(spec)
spec.loader.exec_module(form)
base = form.base
SOURCE = ROOT / 'fixtures/program-samples'
OUTPUT = ROOT / 'output/pdf/사업별신청예시'
NAMES = {'digital': '디지털전환', 'energy': '에너지절감', 'access': '이용환경개선',
         'online': '온라인판로', 'safety': '안전위생'}


def amount(value):
    return f'{value:,}원'


def business_number(value):
    return f'{value[:3]}-{value[3:5]}-{value[5:]}'


def title(number, text, data):
    return form.title(number, text, data['application']['programTitle'])


def frame(label, text):
    return form.table([[form.p(label, 'label')], [form.p(text)]], [507],
                      shaded=[((0, 0), (0, 0))])


def signature(data):
    return [Spacer(1, 16), form.p('2026년 10월 25일', 'center'), Spacer(1, 8),
            form.p(f"신청인: {data['application']['representative']} (테스트 서명)", 'number')]


def application(data):
    a = data['application']
    rows = [['상호', a['businessName'], '대표자', a['representative']],
            ['사업자등록번호', business_number(a['businessNo']), '개업일', a['openedOn']],
            ['사업장 주소', a['address'], '', ''],
            ['업태 / 종목', a['businessType'] + ' / ' + a['businessItem'], '', ''],
            ['연락처', a['phone'], '신청 구분', '개인사업자 / 가상 사례']]
    story = title('1', '지 원 사 업  신 청 서', data)
    story += [form.table(rows, [85, 168.5, 85, 168.5],
                         spans=[((1, 2), (3, 2)), ((1, 3), (3, 3))],
                         shaded=[((0, 0), (0, 4)), ((2, 0), (2, 1)), ((2, 4), (2, 4))])]
    story += form.section('1. 신청 과제')
    story += [frame('과제명', data['label']), Spacer(1, 10), frame('개선 계획 요약', data['plan']['change'])]
    story += form.section('2. 신청 사업비')
    story += [form.table([['지원 신청액', '자부담', '총 사업비'],
                          [amount(a['requestedAmount']), amount(a['selfContribution']), amount(a['totalProjectCost'])]],
                         [169, 169, 169], shaded=[((0, 0), (2, 0))]), Spacer(1, 10),
              form.p('신청금액과 자부담의 합계는 견적 총액과 같습니다. 이 자료의 자부담은 사례별로 정한 가상 금액이며 실제 사업의 보조율 기준이 아닙니다.', 'small')]
    story += form.section('3. 첨부 자료 확인')
    story += [form.p('사업계획서 2쪽 / 사업비·일정 3쪽 / 현황 예시 4쪽 / 사업자등록증 5쪽 / 견적서 6쪽'),
              Spacer(1, 10), frame('작성 확인', '신청 정보와 증빙을 대조하고 금액·수량을 확인했습니다. 모든 정보는 가상이며 실제 개인정보 동의·계약·지원금 지급의 효력이 없습니다.')]
    return story + signature(data)


def plan(data):
    story = title('2', '사 업 계 획 서', data)
    for label, key in [('현재 현황과 개선 필요성', 'situation'), ('개선 범위와 추진 내용', 'change'),
                       ('운영·일정·위험 대응', 'operation'), ('결과물과 확인 방법', 'outcome')]:
        story += [frame(label, data['plan'][key]), Spacer(1, 14)]
    return story


def budget(data):
    a, q = data['application'], data['estimate']
    story = title('3', '사업비 산출 및 추진 일정', data)
    rows = [['항목 / 규격', '수량', '단가(원)', '공급가액(원)']]
    rows += [[i['name'] + '\n' + i['specification'], f"{i['quantity']} {i['unit']}",
              f"{i['unitPrice']:,}", f"{i['amount']:,}"] for i in q['items']]
    story += [form.table(rows, [245, 48, 107, 107], shaded=[((0, 0), (3, 0))])]
    story += form.section('1. 사업비 합계와 부담 구분')
    story += [form.table([['공급가액', amount(q['supplyAmount']), '부가세', amount(q['taxAmount'])],
                          ['견적 총액', amount(q['totalAmount']), '지원 신청액', amount(a['requestedAmount'])],
                          ['자부담', amount(a['selfContribution']), '합계 확인', '신청액 + 자부담 = 총액']],
                         [80, 173.5, 80, 173.5], shaded=[((0, 0), (0, 2)), ((2, 0), (2, 2))])]
    story += form.section('2. 추진 일정 - 선정 통보 후 계획')
    story += [form.table([['기간', '주요 작업', '담당 / 확인 자료']] + data['schedule'], [65, 235, 207],
                         shaded=[((0, 0), (2, 0))]), Spacer(1, 12),
              form.p('위 일정은 가상 신청의 계획이며 실제 선정·계약·설치 또는 용역 수행이 이루어졌다는 뜻이 아닙니다.', 'small')]
    return story


def photos(data):
    image_path = SOURCE / data['dataset'] / '현황예시.png'
    story = title('4', '사업장 현황 및 개선 범위', data)
    story += [form.table([[form.p('AI 생성 현황 예시 / 실제 사업장 사진 아님', 'label')],
                          [Image(str(image_path), width=487, height=324.67)]], [507],
                         shaded=[((0, 0), (0, 0))]), Spacer(1, 8),
              form.p(data['photo']['note'], 'small')]
    story += form.section('현황과 계획의 연결')
    story += [form.table([['구분', '현황 가정', '개선·확인 계획']] + data['conditions'], [75, 180, 252],
                         shaded=[((0, 0), (2, 0))])]
    return story


def certificate(data):
    a = data['application']
    story = form.title('첨부 1', '사업자등록증 (가상)', '문자 인식·내용 대조용 / 실제 세무기관 발급 문서 아님')
    date = a['openedOn'].split('-')
    rows = [['등록번호', business_number(a['businessNo'])], ['상호', a['businessName']],
            ['성명 (대표자)', a['representative']], ['개업연월일', f'{date[0]}년 {date[1]}월 {date[2]}일'],
            ['사업장 소재지', a['address']], ['사업의 종류', f"업태: {a['businessType']}\n종목: {a['businessItem']}"],
            ['사업자 유형', '개인사업자 (가상 분류)']]
    story += [form.table(rows, [110, 397], [46, 46, 46, 46, 60, 64, 42],
                         shaded=[((0, 0), (0, 6))]), Spacer(1, 14),
              frame('시험 자료 안내', '신청서와 증빙의 상호·대표자·사업자번호·개업일 대조를 위한 가상 값입니다. 실제 개인정보·관인·QR·발급기관 표시가 없으며 실제 사업자 여부나 지원 자격을 증명하지 않습니다.'),
              Spacer(1, 18), form.p('2026년 10월 25일\n마음이음 테스트 자료 제작자 (가상)', 'center')]
    return story


def quote(data):
    a, q = data['application'], data['estimate']
    story = form.title('첨부 2', '견  적  서', q['quoteNo'] + ' / 가상 거래 · 실제 청구용 아님')
    story += [form.table([['공급받는 자', '', '공급자', ''],
                          ['상호', a['businessName'], '상호', q['supplierName']],
                          ['대표자', a['representative'], '대표자', q['supplierRepresentative']],
                          ['사업자번호', business_number(a['businessNo']), '사업자번호', business_number(q['supplierBusinessNo'])],
                          ['견적일', q['issuedOn'], '유효기간', q['validUntil']]], [75, 178.5, 75, 178.5],
                         spans=[((0, 0), (1, 0)), ((2, 0), (3, 0))], shaded=[((0, 0), (3, 0))]), Spacer(1, 12)]
    rows = [['항목 / 규격', '수량', '단가(원)', '공급가액(원)']]
    rows += [[i['name'] + '\n' + i['specification'], f"{i['quantity']} {i['unit']}", f"{i['unitPrice']:,}", f"{i['amount']:,}"] for i in q['items']]
    for label, key in [('공급가액', 'supplyAmount'), ('부가세', 'taxAmount'), ('총 견적금액', 'totalAmount')]:
        rows.append([label, '', '', f'{q[key]:,}'])
    n = len(rows)
    story += [form.table(rows, [245, 48, 107, 107], spans=[((0, r), (2, r)) for r in range(n-3, n)],
                         shaded=[((0, 0), (3, 0))]), Spacer(1, 12), frame('거래 조건 및 확인', q['terms']),
              Spacer(1, 12), form.p(f"공급업체: {q['supplierName']} / {q['supplierRepresentative']} (테스트 확인)", 'number')]
    return story


def chrome(c, doc):
    c.saveState()
    c.setFont('SampleRegular', 8)
    c.setFillColor(form.INK)
    c.drawString(44, 814, '마음이음 | 사업별 작성 완료 예시')
    c.setFillColor(form.WARNING)
    c.drawRightString(551, 814, '테스트용 가상 문서 / 공식 서식 아님')
    c.setStrokeColor(form.INK)
    c.line(44, 804, 551, 804)
    c.setFillColor(form.WARNING)
    c.drawString(44, 26, '모든 정보는 가상입니다. 실제 신청·증명·계약·지급에 사용할 수 없습니다.')
    c.setFillColor(form.INK)
    c.drawRightString(551, 26, f'- {doc.page} -')
    c.restoreState()


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    base.WORK.mkdir(parents=True, exist_ok=True)
    for name in ['SampleRegular', 'SampleBold']:
        if not (base.WORK / f'{name}.ttf').exists():
            base.setup_fonts()
            break
    for name in ['SampleRegular', 'SampleBold']:
        form.pdfmetrics.registerFont(form.TTFont(name, str(base.WORK / f'{name}.ttf')))
    base.setup_styles()
    for style in base.STYLES.values():
        style.textColor = form.INK
    base.STYLES['body'].fontSize = 9
    base.STYLES['body'].leading = 15
    base.STYLES['heading'].fontSize = 11
    base.STYLES['heading'].leading = 18
    base.STYLES['formTitle'] = ParagraphStyle('formTitle', fontName='SampleBold', fontSize=18,
                                             leading=24, alignment=form.TA_CENTER, wordWrap='CJK')
    result = []
    for dataset, label in NAMES.items():
        data = json.loads((SOURCE / dataset / '정답데이터.json').read_text())
        app, estimate = data['application'], data['estimate']
        assert sum(i['quantity'] * i['unitPrice'] for i in estimate['items']) == estimate['supplyAmount']
        assert estimate['supplyAmount'] + estimate['taxAmount'] == estimate['totalAmount']
        assert app['requestedAmount'] + app['selfContribution'] == estimate['totalAmount']
        pdf = OUTPUT / f'{label}_신청예시.pdf'
        story = []
        for index, builder in enumerate([application, plan, budget, photos, certificate, quote]):
            if index:
                story.append(PageBreak())
            story.extend(builder(data))
        SimpleDocTemplate(str(pdf), pagesize=base.A4, leftMargin=44, rightMargin=44, topMargin=58,
                          bottomMargin=48, title=f'{label} 신청 완료 가상 예시', author='마음이음 포트폴리오').build(
                              story, onFirstPage=chrome, onLaterPages=chrome)
        reader = PdfReader(pdf)
        assert len(reader.pages) == 6, (dataset, len(reader.pages))
        for page in reader.pages:
            assert '테스트용 가상 문서' in page.extract_text()
        assert app['businessName'] in reader.pages[0].extract_text()
        pages = ROOT / 'output/images/사업별신청예시' / dataset
        for index in range(1, 7):
            base.render_page(pdf, index, pages / f'{label}-{index:02}.png', dpi=150)
        resources = ROOT / 'src/main/resources/fixtures' / dataset
        resources.mkdir(parents=True, exist_ok=True)
        base.render_page(pdf, 5, resources / 'business-cert.png', dpi=200)
        base.render_page(pdf, 6, resources / 'estimate.png', dpi=200)
        (resources / 'application.json').write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n')
        scanned = OUTPUT / f'{label}_신청예시_스캔본.pdf'
        scanned_canvas = canvas.Canvas(str(scanned), pagesize=base.A4)
        for index in range(1, 7):
            image_path = pages / f'{label}-{index:02}.png'
            scanned_canvas.drawImage(str(image_path), 0, 0, width=base.A4[0], height=base.A4[1])
            scanned_canvas.showPage()
        scanned_canvas.save()
        assert len(PdfReader(scanned).pages) == 6
        assert all(not p.extract_text().strip() for p in PdfReader(scanned).pages)
        for image_name in ['business-cert.png', 'estimate.png']:
            image = PillowImage.open(resources / image_name)
            assert image.width * image.height <= 10_000_000
            assert (resources / image_name).stat().st_size <= 10_485_760
        result.append({'dataset': dataset, 'label': data['label'], 'pages': 6,
                       'pdf': str(pdf.relative_to(ROOT)), 'scannedPdf': str(scanned.relative_to(ROOT)),
                       'sha256': hashlib.sha256(pdf.read_bytes()).hexdigest(), 'checks': '합계·쪽수·텍스트·업로드 크기 확인'})
        print(label, '6쪽 PDF·스캔본·업로드 PNG 생성 완료', flush=True)
    (OUTPUT / '생성검증.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')


if __name__ == '__main__':
    main()

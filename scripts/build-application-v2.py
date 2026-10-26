#!/usr/bin/env python3
"""전북·수원 서식의 구조를 참고한 가상 신청 문서. 기존 버전은 보존한다."""
import hashlib
import importlib.util
import json
import shutil
from pathlib import Path

from PIL import Image as PillowImage
from pypdf import PdfReader
from reportlab.lib import colors
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.enums import TA_CENTER
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import Flowable, Image, PageBreak, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('samples', ROOT / 'scripts/lib/sample_document_layout.py')
base = importlib.util.module_from_spec(spec)
spec.loader.exec_module(base)
WIDTH = 507
INK = colors.HexColor('#222222')
GRAY = colors.HexColor('#e5e5e5')
WARNING = colors.HexColor('#c00000')
OUT = ROOT / 'output/pdf/지원사업_신청서류_개정2판.pdf'
FIXTURES = ROOT / 'fixtures/support-application-v2'


def p(text, style='body'):
    # 빨간색 안내는 머리말·꼬리말에서만 사용한다.
    return base.para(text, style)



def table(rows, widths, heights=None, spans=(), shaded=(), centered=()):
    cells = [[v if isinstance(v, (Flowable, list)) else p(v) for v in row] for row in rows]
    result = Table(cells, colWidths=widths, rowHeights=heights, hAlign='LEFT')
    commands = [('GRID', (0, 0), (-1, -1), .55, INK), ('BOX', (0, 0), (-1, -1), 1, INK),
                ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'), ('LEFTPADDING', (0, 0), (-1, -1), 7),
                ('RIGHTPADDING', (0, 0), (-1, -1), 7), ('TOPPADDING', (0, 0), (-1, -1), 6),
                ('BOTTOMPADDING', (0, 0), (-1, -1), 6)]
    commands += [('SPAN', a, b) for a, b in spans]
    commands += [('BACKGROUND', a, b, GRAY) for a, b in shaded]
    commands += [('ALIGN', a, b, 'CENTER') for a, b in centered]
    result.setStyle(TableStyle(commands))
    return result


def title(number, text, subtitle='2026 소상공인 매장 환경개선 지원사업'):
    return [p(f'【 가상 서식 {number} 】', 'label'), Spacer(1, 5),
            table([[p(text, 'formTitle')], [p(subtitle, 'center')]], [WIDTH], [34, 23],
                  shaded=[((0, 0), (0, 1))]), Spacer(1, 12)]


def section(text):
    return [Spacer(1, 9), p(text, 'heading'), Spacer(1, 6)]


def frame(label, content, height):
    return table([[p('■ '+label, 'label')], [p(content)]], [WIDTH], [28, height],
                 shaded=[((0, 0), (0, 0))])


def signature():
    return [Spacer(1, 12), p('2026년 10월 23일', 'center'), Spacer(1, 8),
            p('신청인: 가상대표  (테스트 서명)', 'number'), Spacer(1, 12),
            p('마음이음 지원사업 검토 담당자 귀하 (가상 수신처)', 'label')]


def checklist():
    story = title('1', '신청 서류 확인표')
    story += [table([['구분', '제출 자료', '해당 쪽', '확인'],
                     ['신청', '지원사업 신청서', '2쪽', '제출'],
                     ['', '사업계획서', '3쪽', '제출'],
                     ['', '사업비 산출 및 추진 일정', '4쪽', '제출'],
                     ['', '사업장 현황 사진대지', '5쪽', '제출'],
                     ['증빙', '사업자등록증 예시 (가상)', '6쪽', '제출'],
                     ['', '시공 견적서 (가상)', '7쪽', '제출']],
                    [48, 330, 65, 64], [30]*7,
                    spans=[((0, 1), (0, 4)), ((0, 5), (0, 6))],
                    shaded=[((0, 0), (3, 0)), ((0, 1), (0, 6))])]
    story += section('1. 가상 사업의 신청 조건')
    story += [frame('예시 문서에 적용하는 테스트 기준',
                    '• 간판·조명·배선 개선을 하나의 매장 환경개선 과제로 신청합니다.\n'
                    '• 이 사례의 지원 신청액은 공급가액 3,000,000원, 자부담은 부가세 300,000원입니다.\n'
                    '• 선정 통보 후 30일 이내 추진하는 가상 계획입니다.\n'
                    '• 위 기준은 데모 사례의 가정이며 실제 기관의 지원 요건·보조율이 아닙니다.', 92)]
    story += section('2. 신청 전 확인')
    story += [frame('자료 확인 및 사용 범위',
                    '확인 완료: 신청서와 증빙의 상호·대표자·번호·개업일이 일치합니다.\n'
                    '확인 완료: 품목별 산출액, 공급가액, 부가세, 총액의 합계가 일치합니다.\n'
                    '확인 완료: 사진 2장은 AI로 생성한 테스트용 자료입니다.\n'
                    '모든 정보는 가상이며 실제 신청·계약·지급·개인정보 동의의 효력은 없습니다.', 92)]
    story += signature()
    return story


def application():
    story = title('2', '지 원 사 업  신 청 서')
    rows = [
        ['접수번호', 'TEST-APP-2026-001', '', '신청일', '2026.10.23', ''],
        ['기업현황', '상호', '가상상점', '대표자', '가상대표', ''],
        ['', '사업자번호', '000-00-00000', '개업일', '2024.01.01', ''],
        ['', '연락처', '000-0000-0000', '사업자 유형', '개인사업자 (가상)', ''],
        ['', '업태 / 종목', '도매 및 소매업 / 생활잡화 소매', '', '', ''],
        ['', '사업장주소', base.APP['address'], '', '', ''],
        ['신청내용', '개선 분야', '■ 간판 교체   ■ 조명 개선   ■ 배선 점검·정비', '', '', ''],
        ['', '과제명', '매장 간판·조명 및 배선 환경개선', '', '', ''],
        ['', '사업기간', '선정 통보 후 30일 이내 (가상 계획)', '', '', ''],
        ['사업비', '지원 신청액', '자부담 (부가세)', '', '총 사업비', ''],
        ['', '3,000,000원', '300,000원', '', '3,300,000원', ''],
    ]
    spans=[((1, 0), (2, 0)), ((4, 0), (5, 0)), ((0, 1), (0, 5)), ((4, 1), (5, 1)),
           ((4, 2), (5, 2)), ((4, 3), (5, 3)), ((2, 4), (5, 4)), ((2, 5), (5, 5)),
           ((0, 6), (0, 8)), ((2, 6), (5, 6)), ((2, 7), (5, 7)), ((2, 8), (5, 8)),
           ((0, 9), (0, 10)), ((2, 9), (3, 9)), ((4, 9), (5, 9)), ((2, 10), (3, 10)), ((4, 10), (5, 10))]
    story += [table(rows, [61, 89, 114, 66, 90, 87], [31]*11, spans,
                    shaded=[((0, 0), (0, 10)), ((1, 9), (5, 9))])]
    story += section('신청 내용 확인')
    story += [frame('작성 완료 예시',
                    '노후 간판 교체, LED 조명 10개 교체, 배선 점검·정비를 통해 매장 이용 환경을 개선하고자 합니다. '
                    '사업자 정보와 견적 내역을 확인하고 사업계획서·사진·증빙을 함께 제출합니다.\n\n'
                    '본 신청서는 가상 자료입니다. 실제 기관의 자격심사·지원금 지급을 요청하는 문서가 아닙니다.', 95)]
    story += signature()
    return story


def plan():
    story = title('3', '사 업 계 획 서')
    story += [frame('업체 및 주요 서비스 소개',
                    '가상상점은 생활잡화와 주방·청소용품을 판매하는 가상 소매점입니다. '
                    '고객이 입구를 쉽게 찾고 진열 상품을 확인할 수 있는 매장 환경을 목표로 합니다. '
                    '상호·소재지·개업일은 신청서 및 가상 증빙과 동일하게 작성했습니다.', 118),
              frame('환경개선 필요성 및 진행 예정 사항',
                    '외부 간판은 표면이 낡고 글자가 바래 시인성 개선이 필요합니다. '
                    '내부는 천장 조명과 진열 통로의 밝기가 고르지 않은 상황을 가정했습니다.\n\n'
                    '기존 간판 철거 및 LED 간판 설치, 천장 LED 조명 10개 교체를 계획합니다. '
                    '배선은 사진으로 상태를 판정하지 않고 시공 전 별도 점검 후 정비 범위를 확인합니다. '
                    '모든 시공 내용은 실제 작업이 없는 테스트 계획입니다.', 155),
              frame('기대 효과 및 완료 확인 방법',
                    '출입구 식별과 진열 상품 확인이 쉬워지는 것을 목표로 합니다. 매출 증가율은 임의로 제시하지 않습니다.\n\n'
                    '완료 확인은 간판 점등 상태, 조명 교체 수량, 점검·정비 내역 및 동일 위치의 전·후 사진으로 진행하도록 계획합니다. '
                    '완료 증빙은 향후 결과 보고 단계의 예시이며 이번 자료에 실제 시공 결과를 포함하지 않습니다.', 126)]
    return story


def budget():
    story = title('4', '사업비 산출 및 추진 일정')
    story += section('1. 사업비 산출 내역')
    rows = [['품목', '수량', '단가(원)', '공급가액(원)']]
    for item in base.QUOTE['items']:
        rows.append([item['name'], f"{item['quantity']} {item['unit']}", f"{item['unitPrice']:,}", f"{item['amount']:,}"])
    rows += [['공급가액 합계', '', '', '3,000,000'], ['부가세', '', '', '300,000'], ['총 사업비', '', '', '3,300,000']]
    story += [table(rows, [232, 55, 110, 110], [32]*7,
                    spans=[((0, r), (2, r)) for r in [4, 5, 6]], shaded=[((0, 0), (3, 0))])]
    story += section('2. 재원 구성')
    story += [table([['지원 신청액', '자부담 (부가세)', '총 사업비'], ['3,000,000원', '300,000원', '3,300,000원']],
                    [169]*3, [30, 34], shaded=[((0, 0), (2, 0))])]
    story += section('3. 추진 일정 및 점검')
    story += [table([['단계', '예정 시점', '주요 내용'],
                     ['준비', '선정 통보 후 1~7일', '시공 범위·일정 협의, 기존 상태 기록'],
                     ['시공', '8~21일', '간판·조명 교체, 배선 점검·정비'],
                     ['확인', '22~30일', '수량·점등·정비 내역 확인, 결과 자료 정리']],
                    [60, 145, 302], [30, 43, 43, 43], shaded=[((0, 0), (2, 0))])]
    story += [Spacer(1, 10), p('※ 시점은 가상 계획입니다. 실제 지원사업의 계약·착공·정산 절차를 대신하지 않습니다.', 'small')]
    return story


def photo_cell(filename, text):
    path = base.SOURCE / 'photos' / filename
    with PillowImage.open(path) as image: width, height = image.size
    return [Image(str(path), width=230, height=230*height/width), Spacer(1, 8), p(text, 'small'),
            Spacer(1, 5), p('AI로 생성한 테스트용 사진\n실제 매장 촬영 아님', 'small')]


def photos():
    story = title('5', '사업장 현황 및 개선 계획 사진대지')
    story += [table([[p('■ 사업장 내·외부 현황', 'label'), ''],
                     [photo_cell('매장외부-개선전.png', '외부: 출입구 정면 / 기존 간판이 보이는 구도'),
                      photo_cell('매장내부-개선전.png', '내부: 출입구에서 진열 통로·천장 조명을 보는 구도')]],
                    [253.5, 253.5], [30, 242], spans=[((0, 0), (1, 0))], shaded=[((0, 0), (1, 0))])]
    story += [table([[p('■ 개선할 부분 및 상세 설명', 'label'), '', ''],
                     ['구분', '현재 상태', '개선 계획'],
                     ['외부 간판', '표면 노후 및 글자 퇴색', '기존 간판 철거 및 LED 간판 설치'],
                     ['내부 조명', '진열 통로의 고르지 않은 밝기', '천장 LED 조명 10개 교체'],
                     ['배선 점검', '사진만으로 상태를 판정할 수 없음', '별도 점검 후 정비 범위 확인']],
                    [73, 185, 249], [30, 29, 60, 60, 60], spans=[((0, 0), (2, 0))],
                    shaded=[((0, 0), (2, 1))])]
    story += [Spacer(1, 10), p('※ 사진은 두 개의 별도 생성 장면입니다. 실제 촬영일·GPS 정보가 없으며 현장 상태 또는 공사 결과를 증명하지 않습니다.', 'small')]
    return story


def certificate():
    story = title('첨부 1', '사업자등록증 (가상)', '문서 대조·OCR 시험용 / 실제 세무기관 발급 문서 아님')
    rows = [['등록번호', '000-00-00000'], ['상호', base.APP['businessName']], ['성명 (대표자)', base.APP['representative']],
            ['개업연월일', '2024년 01월 01일'], ['사업장 소재지', base.APP['address']],
            ['사업의 종류', '업태: 도매 및 소매업\n종목: 생활잡화 소매'], ['사업자 유형', '개인사업자 (가상 분류)']]
    story += [table(rows, [110, 397], [48, 48, 48, 48, 60, 68, 48], shaded=[((0, 0), (0, 6))]),
              table([[p('시험 자료 안내', 'label')], [p('등록번호·상호·대표자·개업일은 신청서와 대조하기 위한 가상 값입니다. '
                    '이 문서에는 실제 개인정보·관인·인증 QR이 없습니다. 실제 사업자 여부를 증명하거나 지원 자격을 판정할 수 없습니다.')]],
                    [WIDTH], [29, 75], shaded=[((0, 0), (0, 0))]),
              Spacer(1, 18), p('2026년 10월 20일\n마음이음 테스트 자료 제작자 (가상)', 'center')]
    return story


def quote():
    story = title('첨부 2', '견  적  서', 'TEST-Q-2026-001 / 가상 거래 견적 · 실제 청구용 아님')
    story += [table([['공급받는 자', '', '공급자', ''], ['상호', '가상상점', '상호', '가상간판공방'],
                     ['대표자', '가상대표', '대표자', '가상시공자'], ['사업자번호', '000-00-00000', '사업자번호', '000-00-00001'],
                     ['견적일', '2026.10.20', '연락처', '000-0000-0000']], [76, 177.5, 76, 177.5], [29]*5,
                    spans=[((0, 0), (1, 0)), ((2, 0), (3, 0))], shaded=[((0, 0), (3, 0))])]
    rows = [['품목 / 규격', '수량', '단가(원)', '공급가액(원)']]
    for item in base.QUOTE['items']:
        rows.append([item['name']+'\n'+item['specification'], f"{item['quantity']} {item['unit']}", f"{item['unitPrice']:,}", f"{item['amount']:,}"])
    rows += [['공급가액 합계', '', '', '3,000,000'], ['부가세', '', '', '300,000'], ['총 견적금액', '', '', '3,300,000']]
    story += [table(rows, [232, 55, 110, 110], [30, 57, 57, 57, 30, 30, 34],
                    spans=[((0, r), (2, r)) for r in [4, 5, 6]], shaded=[((0, 0), (3, 0))]),
              table([[p('거래 조건 및 확인', 'label')], [p('유효기간: 2026.11.20까지 / 완료 예정: 일정 협의 후 30일 이내\n'
                    '시공 예정지: '+base.APP['address']+'\n위 견적의 품목·수량·금액을 확인합니다. 공급업체: 가상간판공방 (테스트 확인)\n'
                    '실제 계약·시공·계약금 입금이 없는 가상 거래 자료입니다.')]],
                    [WIDTH], [28, 100], shaded=[((0, 0), (0, 0))])]
    return story


def chrome(canvas, doc):
    canvas.saveState()
    canvas.setFont('SampleRegular', 8)
    canvas.setFillColor(INK)
    canvas.drawString(44, 814, '마음이음 | 작성 완료 예시 · 개정 2판')
    canvas.setFillColor(WARNING)
    canvas.drawRightString(551, 814, '테스트용 가상 문서 / 공식 서식 아님')
    canvas.setStrokeColor(INK)
    canvas.setLineWidth(.4)
    canvas.line(44, 804, 551, 804)
    canvas.setFillColor(WARNING)
    canvas.drawString(44, 26, '모든 정보는 가상입니다. 실제 신청·증명·계약·지급에 사용할 수 없습니다.')
    canvas.setFillColor(INK)
    canvas.drawRightString(551, 26, f'- {doc.page} -')
    canvas.restoreState()


def main():
    base.WORK.mkdir(parents=True, exist_ok=True)
    FIXTURES.mkdir(parents=True, exist_ok=True)
    for name in ['SampleRegular', 'SampleBold']:
        if not (base.WORK / f'{name}.ttf').exists(): base.setup_fonts(); break
    for name in ['SampleRegular', 'SampleBold']:
        pdfmetrics.registerFont(TTFont(name, str(base.WORK / f'{name}.ttf')))
    base.setup_styles()
    for style in base.STYLES.values(): style.textColor = INK
    base.STYLES['body'].fontSize=9
    base.STYLES['body'].leading=15
    base.STYLES['heading'].fontSize=11
    base.STYLES['heading'].leading=18
    base.STYLES['formTitle'] = ParagraphStyle('formTitle', fontName='SampleBold', fontSize=18, leading=24, alignment=TA_CENTER)
    story=[]
    builders=[checklist, application, plan, budget, photos, certificate, quote]
    for index, builder in enumerate(builders):
        if index: story.append(PageBreak())
        story.extend(builder())
    SimpleDocTemplate(str(OUT), pagesize=base.A4, leftMargin=44, rightMargin=44,
                      topMargin=58, bottomMargin=48, title='마음이음 지원사업 신청서류 개정 2판', author='마음이음 포트폴리오').build(story,onFirstPage=chrome,onLaterPages=chrome)
    reader=PdfReader(OUT)
    assert len(reader.pages)==7, f'예상 7쪽, 실제 {len(reader.pages)}쪽'
    for page in reader.pages: assert '테스트용 가상 문서' in page.extract_text()
    assert len(reader.pages[4].images)==2
    for index in range(7): base.render_page(OUT,index+1,FIXTURES/f'서류-{index+1}.png')
    shutil.copyfile(OUT,ROOT/'src/main/webapp/assets/samples'/OUT.name)
    manifest={'version':'support-application-v2','pages':7,'pdf':str(OUT.relative_to(ROOT)),'files':{}}
    # 정답은 평가용 산출물이며 OCR 공급자에 전달하지 않는다.
    answers=json.loads(json.dumps(base.DATA))
    answers['datasetVersion']='support-application-v2'
    answers['estimate']['supplierRepresentative']='가상시공자'
    answers['documents']={'checklist': {'pdfPage':1}, 'application': {'pdfPage':2}, 'plan':{'pdfPage':3},
                          'budget':{'pdfPage':4}, 'photos':{'pdfPage':5},
                          'businessCertificate':{'pdfPage':6,'expectedFields':base.DATA['documents']['businessCertificate']['expectedFields']},
                          'estimate':{'pdfPage':7}}
    answers['scenarios']={'NORMAL':base.DATA['scenarios']['NORMAL']}
    answers['note']='개정 2판은 정상 사례 1건이다. 불일치·보완 변형은 v1에 있으며 이 버전으로 새로 생성하지 않았다.'
    answer_path=FIXTURES/'정답데이터.json'
    answer_path.write_text(json.dumps(answers,ensure_ascii=False,indent=2)+'\n')
    for path in [OUT,answer_path,*sorted(FIXTURES.glob('*.png'))]:
        manifest['files'][str(path.relative_to(ROOT))]={'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'bytes':path.stat().st_size}
    (FIXTURES/'생성목록.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
    print(OUT)


if __name__=='__main__': main()

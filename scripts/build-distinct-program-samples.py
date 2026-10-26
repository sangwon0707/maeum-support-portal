#!/usr/bin/env python3
"""사업별 업무 구조를 반영한 개별 신청서 예시 PDF와 페이지 이미지를 만든다."""
from __future__ import annotations

import importlib.util
import json
from pathlib import Path
import sys

from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT, TA_RIGHT
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import Flowable, Image, PageBreak, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle
from pypdf import PdfReader
import pdfplumber

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('program_samples', ROOT / 'scripts/build-program-samples.py')
form = importlib.util.module_from_spec(spec)
spec.loader.exec_module(form)
base = form.base
SOURCE = ROOT / 'fixtures/program-samples'
PDF_DIR = ROOT / 'output/pdf/사업별신청예시-개별서식'
IMAGE_DIR = ROOT / 'output/images/사업별신청예시-개별서식'
NAMES = form.NAMES
INK = colors.HexColor('#191919')
MID = colors.HexColor('#777777')
LIGHT = colors.HexColor('#eeeeee')
RED = colors.HexColor('#b00020')
WIDTH = 507


def p(text, style='body'):
    return base.para(str(text), style)


def tbl(rows, widths, heights=None, shaded=(), spans=(), centered=(), padd=6):
    cells = [[v if isinstance(v, (Flowable, list)) else p(v) for v in row] for row in rows]
    t = Table(cells, colWidths=widths, rowHeights=heights, hAlign='LEFT')
    cmds = [('GRID', (0, 0), (-1, -1), .55, INK), ('BOX', (0, 0), (-1, -1), 1, INK),
            ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'), ('LEFTPADDING', (0, 0), (-1, -1), padd),
            ('RIGHTPADDING', (0, 0), (-1, -1), padd), ('TOPPADDING', (0, 0), (-1, -1), 5),
            ('BOTTOMPADDING', (0, 0), (-1, -1), 5)]
    cmds.extend(('BACKGROUND', a, b, LIGHT) for a, b in shaded)
    cmds.extend(('SPAN', a, b) for a, b in spans)
    cmds.extend(('ALIGN', a, b, 'CENTER') for a, b in centered)
    t.setStyle(TableStyle(cmds))
    return t


def heading(dataset, number, title, subtitle):
    # 사업별로 제목 위치와 표식의 밀도를 달리해 첫눈에 서로 다른 서식으로 보이게 한다.
    if dataset == 'digital':
        return [p(f'서식 D-{number}  |  디지털 전환 신청자료', 'label'), Spacer(1, 4),
                p(title, 'formTitle'), p(subtitle, 'small'), Spacer(1, 10)]
    if dataset == 'energy':
        return [tbl([[p(f'에너지 개선  /  {number}', 'label'), p(title, 'formTitle')]], [93, 414], shaded=[((0, 0), (0, 0))]),
                Spacer(1, 7), p(subtitle, 'small'), Spacer(1, 8)]
    if dataset == 'access':
        return [p(title, 'formTitle'), tbl([[f'접근환경 개선 기록  •  {number}', subtitle]], [145, 362], shaded=[((0, 0), (0, 0))]), Spacer(1, 10)]
    if dataset == 'online':
        return [p(f'온라인 판로 / 제작 의뢰서 / {number}', 'label'), Spacer(1, 7), p(title, 'formTitle'),
                p(subtitle, 'right'), Spacer(1, 10)]
    return [tbl([[p(title, 'formTitle')], [p(f'안전·위생 현장관리 기록   |   {number}  ·  {subtitle}', 'small')]], [WIDTH], shaded=[((0, 0), (0, 1))]), Spacer(1, 10)]


def section(label):
    return [Spacer(1, 8), p(label, 'heading'), Spacer(1, 5)]


def app_page(data):
    d, a = data['dataset'], data['application']
    story = heading(d, '01', '지원사업 신청서', a['programTitle'])
    common = [['신청인', a['businessName'], '대표자', a['representative']],
              ['등록번호', form.business_number(a['businessNo']), '개업일', a['openedOn']],
              ['사업장', a['address'], '연락처', a['phone']],
              ['업태·종목', f"{a['businessType']} / {a['businessItem']}", '사례', '가상 신청']]
    if d == 'digital':
        story += [tbl(common, [75, 177, 75, 180], shaded=[((0, 0), (0, 3)), ((2, 0), (2, 3))])]
        story += section('도입 전·후 업무 흐름 비교')
        story += [tbl([['업무 단계', '도입 전', '도입 후 목표'],
                       ['주문 전달', '종이 전표를 주방에 전달', 'POS 주문이 주방 출력기로 전달'],
                       ['주문 변경', '변경 내용을 다시 전달', '변경 내역을 화면·출력으로 확인'],
                       ['재고 정리', '영업 종료 뒤 수기 집계', '주요 품목을 화면에서 함께 관리']], [74, 204, 229], shaded=[((0, 0), (2, 0))])]
        story += section('신청 범위와 사업비')
        story += [tbl([['신청 품목', '수량·규격', '확인 기준'], ['매장용 POS', '1대 / 주문·재고 관리', '메뉴 등록 및 주문 흐름 확인'],
                       ['주방 출력 장치', '2대 / 주문표 출력', '주문 전달·변경 출력 확인'], ['초기 설정·교육', '견적 명세 기준', '계정·메뉴 설정 및 직원 안내']], [112, 185, 210]),
                    Spacer(1, 8), tbl([['지원 신청액', '자부담', '총 사업비'], [f"{a['requestedAmount']:,}원", f"{a['selfContribution']:,}원", f"{a['totalProjectCost']:,}원"]], [169]*3, shaded=[((0, 0), (2, 0))]),
                    Spacer(1, 7), p('본 자료와 인물·업체·금액은 가상입니다. 실제 지원 자격·선정·지급 효력이 없습니다.', 'small')]
    elif d == 'energy':
        story += [tbl([[p('사업자 기본정보', 'label'), ''], *[[r[0]+'  '+r[1], r[2]+'  '+r[3]] for r in common]], [253.5, 253.5], spans=[((0, 0), (1, 0))], shaded=[((0, 0), (1, 0))])]
        story += section('교체 대상 설비 대조')
        story += [tbl([['구분', '기존 설비', '교체·개선 대상'], ['냉장 진열', '꽃 냉장 진열장 1대', '가상 모델 FL-E1 1대'], ['조명', '천장 조명 12개', '가상 모델 LT-E2 LED 12개'], ['철거·설치', '기존 설비 처리 필요', '철거·운반 및 시운전']], [75, 216, 216], shaded=[((0, 0), (2, 0))]), Spacer(1, 5),
                    p('제품 사양·효율 등급은 제품자료로 확인합니다. 현황 예시 사진만으로 소비전력이나 절감 효과를 판정하지 않습니다.', 'small')]
        story += section('사업비 구성')
        story += [tbl([['지원 신청액', '자부담', '총 사업비'], [f"{a['requestedAmount']:,}원", f"{a['selfContribution']:,}원", f"{a['totalProjectCost']:,}원"]], [169]*3, shaded=[((0, 0), (2, 0))]), Spacer(1, 8), p('신청일: '+a['applicationDate']+'  /  전기 사용시간과 기존·교체 제품 수치는 확인자료 수집 전까지 미확인', 'small')]
    elif d == 'access':
        story += [tbl([['신청인 / 상호', a['businessName'], '대표자', a['representative']], ['등록번호 / 개업', form.business_number(a['businessNo'])+' / '+a['openedOn'], '연락처', a['phone']], ['사업장 소재지', a['address'], '업종', a['businessItem']]], [105, 190, 65, 147], spans=[((1, 2), (3, 2))], shaded=[((0, 0), (0, 2)), ((2, 0), (2, 2))])]
        story += section('위치별 불편·치수 조사 및 설치 허가 계획')
        story += [tbl([['위치', '현재 불편', '현장 확인·허가', '예정 작업'], ['출입구', '작은 단차', '폭·높이 실측 / 임대인 협의', '단차 완화 설치물 1식'], ['서가 사이', '진열대 사이 폭이 좁음', '유효 통로폭 실측', '진열대 이동·동선 확보'], ['안내 구간', '서가 구분이 어려움', '설치 위치·고정방식 협의', '큰 글자 안내판 6개']], [68, 130, 167, 144], shaded=[((0, 0), (3, 0))])]
        story += section('신청액과 사업 범위')
        story += [tbl([['지원 신청액', f"{a['requestedAmount']:,}원"], ['자부담', f"{a['selfContribution']:,}원"], ['총 사업비', f"{a['totalProjectCost']:,}원"]], [180, 327], shaded=[((0, 0), (0, 2))]), Spacer(1, 8), p('공용 보도에 고정물을 설치하지 않는 방안을 우선 검토합니다. 허가·규격 확인 전 설치하지 않는 가상 계획입니다.', 'small')]
    elif d == 'online':
        story += [tbl([['상호', a['businessName'], '대표자', a['representative']], ['사업자번호', form.business_number(a['businessNo']), '개업일', a['openedOn']], ['주소·연락처', a['address']+' / '+a['phone'], '업종', a['businessItem']]], [78, 176, 72, 179], spans=[((1, 2), (3, 2))], shaded=[((0, 0), (0, 2)), ((2, 0), (2, 2))])]
        story += section('콘텐츠 제작 과업지시 요약')
        story += [tbl([['납품물', '수량·규격', '검수 기준'], ['상품 사진', '보정 JPG 20컷', '컷 수·색상·파일 열림'], ['상세페이지', '규격·재질·관리방법 3종', '표기 내용·이미지 크기'], ['소개 영상', '제품 사용 장면 MP4 2편', '재생·수량·길이 협의']], [100, 205, 202], shaded=[((0, 0), (2, 0))])]
        story += section('예산·이용 범위')
        story += [tbl([['지원액', '자부담', '총액'], [f"{a['requestedAmount']:,}원", f"{a['selfContribution']:,}원", f"{a['totalProjectCost']:,}원"]], [169]*3, shaded=[((0, 0), (2, 0))]), p('초안 수정 2회. 자사 온라인 판매 채널 이용 범위와 원본파일 제공 여부는 계약 전에 명시할 계획입니다.', 'small')]
    else:
        story += [tbl([['신청자', a['businessName'], '대표자', a['representative']], ['번호·개업', form.business_number(a['businessNo'])+' / '+a['openedOn'], '연락처', a['phone']], ['주소·업종', a['address']+' / '+a['businessItem'], '', '']], [80, 180, 80, 177], spans=[((1, 2), (3, 2))], shaded=[((0, 0), (0, 2)), ((2, 0), (2, 2))])]
        story += section('구역별 점검 및 공사 범위')
        story += [tbl([['작업 구역', '점검·공사 내용', '분리·운영 조치'], ['조리 후드', '배기 연결부 점검 후 1식 보완', '조리도구·식재료 이동'], ['작업 바닥', '미끄럼 방지 바닥 15㎡', '작업구역 출입 제한'], ['세척대', '급·배수 연결부 점검·정비', '세척 일정·임시 사용 협의']], [87, 220, 200], shaded=[((0, 0), (2, 0))])]
        story += section('사업비 및 영업중단 계획')
        story += [tbl([['지원 신청액', '자부담', '총 사업비'], [f"{a['requestedAmount']:,}원", f"{a['selfContribution']:,}원", f"{a['totalProjectCost']:,}원"]], [169]*3, shaded=[((0, 0), (2, 0))]), Spacer(1, 8), p('휴무일 공사를 가정합니다. 식재료·조리도구를 작업 구역과 분리하고, 실제 공사일은 작업 범위 확인 뒤 정합니다.', 'small')]
    story += [Spacer(1, 10), p('작성일 '+a['applicationDate']+'  ·  테스트용 가상 신청서', 'right')]
    return story


def plan_page(data):
    d = data['dataset']; pl = data['plan']
    labels = {'digital':['현행 업무와 문제','도입 기능·장비 명세','운영 전환·교육','완료 확인 항목'], 'energy':['기존 설비 기준','교체 대상과 자료 확인','사용시간 기록 방식','사후 확인 방법'], 'access':['위치별 이용 불편','실측·허가·설치 순서','통행 유지 계획','이용 동선 확인'], 'online':['제작 목표와 대상 상품','납품 규격·산출물','수정·검수·권리 협의','파일·수량 검수'], 'safety':['점검 구역과 범위','공사 방법·자재','영업중단·분리계획','작업 후 확인']}[d]
    story = heading(d, '02', {'digital':'도입 전후 업무 전환 계획','energy':'설비 교체 및 사용량 확인 계획','access':'위치별 이용환경 개선 계획','online':'콘텐츠 제작 과업서','safety':'안전·위생 공사 작업계획'}[d], data['label'])
    if d == 'digital':
        story += [tbl([['업무', '현재 처리', '도입 기능', '기대 확인'], ['주문', '종이 전표', 'POS 등록·주방 출력', '누락·재전달 기록'], ['재고', '마감 후 수기', '품목별 화면 관리', '입출고 기록'], ['직원 인계', '구두 전달', '공통 메뉴·절차', '교육 체크']], [70, 130, 177, 130], shaded=[((0, 0), (3, 0))]), Spacer(1, 12)]
    elif d == 'energy':
        story += [tbl([['비교자료', '기존 설비', '교체 설비', '자료 상태'], ['모델명', '기존 냉장 진열장', 'FL-E1 가상 모델', '제품자료 대조 예정'], ['소비전력', '미확인', '미확인', '명판·사양서 확인 전 미확인'], ['사용시간', '미확인', '미확인', '같은 기간으로 기록 예정'], ['조명 수량', '12개', 'LED 12개', '실물·견적 대조 예정']], [85, 145, 145, 132], shaded=[((0, 0), (3, 0))]), Spacer(1, 8), p('사용시간 및 소비전력 확인표: 현재 값은 확인되지 않아 미확인으로 둡니다. 추정 절감량을 계산하거나 확정하지 않습니다.', 'small'), Spacer(1, 10)]
    elif d == 'access':
        story += [tbl([['위치', '조사 항목', '치수·허가 기록', '공사 전 조건'], ['출입구', '단차·통과 폭', '현장 실측 전 미기입', '임대인 설치 동의 확인'], ['서가 사이', '유효 통로 폭', '가구 이동 후 재측정', '진열대 이동 범위 협의'], ['안내 구간', '문자 크기·시야', '설치 높이 확인 예정', '벽체 고정 허가 확인']], [75, 125, 163, 144], shaded=[((0, 0), (3, 0))]), Spacer(1, 10)]
    elif d == 'online':
        story += [tbl([['과업', '납품 규격', '수량', '검수·권리 조건'], ['상품사진', '보정 JPG', '20컷', '색상 기준·원본 제공 협의'], ['상세페이지', '규격·재질·관리 표기', '3종', '초안 후 수정 2회'], ['소개영상', 'MP4', '2편', '자사 채널 이용 범위 합의']], [92, 164, 55, 196], shaded=[((0, 0), (3, 0))]), Spacer(1, 10), p('계약서에 저작재산권 귀속, 사용 기간·매체, 원본 파일 제공 여부, 제3자 폰트·음원 권리를 구분해 적도록 합니다.', 'small'), Spacer(1, 8)]
    else:
        story += [tbl([['구역', '점검·작업', '중단·분리 조치', '완료 확인'], ['조리 후드', '연결부 점검·보완', '조리 중단 / 도구 분리', '작동·연결 상태'], ['작업 바닥', '15㎡ 미끄럼 방지', '구역 통제·건조시간 준수', '시공면적·마감'], ['세척대', '급·배수 점검·정비', '식재료 임시 보관 분리', '누수·배수 확인']], [80, 145, 155, 127], shaded=[((0, 0), (3, 0))]), Spacer(1, 10)]
    for label, key in zip(labels, ['situation','change','operation','outcome']):
        story += section(label)
        story += [tbl([[p(label, 'label')], [p(pl[key])]], [WIDTH], shaded=[((0, 0), (0, 0))]), Spacer(1, 4)]
    return story


def budget_page(data):
    a, q, d = data['application'], data['estimate'], data['dataset']
    story = heading(d, '03', {'digital':'장비·기능별 산출 및 설치 일정','energy':'설비 대조·교체 비용과 확인 일정','access':'위치별 개선비와 현장 추진 일정','online':'제작 단위별 산출·검수 일정','safety':'공사 구역별 산출·작업 일정'}[d], '견적 번호 '+q['quoteNo'])
    rows = [['품목·규격', '수량', '단가', '금액']]
    rows += [[x['name']+'\n'+x['specification'], f"{x['quantity']} {x['unit']}", f"{x['unitPrice']:,}원", f"{x['amount']:,}원"] for x in q['items']]
    rows += [['공급가액', '', '', f"{q['supplyAmount']:,}원"], ['부가세', '', '', f"{q['taxAmount']:,}원"], ['총액', '', '', f"{q['totalAmount']:,}원"]]
    last = len(rows)-3
    story += [tbl(rows, [244, 55, 104, 104], spans=[((0, i), (2, i)) for i in range(last, last+3)], shaded=[((0, 0), (3, 0))])]
    story += section('재원 대조')
    story += [tbl([['지원 신청액', '자부담', '총 사업비', '합계 대조'], [f"{a['requestedAmount']:,}원", f"{a['selfContribution']:,}원", f"{a['totalProjectCost']:,}원", '신청액 + 자부담 = 견적 총액']], [125, 115, 120, 147], shaded=[((0, 0), (3, 0))])]
    story += section('추진 일정')
    story += [tbl([['시점', '작업·확인', '기록물']] + data['schedule'], [65, 275, 167], shaded=[((0, 0), (2, 0))]), Spacer(1, 10), p('가상 계획입니다. 설치·제작·공사·비용 지급이 실제로 이루어졌다는 뜻이 아닙니다.', 'small')]
    return story


def photo_page(data):
    d, label = data['dataset'], NAMES[data['dataset']]
    image_path = SOURCE / d / '현황예시.png'
    story = heading(d, '04', {'digital':'업무 공간 현황·장비 배치','energy':'설비 현황·교체 전 조사','access':'현장 위치·불편 사진대지','online':'상품·촬영 환경 참고자료','safety':'구역별 현황·작업 분리도'}[d], 'AI 생성 현황 예시 / 실제 현장·촬영·시공 증거 아님')
    if d in ('digital','online'):
        photo = Image(str(image_path), width=436, height=290.67)
        story += [tbl([[p('현황 참고 이미지', 'label')], [photo]], [WIDTH], shaded=[((0, 0), (0, 0))]), Spacer(1, 7), p(data['photo']['note'], 'small')]
    elif d == 'energy':
        photo = Image(str(image_path), width=307, height=204.67)
        checklist = tbl([['설비 확인표', '확인값'], ['기존 모델명', '미확인'], ['기존 소비전력', '미확인'], ['사용시간', '미확인'], ['교체 모델자료', '확인 예정'], ['비교기간', '설치 후 기록']], [82, 90], shaded=[((0, 0), (1, 0))])
        story += [tbl([[photo, checklist]], [315, 192]), Spacer(1, 8), p('확인되지 않은 소비전력과 사용시간은 미확인으로 표시했습니다.', 'small')]
    elif d == 'access':
        photo = Image(str(image_path), width=286, height=190.67)
        story += [tbl([[p('현장 예시 이미지', 'label'), p('위치·치수 조사 메모', 'label')], [photo, tbl([['위치','확인 항목'],['출입구','폭·단차 실측'],['서가 사이','유효 통로폭'],['안내판','높이·고정방식'],['허가','임대인 확인']], [75, 100])]], [305, 202]), Spacer(1, 7), p('사진은 치수를 판독하거나 시공 권한을 입증하지 않습니다. 실제 실측과 동의가 선행되어야 합니다.', 'small')]
    else:
        photo = Image(str(image_path), width=300, height=200)
        controls = tbl([['작업 구역', '운영 조치'], ['조리 구역', '작업 중 조리 중단'], ['도구', '작업장 밖 보관'], ['식재료', '덮개·분리 보관'], ['바닥', '통제·건조 후 개방']], [75, 100], shaded=[((0, 0), (1, 0))])
        story += [tbl([[photo, controls]], [310, 197]), Spacer(1, 8), p('구역 표시와 실제 분리 동선은 공사 전에 사업주·시공자가 현장에서 확정합니다.', 'small')]
    story += section('사진과 계획의 연결')
    story += [tbl([['현황 항목', '계획된 확인'], *data['conditions']], [92, 200, 215], shaded=[((0, 0), (2, 0))]), Spacer(1, 7), p(data['photo']['note'], 'small')]
    return story


def certificate_page(data):
    a = data['application']; day = a['openedOn'].split('-')
    story = [p('첨부 서류  A  /  사업자 정보 대조용', 'label'), Spacer(1, 8), p('사업자등록증 예시 (가상)', 'formTitle'),
             p('실제 세무기관 발급 문서가 아니며 자격 증빙으로 사용할 수 없습니다.', 'center'), Spacer(1, 20)]
    rows = [['등록번호', form.business_number(a['businessNo'])], ['상호', a['businessName']], ['성명 (대표자)', a['representative']],
            ['개업연월일', f'{day[0]}년 {day[1]}월 {day[2]}일'], ['사업장 소재지', a['address']],
            ['사업의 종류', f"업태: {a['businessType']}\n종목: {a['businessItem']}"], ['사업자 유형', '개인사업자 (가상 분류)']]
    story += [tbl(rows, [115, 392], [45, 45, 45, 45, 60, 64, 42], shaded=[((0, 0), (0, 6))]), Spacer(1, 18),
              tbl([[p('시험 자료 안내', 'label')], [p('신청서와 상호·대표자·번호·개업일을 대조하기 위한 가상 사례입니다. 실제 개인정보·관인·QR·발급기관 표시는 없습니다.')]], [WIDTH], shaded=[((0, 0), (0, 0))]), Spacer(1, 22), p('2026년 10월 25일  /  마음이음 테스트 자료 제작자 (가상)', 'center')]
    return story


def quote_page(data):
    a, q, d = data['application'], data['estimate'], data['dataset']
    story = heading(d, '첨부', '사업별 견적서 예시', q['quoteNo']+' / 가상 거래 · 청구용 아님')
    rows = [['공급받는 자', '', '공급자', ''], ['상호', a['businessName'], '상호', q['supplierName']], ['대표자', a['representative'], '대표자', q['supplierRepresentative']],
            ['사업자번호', form.business_number(a['businessNo']), '사업자번호', form.business_number(q['supplierBusinessNo'])], ['견적일', q['issuedOn'], '유효기간', q['validUntil']]]
    story += [tbl(rows, [75, 178.5, 75, 178.5], spans=[((0, 0), (1, 0)), ((2, 0), (3, 0))], shaded=[((0, 0), (3, 0))]), Spacer(1, 10)]
    items = [['항목·규격', '수량', '단가', '금액']]
    items += [[x['name']+'\n'+x['specification'], f"{x['quantity']} {x['unit']}", f"{x['unitPrice']:,}원", f"{x['amount']:,}원"] for x in q['items']]
    items += [['공급가액', '', '', f"{q['supplyAmount']:,}원"], ['부가세', '', '', f"{q['taxAmount']:,}원"], ['총 견적금액', '', '', f"{q['totalAmount']:,}원"]]
    start = len(items)-3
    story += [tbl(items, [244, 55, 104, 104], spans=[((0, i), (2, i)) for i in range(start, start+3)], shaded=[((0, 0), (3, 0))]), Spacer(1, 11)]
    story += [tbl([[p('거래 조건·사업별 확인사항', 'label')], [p(q['terms'])]], [WIDTH], shaded=[((0, 0), (0, 0))]), Spacer(1, 9), p('공급업체 확인: '+q['supplierName']+' / '+q['supplierRepresentative']+' (가상 확인)', 'right')]
    return story


def chrome(dataset):
    def draw(c, doc):
        c.saveState(); c.setStrokeColor(INK); c.setLineWidth(.7)
        if dataset in ('digital', 'energy'):
            c.line(44, 799, 551, 799)
        elif dataset == 'access':
            c.line(44, 801, 551, 801); c.line(44, 790, 551, 790)
        elif dataset == 'online':
            c.line(44, 797, 551, 797); c.line(44, 38, 551, 38)
        else:
            c.line(44, 798, 551, 798)
        c.setFont('SampleRegular', 8); c.setFillColor(INK)
        c.drawString(44, 813, '마음이음  |  '+NAMES[dataset]+' 개별 서식')
        c.setFillColor(RED); c.drawRightString(551, 813, '가상 예시 / 공식 서식 아님')
        c.drawString(44, 25, '가상 자료: 실제 신청·증명·계약·지급에 사용할 수 없습니다.')
        c.setFillColor(INK); c.drawRightString(551, 25, f'{doc.page} / 6')
        c.restoreState()
    return draw


def main():
    PDF_DIR.mkdir(parents=True, exist_ok=True); IMAGE_DIR.mkdir(parents=True, exist_ok=True)
    if not (base.WORK / 'SampleRegular.ttf').exists(): base.setup_fonts()
    pdfmetrics.registerFont(TTFont('SampleRegular', str(base.WORK / 'SampleRegular.ttf')))
    pdfmetrics.registerFont(TTFont('SampleBold', str(base.WORK / 'SampleBold.ttf')))
    base.setup_styles()
    base.STYLES['body'].fontSize = 8.5; base.STYLES['body'].leading = 12
    base.STYLES['small'].fontSize = 7.5; base.STYLES['small'].leading = 10
    base.STYLES['heading'].fontSize = 10; base.STYLES['heading'].leading = 14
    base.STYLES['label'].fontSize = 8; base.STYLES['label'].leading = 11
    base.STYLES['right'] = ParagraphStyle('right', fontName='SampleRegular', fontSize=8, leading=11, alignment=TA_RIGHT, wordWrap='CJK')
    base.STYLES['formTitle'] = ParagraphStyle('formTitle', fontName='SampleBold', fontSize=17, leading=21, alignment=TA_LEFT, wordWrap='CJK')
    builders = [app_page, plan_page, budget_page, photo_page, certificate_page, quote_page]
    selected = sys.argv[1:] or list(NAMES)
    for dataset in selected:
        label = NAMES[dataset]
        data = json.loads((SOURCE / dataset / '정답데이터.json').read_text())
        a, q = data['application'], data['estimate']
        assert a['requestedAmount'] + a['selfContribution'] == a['totalProjectCost'] == q['totalAmount']
        assert sum(x['amount'] for x in q['items']) == q['supplyAmount']
        out = PDF_DIR / f'{label}_신청예시_개별서식.pdf'
        story = []
        for index, builder in enumerate(builders):
            if index: story.append(PageBreak())
            story.extend(builder(data))
        SimpleDocTemplate(str(out), pagesize=base.A4, leftMargin=44, rightMargin=44, topMargin=61, bottomMargin=46,
                          title=label+' 신청예시 개별서식', author='마음이음 포트폴리오').build(story, onFirstPage=chrome(dataset), onLaterPages=chrome(dataset))
        reader = PdfReader(out)
        assert len(reader.pages) == 6, (dataset, len(reader.pages))
        for page in reader.pages:
            assert '가상 예시 / 공식 서식 아님' in page.extract_text()
        assert a['businessName'] in reader.pages[0].extract_text()
        images = IMAGE_DIR / dataset; images.mkdir(parents=True, exist_ok=True)
        for number in range(1, 7):
            base.render_page(out, number, images / f'{label}-{number:02}.png', dpi=150)
        with pdfplumber.open(out) as pdf:
            for number, page in enumerate(pdf.pages, 1):
                assert page.width > 0 and page.height > 0
                assert page.chars, f'{dataset} page {number} has no selectable text'
                for char in page.chars:
                    assert char['x0'] >= -0.1 and char['x1'] <= page.width + 0.1, (dataset, number, char)
                    assert char['top'] >= -0.1 and char['bottom'] <= page.height + 0.1, (dataset, number, char)
        print(dataset, out, '6 pages', flush=True)


if __name__ == '__main__':
    main()

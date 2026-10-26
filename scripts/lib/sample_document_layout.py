#!/usr/bin/env python3
"""하나의 정답 자료에서 가상 신청서·증빙 PDF와 서비스용 PNG를 재생성한다.

필요 패키지: reportlab, fonttools. PNG 변환에는 시스템 pdftoppm을 사용한다.
정답 자료와 지정한 출력 경로만 사용한다.
"""
from __future__ import annotations

import hashlib
import json
import shutil
import subprocess
from pathlib import Path
from xml.sax.saxutils import escape

from pypdf import PdfReader
from fontTools.ttLib import TTFont as SourceFont
from fontTools.varLib.instancer import instantiateVariableFont
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_RIGHT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    Flowable, KeepTogether, PageBreak, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle
)

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'fixtures/support-application-v2'
WORK = ROOT / 'tmp/pdfs'
OUTPUT = ROOT / 'output/pdf'
RESOURCES = ROOT / 'src/main/resources/fixtures/support-application-v2'
DATA = json.loads((SOURCE / '정답데이터.json').read_text())
APP = DATA['application']
QUOTE = DATA['estimate']
INK = colors.HexColor('#19364b')
MUTED = colors.HexColor('#566779')
LINE = colors.HexColor('#ccd5df')
PALE = colors.HexColor('#f2f5f8')
PAGE_WIDTH, PAGE_HEIGHT = A4
CONTENT_WIDTH = PAGE_WIDTH - 88


def setup_fonts():
    source = ROOT / 'src/main/resources/fonts/NotoSansKR.ttf'
    for name, weight in [('SampleRegular', 400), ('SampleBold', 700)]:
        target = WORK / f'{name}.ttf'
        font = SourceFont(source)
        instantiateVariableFont(font, {'wght': weight}, inplace=True).save(target)
        font.close()
        pdfmetrics.registerFont(TTFont(name, str(target)))


STYLES = {}


def setup_styles():
    for name, size, leading, font, color, align in [
        ('body', 10, 16, 'SampleRegular', INK, TA_LEFT),
        ('small', 8, 12, 'SampleRegular', MUTED, TA_LEFT),
        ('label', 9, 14, 'SampleBold', INK, TA_LEFT),
        ('heading', 14, 22, 'SampleBold', INK, TA_LEFT),
        ('title', 23, 34, 'SampleBold', INK, TA_LEFT),
        ('center', 10, 16, 'SampleRegular', INK, TA_CENTER),
        ('number', 10, 16, 'SampleRegular', INK, TA_RIGHT),
    ]:
        STYLES[name] = ParagraphStyle(name, fontName=font, fontSize=size, leading=leading,
                                     textColor=color, alignment=align, wordWrap='CJK', spaceAfter=0)


def para(text, style='body'):
    return Paragraph(escape(str(text)).replace('\n', '<br/>'), STYLES[style])


def money(value):
    return f'{value:,}원'


def section(number, title):
    return [Spacer(1, 10), para(f'{number}. {title}', 'heading'), Spacer(1, 8)]


def heading(label, title, description):
    return [para(label, 'small'), Spacer(1, 7), para(title, 'title'), Spacer(1, 8),
            para(description), Spacer(1, 14)]


def grid(rows, widths, header=False):
    converted = [[para(cell, 'label' if header and row_index == 0 else 'body')
                  for cell in row] for row_index, row in enumerate(rows)]
    table = Table(converted, colWidths=widths, hAlign='LEFT', repeatRows=1 if header else 0)
    commands = [
        ('GRID', (0, 0), (-1, -1), .45, LINE),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('LEFTPADDING', (0, 0), (-1, -1), 9),
        ('RIGHTPADDING', (0, 0), (-1, -1), 9),
        ('TOPPADDING', (0, 0), (-1, -1), 5),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 5),
    ]
    if header:
        commands += [('BACKGROUND', (0, 0), (-1, 0), PALE)]
    else:
        commands += [('BACKGROUND', (0, 0), (0, -1), PALE)]
    table.setStyle(TableStyle(commands))
    return table


def notice(text):
    return grid([[text]], [CONTENT_WIDTH])


def chrome(canvas, doc):
    canvas.saveState()
    canvas.setFont('SampleRegular', 8)
    canvas.setFillColor(MUTED)
    canvas.drawString(44, PAGE_HEIGHT - 31, '마음이음  |  소상공인 지원사업 포트폴리오')
    canvas.setFont('SampleBold', 8)
    canvas.drawRightString(PAGE_WIDTH - 44, PAGE_HEIGHT - 31, '테스트용 가상 문서 · 공식 서식 아님')
    canvas.setStrokeColor(LINE)
    canvas.line(44, PAGE_HEIGHT - 39, PAGE_WIDTH - 44, PAGE_HEIGHT - 39)
    canvas.line(44, 42, PAGE_WIDTH - 44, 42)
    canvas.setFont('SampleRegular', 8)
    canvas.drawString(44, 28, '실제 신청·증명·계약에 사용할 수 없습니다.  /  모든 정보는 가상입니다.')
    canvas.drawRightString(PAGE_WIDTH - 44, 28, f'{doc.page}')
    canvas.restoreState()


def build(path, story):
    SimpleDocTemplate(str(path), pagesize=A4, leftMargin=44, rightMargin=44,
                      topMargin=61, bottomMargin=59, title='지원사업 정상 신청 예시 문서',
                      author='마음이음 포트폴리오', pageCompression=1).build(
                          story, onFirstPage=chrome, onLaterPages=chrome)



def render_page(pdf, page, target, dpi=150):
    """OCR 입력용 페이지 이미지를 원본 페이지 비율 그대로 만든다."""
    target.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(['pdftoppm', '-f', str(page), '-l', str(page), '-r', str(dpi), '-singlefile',
                    '-png', str(pdf), str(target.with_suffix(''))], check=True)

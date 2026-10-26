#!/usr/bin/env python3
"""기존 스캔 원본을 보존하고 웹 업로드용 이미지 PDF 사본을 준비한다."""
from pathlib import Path
import sys, base64, io
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "ocr"))
from pdf_document import PdfDocument, run
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
SOURCES={
    'digital':'사업별신청예시-개별서식/디지털전환_신청예시_비정형스캔.pdf',
    'energy':'사업별신청예시-개별서식/에너지절감_신청예시_비정형스캔.pdf',
    'access':'사업별신청예시-개별서식/이용환경개선_신청예시_비정형스캔.pdf',
    'online':'사업별신청예시-개별서식/온라인판로_신청예시_비정형스캔.pdf',
    'safety':'사업별신청예시-개별서식/안전위생_신청예시_비정형스캔.pdf',
    'original':'지원사업_신청서류_개정2판_이미지형.pdf',
    'comparison':'지원사업_비교용_대체입력.pdf',
}
for code,relative in SOURCES.items():
    source=ROOT/'output/pdf'/relative
    destination=ROOT/'src/main/resources/fixtures'/code/'application-scan.pdf'
    run('scan',source,destination)
    with PdfDocument(destination.read_bytes()) as result:
        assert destination.stat().st_size<10*1024*1024
        preview=ROOT/'src/main/webapp/assets/samples'/f'{code}-scan.jpg'
        preview.parent.mkdir(parents=True,exist_ok=True)
        first=None
        for page in result.pages(dpi=30):
            assert not page['lines']
            if first is None: first=page
        with Image.open(io.BytesIO(base64.b64decode(first['preview']))) as image:
            image.save(preview,'JPEG',quality=85)
        print(code,len(result),destination.stat().st_size)

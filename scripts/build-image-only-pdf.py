#!/usr/bin/env python3
"""최종 PDF를 200dpi 페이지 이미지로 변환해 텍스트 층 없는 PDF로 합성한다."""
from pathlib import Path
import hashlib
import json
import subprocess

from pypdf import PdfReader
from reportlab.pdfgen.canvas import Canvas

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'output/pdf/지원사업_신청서류_개정2판.pdf'
OUTPUT = ROOT / 'output/pdf/지원사업_신청서류_개정2판_이미지형.pdf'
PAGES = ROOT / 'fixtures/support-application-v2/image-only'
DPI = 200


def main():
    PAGES.mkdir(parents=True, exist_ok=True)
    original = PdfReader(SOURCE)
    subprocess.run(['pdftoppm', '-r', str(DPI), '-png', str(SOURCE), str(PAGES/'page')], check=True)
    canvas = Canvas(str(OUTPUT), pageCompression=1)
    canvas.setTitle('마음이음 지원사업 신청서류 개정 2판 - 이미지형')
    canvas.setAuthor('마음이음 포트폴리오')
    images = sorted(PAGES.glob('page-*.png'))
    assert len(images) == len(original.pages) == 7
    for page, image in zip(original.pages, images):
        width, height = float(page.mediabox.width), float(page.mediabox.height)
        canvas.setPageSize((width, height))
        canvas.drawImage(str(image), 0, 0, width=width, height=height)
        canvas.showPage()
    canvas.save()
    result = PdfReader(OUTPUT)
    assert len(result.pages) == 7
    for before, after in zip(original.pages, result.pages):
        assert not after.extract_text().strip(), '텍스트 층이 남았습니다.'
        assert len(after.images) == 1, '페이지당 한 장의 이미지여야 합니다.'
        assert list(before.mediabox) == list(after.mediabox)
    manifest = {'source':str(SOURCE.relative_to(ROOT)), 'output':str(OUTPUT.relative_to(ROOT)),
                'dpi':DPI, 'pages':7, 'textLayer':False, 'files':{}}
    for path in [SOURCE, OUTPUT, *images]:
        manifest['files'][str(path.relative_to(ROOT))] = {'sha256':hashlib.sha256(path.read_bytes()).hexdigest(), 'bytes':path.stat().st_size}
    (PAGES/'생성목록.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2)+'\n')
    print(OUTPUT)
    print(f'7쪽 / 페이지당 이미지 1장 / 텍스트 층 없음 / {OUTPUT.stat().st_size:,} bytes')


if __name__ == '__main__':
    main()

#!/usr/bin/env python3
"""검토 완료한 img2img 페이지를 순서대로 묶는다. 이미지 자체는 수정하지 않는다."""
from __future__ import annotations

import hashlib
import json
from pathlib import Path

from PIL import Image
from pypdf import PdfReader
from reportlab.lib.pagesizes import A4
from reportlab.pdfgen import canvas

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / '.ref/document-scans/편집작업기록.json'
OUTPUT = ROOT / 'output/pdf/사업별신청예시-개별서식'
NAMES = {'digital': '디지털전환', 'energy': '에너지절감', 'access': '이용환경개선',
         'online': '온라인판로', 'safety': '안전위생'}


def main():
    manifest = json.loads(MANIFEST.read_text())
    verification = []
    OUTPUT.mkdir(parents=True, exist_ok=True)
    for dataset, label in NAMES.items():
        pages = sorted((entry for entry in manifest['pages'] if entry['dataset'] == dataset),
                       key=lambda entry: entry['page'])
        if [entry['page'] for entry in pages] != list(range(1, 7)):
            raise ValueError(f'{dataset}: 여섯 페이지가 필요합니다.')
        pdf = OUTPUT / f'{label}_신청예시_비정형스캔.pdf'
        document = canvas.Canvas(str(pdf), pagesize=A4)
        document.setTitle(f'{label} 가상 신청 예시 - AI 편집 비정형 스캔')
        document.setAuthor('마음이음 포트폴리오')
        for entry in pages:
            if entry.get('reviewStatus') != 'reviewed':
                raise ValueError(f"{dataset} {entry['page']}: 이미지 검토가 필요합니다.")
            source_path = ROOT / entry['sourcePath']
            if hashlib.sha256(source_path.read_bytes()).hexdigest() != entry['sourceSha256']:
                raise ValueError(f'{source_path}: 편집 입력이 바뀌어 다시 검토해야 합니다.')
            image_path = ROOT / entry['outputPath']
            if hashlib.sha256(image_path.read_bytes()).hexdigest() != entry['outputSha256']:
                raise ValueError(f'{image_path}: 검토한 이미지와 해시가 다릅니다.')
            with Image.open(image_path) as image:
                width, height = image.size
                if width < 1000 or height < 1400:
                    raise ValueError(f'{image_path}: 문서 검토에 필요한 해상도가 부족합니다.')
            # 출력의 기울어짐·여백을 그대로 보존하고 PDF 종이 안에 비율대로 넣는다.
            scale = min(A4[0] / width, A4[1] / height)
            draw_width, draw_height = width * scale, height * scale
            document.drawImage(str(image_path), (A4[0] - draw_width) / 2,
                               (A4[1] - draw_height) / 2, width=draw_width, height=draw_height)
            document.showPage()
        document.save()
        reader = PdfReader(pdf)
        assert len(reader.pages) == 6
        assert all(not page.extract_text().strip() for page in reader.pages)
        verification.append({'dataset': dataset, 'pages': 6, 'pdf': str(pdf.relative_to(ROOT)),
                             'sha256': hashlib.sha256(pdf.read_bytes()).hexdigest(),
                             'textLayer': False, 'method': '기본 제공 이미지 편집 도구 img2img 6장 합성'})
        print(f'{label}: 비정형 스캔 PDF 6쪽 생성', flush=True)
    (OUTPUT / '비정형스캔검증.json').write_text(json.dumps(verification, ensure_ascii=False, indent=2) + '\n')


if __name__ == '__main__':
    main()

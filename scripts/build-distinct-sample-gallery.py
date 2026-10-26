#!/usr/bin/env python3
"""개별 서식과 AI 편집 스캔본을 나란히 검토하는 로컬 정적 페이지를 만든다."""
import html
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DESTINATION = ROOT / 'output/visual-direction'
LABELS = {'digital': '디지털전환', 'energy': '에너지절감', 'access': '이용환경개선',
          'online': '온라인판로', 'safety': '안전위생'}
DIFFERENCES = {
    'digital': '도입 전·후 업무 비교와 주문·재고 기능 / 장비·설정·교육 구분',
    'energy': '기존·교체 설비 대조와 소비전력·사용 시간 확인 / 설치·철거 포함',
    'access': '위치별 불편·치수 조사와 설치 조건 / 사진·동선 확인',
    'online': '제작 과업·납품 규격과 수정·이용 범위 / 사진·페이지·영상별 산출',
    'safety': '작업 구역별 점검과 공사 범위 / 영업·식재료 분리 계획',
}


def main():
    sections = []
    for dataset, label in LABELS.items():
        data = json.loads((ROOT / f'fixtures/program-samples/{dataset}/정답데이터.json').read_text())
        target = DESTINATION / 'distinct-samples' / dataset
        target.mkdir(parents=True, exist_ok=True)
        downloads = []
        for suffix, caption in [('개별서식', '개별 서식 PDF'), ('비정형스캔', 'AI 편집 스캔 PDF')]:
            source = ROOT / f'output/pdf/사업별신청예시-개별서식/{label}_신청예시_{suffix}.pdf'
            if source.exists():
                shutil.copy2(source, target / source.name)
                downloads.append(f'<a class="button" href="distinct-samples/{dataset}/{source.name}" download>{caption}</a>')
        rows = []
        for source_name, caption, prefix in [('개별서식', '작성 완료 원본', 'clean'), ('비정형스캔', 'img2img 비정형 스캔', 'scan')]:
            images = []
            for number in range(1, 7):
                source = ROOT / f'output/images/사업별신청예시-{source_name}/{dataset}/{label}-{number:02}.png'
                if source.exists():
                    filename = f'{prefix}-{number:02}.png'
                    shutil.copy2(source, target / filename)
                    url = f'distinct-samples/{dataset}/{filename}'
                    alt = html.escape(f'{label} {caption} {number}쪽')
                    images.append(f'<a href="{url}" target="_blank"><img src="{url}" alt="{alt}"><span>{number}쪽</span></a>')
            rows.append(f'<h3>{caption}</h3><div class="pages">{"".join(images)}</div>')
        sections.append(f'<section><div class="section-title"><div><span class="category">{label}</span><h2>{html.escape(data["label"])}</h2></div><div class="downloads">{"".join(downloads)}</div></div><p>{DIFFERENCES[dataset]}</p>{"".join(rows)}</section>')
    page = '''<!doctype html><html lang="ko"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>사업별 개별 서식·비정형 스캔</title><style>
    *{box-sizing:border-box}body{font-family:system-ui,sans-serif;margin:0;background:#f1f3f7;color:#222c3b;line-height:1.6}main{max-width:1500px;margin:auto;padding:40px}h1{font-size:30px;margin-bottom:8px}h2{font-size:21px;margin:6px 0}h3{font-size:15px;margin:25px 0 12px}.intro{margin-bottom:30px;color:#536174}.notice{border-left:3px solid #415e86;padding:10px 16px;background:#fff}.category{color:#315781;font-weight:700;font-size:13px}section{background:white;border:1px solid #dce1e9;border-radius:14px;margin:26px 0;padding:28px}.section-title{display:flex;align-items:center;justify-content:space-between;gap:20px}.downloads{display:flex;gap:10px;flex-wrap:wrap}.button{padding:10px 14px;border-radius:5px;background:#edf2fa;color:#234567;text-decoration:none;font-size:13px}.pages{display:grid;grid-template-columns:repeat(6,minmax(0,1fr));gap:12px}.pages a{color:inherit;text-decoration:none;text-align:center;font-size:12px}.pages img{width:100%;height:auto;aspect-ratio:210/297;object-fit:contain;border:1px solid #e1e4e9;display:block;margin-bottom:8px}p{font-size:14px}@media(max-width:850px){main{padding:20px}.pages{grid-template-columns:repeat(3,minmax(0,1fr))}.section-title{display:block}.downloads{margin-top:15px}section{padding:18px}}@media(max-width:450px){.pages{grid-template-columns:repeat(2,minmax(0,1fr))}h1{font-size:24px}}
    </style><main><h1>사업별로 달라진 신청 서식과 스캔본</h1><p class="intro">5개 사업 · 각 6쪽 · 원본과 img2img 편집본 비교 · 이미지 클릭 시 확대</p><p class="notice">모두 가상 자료입니다. 아래 스캔본은 이미지 생성 도구로 편집한 테스트 이미지이며 실제 촬영·스캔이 아닙니다. 이름·금액 등 정답은 작성 완료 원본을 기준으로 확인하세요.</p>'''+''.join(sections)+'</main></html>'
    DESTINATION.mkdir(parents=True, exist_ok=True)
    (DESTINATION / 'distinct-samples.html').write_text(page)
    print(DESTINATION / 'distinct-samples.html')


if __name__ == '__main__':
    main()

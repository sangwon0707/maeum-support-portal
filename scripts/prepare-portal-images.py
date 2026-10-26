#!/usr/bin/env python3
"""생성 원본을 보존하고 웹 전송용 WebP만 별도로 만든다."""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/webapp/assets/images'
SOURCES = {name: ASSETS / f'{name}.png' for name in [
    'neighborhood-renewal-v1', 'support-campaign-v1', 'guide-apply-v1',
    'guide-upload-v1', 'guide-review-v1', 'guide-result-v1']}
SOURCES.update({f'program-{dataset}-v1': ROOT / f'fixtures/program-samples/{dataset}/현황예시.png'
                for dataset in ['digital', 'energy', 'access', 'online', 'safety']})

for name, source in SOURCES.items():
    destination = ASSETS / f'{name}.webp'
    subprocess.run(['cwebp', '-quiet', '-q', '82', '-metadata', 'none', str(source),
                    '-o', str(destination)], check=True)
    print(f'{destination.name}: {destination.stat().st_size // 1024}KiB')

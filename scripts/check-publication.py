#!/usr/bin/env python3
"""새 저장소에 넣을 파일의 제외 규칙·비밀값·고지·문서 링크를 검사한다."""
import json
from pathlib import Path
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
PRIVATE_ROOTS = ('.ref/', '.local/', '.tools/', '.archive/', 'target/', 'output/',
                 'pdf-engine/target/')
TEXT_SUFFIXES = {'.md', '.json', '.xml', '.yml', '.yaml', '.py', '.java', '.js',
                 '.css', '.jsp', '.tag', '.tld', '.sql', '.sh', '.txt', '.html', '.example'}
SECRET_PATTERN = re.compile(
    rb'nvapi-[A-Za-z0-9_-]{20,}|sk-or-v1-[a-f0-9]{32,}|'
    rb'-----BEGIN (?:RSA |OPENSSH |EC )?PRIVATE KEY-----')


def candidate_paths():
    result = subprocess.check_output(
        ['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z'], cwd=ROOT)
    return sorted({name for name in result.decode().split('\0')
                   if name and (ROOT / name).is_file()})


def private_values():
    values = set()
    for name in ('.env.local', '.env.ai.local', '.local/oracle-maeum.env'):
        path = ROOT / name
        if not path.is_file():
            continue
        for line in path.read_text().splitlines():
            if line.startswith('#') or '=' not in line:
                continue
            key, value = line.split('=', 1)
            value = value.strip().strip('\"\'')
            if re.search(r'API_KEY|TOKEN|SECRET', key) and len(value) >= 20:
                values.add(value.encode())
    return values


def inspect():
    names = candidate_paths()
    exported = set(names)
    failures = []
    secrets = private_values()
    for name in names:
        path = ROOT / name
        if name.startswith(PRIVATE_ROOTS) or name.split('/')[0].endswith('-main'):
            failures.append((name, '로컬 보존 전용 경로가 추적 대상에 포함됨'))
        if (path.name.startswith('.env') and not path.name.endswith('.example')) or path.suffix in {'.pem', '.key', '.p12', '.pfx'}:
            failures.append((name, '실행 비밀 설정 또는 인증 파일'))
        if path.is_symlink() and not path.resolve().is_relative_to(ROOT):
            failures.append((name, '프로젝트 밖을 가리키는 심볼릭 링크'))
        content = path.read_bytes()
        if SECRET_PATTERN.search(content) or any(value in content for value in secrets):
            failures.append((name, '공급자 키·개인키·실제 로컬 비밀값 의심. 값은 출력하지 않음'))
        if path.suffix not in TEXT_SUFFIXES:
            continue
        text = content.decode('utf-8', errors='replace')
        if path.suffix == '.md':
            for target in re.findall(r'\]\(([^)]+)\)', text):
                if target.startswith(('https:', 'http:', 'mailto:', '#')):
                    continue
                target = target.split('#')[0]
                if not target:
                    continue
                resolved = (path.parent / target).resolve()
                relative = resolved.relative_to(ROOT).as_posix() if resolved.is_relative_to(ROOT) else None
                included = relative in exported or (relative is not None and any(
                    item.startswith(relative.rstrip('/') + '/') for item in exported))
                if not included:
                    failures.append((name, '공개 대상에 없는 상대 링크: ' + target))
        if path.suffix == '.css':
            for target in re.findall(r'url\([\"\']?([^\)\"\']+)', text):
                if target.startswith(('data:', 'https:', 'http:', '#', '/')):
                    continue
                resolved = (path.parent / target.split('?')[0].split('#')[0]).resolve()
                if not resolved.is_file():
                    failures.append((name, '없는 CSS 자산 참조: ' + target))
    required = {
        'THIRD_PARTY_NOTICES.md': 'Pretendard',
        'src/main/webapp/assets/krds/fonts/OFL.txt': 'SIL OPEN FONT LICENSE',
        'src/main/webapp/assets/krds/fonts/NOTICE.txt': 'Kil Hyung-jin',
        'src/main/webapp/assets/krds/css/plugin/LICENSE.swiper.txt': 'Permission is hereby granted',
        'src/main/resources/fonts/OFL.txt': 'SIL OPEN FONT LICENSE',
        'pdf-engine/LICENSE': 'Apache License',
        'pdf-engine/NOTICE': 'Apache PDFBox',
    }
    for name, marker in required.items():
        if name not in exported or marker not in (ROOT / name).read_text():
            failures.append((name, '필수 저작권·라이선스 고지 누락'))
    if any('/favicon/' in name and '/krds/' in name for name in names):
        failures.append(('assets/krds', '제외한 정부 상징 파비콘이 다시 포함됨'))
    return names, failures


if __name__ == '__main__':
    names, failures = inspect()
    report = {'candidateCount': len(names), 'failures': failures,
              'scope': '현재 파일·추적 목록 검사. 과거 Git 이력 및 이미지 픽셀 OCR 검사는 별도.'}
    output = ROOT / '.local/publication-check.json'
    output.parent.mkdir(exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    for path, reason in failures:
        print(f'{path}: {reason}')
    print(f'공개 대상 {len(names)}개 / 문제 {len(failures)}건')
    sys.exit(bool(failures))

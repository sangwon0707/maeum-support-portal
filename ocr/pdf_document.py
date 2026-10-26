"""PDFBox Java 모듈의 제한된 프로세스 계약. 셸·외부 주소·문서 명령은 실행하지 않는다."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import threading

ROOT = Path(__file__).resolve().parents[1]
MAX_BYTES = 10 * 1024 * 1024


def command(action, source, *arguments):
    configured = os.getenv('PDF_JAVA')
    local = ROOT / '.tools/amazon-corretto-21.jdk/Contents/Home/bin/java'
    java = configured or (str(local) if local.exists() else shutil.which('java'))
    jar = Path(os.getenv('PDF_ENGINE_JAR') or ROOT / 'pdf-engine/target/pdf-engine.jar')
    if not java or not jar.is_file():
        raise RuntimeError('PDF_ENGINE_NOT_INSTALLED')
    return [java, '-Xmx128m', '-XX:+UseSerialGC', '-Djava.awt.headless=true', '-Dfile.encoding=UTF-8',
            '-jar', str(jar), action, str(source), *map(str, arguments)]


def run(action, source, *arguments):
    try:
        result = subprocess.run(command(action, source, *arguments), capture_output=True, timeout=120, check=True)
        return result.stdout
    except (subprocess.SubprocessError, OSError):
        raise ValueError('문서 형식·암호·페이지 수를 확인해 주세요.') from None


def verify_runtime():
    result = json.loads(run('version', 'unused'))
    if result != {'engine': 'Apache PDFBox', 'version': '3.0.8'}:
        raise RuntimeError('PDF_ENGINE_VERSION_MISMATCH')
    return result


class PdfDocument:
    def __init__(self, content):
        if not content or len(content) > MAX_BYTES:
            raise ValueError('파일당 10MiB 이하로 선택해 주세요.')
        self.folder = tempfile.TemporaryDirectory(prefix='maeum-pdf-')
        self.path = Path(self.folder.name) / 'source'
        try:
            self.path.write_bytes(content)
            os.chmod(self.path, 0o600)
            self.count = json.loads(run('validate', self.path))['pageCount']
        except BaseException:
            self.folder.cleanup()
            raise

    def __enter__(self):
        return self

    def __exit__(self, *args):
        self.close()

    def __len__(self):
        return self.count

    def close(self):
        self.folder.cleanup()

    def split(self, selected):
        if (not selected or len(selected) > 12 or len(set(selected)) != len(selected)
                or any(type(index) is not int or index < 0 or index >= self.count for index in selected)):
            raise ValueError('분할할 페이지를 확인해 주세요.')
        return run('split', self.path, ','.join(str(index+1) for index in selected))

    def pages(self, dpi=120):
        process = subprocess.Popen(command('render', self.path, dpi), stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
        try:
            seen = 0
            while True:
                # OCR을 기다리는 동안 JVM을 죽이지 않고, 실제 PDF 응답 읽기에만 제한을 건다.
                timer = threading.Timer(120, process.kill)
                timer.start()
                try:
                    line = process.stdout.readline(20 * 1024 * 1024 + 1)
                finally:
                    timer.cancel()
                if not line:
                    break
                if len(line) > 20 * 1024 * 1024:
                    raise ValueError('페이지 데이터가 너무 큽니다.')
                event = json.loads(line)
                if event['event'] == 'page':
                    seen += 1
                    if seen > self.count:
                        raise ValueError('페이지 수가 일치하지 않습니다.')
                    yield event
            if process.wait(timeout=5) != 0 or seen != self.count:
                raise ValueError('PDF 페이지 처리를 완료하지 못했습니다.')
        finally:
            if process.poll() is None:
                process.kill()
            process.wait()
            process.stdout.close()

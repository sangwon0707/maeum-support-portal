"""내부 전용 OCR API. 한 프로세스에서 모델 한 개를 사용하며 중복 추론을 거절한다."""
import asyncio
from contextlib import asynccontextmanager
import hmac
import io
import os
import json

from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import StreamingResponse
from document_analyzer import DocumentAnalyzer
import numpy as np
from PIL import Image, UnidentifiedImageError
from starlette.concurrency import run_in_threadpool

from ai import AiAssistant
from document_ai import DocumentAi
from pdf_document import verify_runtime
from engine import PaddleEngine

MAX_BYTES = 10 * 1024 * 1024
Image.MAX_IMAGE_PIXELS = 10_000_000


def create_app(engine_factory=PaddleEngine):
    @asynccontextmanager
    async def lifespan(app):
        token = os.getenv("OCR_API_TOKEN", "")
        if len(token) < 32:
            raise RuntimeError("OCR_API_TOKEN은 32자 이상으로 설정해야 합니다.")
        app.state.token = token
        app.state.pdf = await run_in_threadpool(verify_runtime)
        app.state.engine = await run_in_threadpool(engine_factory)
        app.state.ai = AiAssistant()
        app.state.busy = asyncio.Lock()
        yield

    app = FastAPI(title="문서 OCR 내부 API", lifespan=lifespan, docs_url=None, redoc_url=None, openapi_url=None)

    def authorize(request):
        supplied = request.headers.get("authorization", "")
        if not hmac.compare_digest(supplied.encode(), f"Bearer {app.state.token}".encode()):
            raise HTTPException(401, "인증이 필요합니다.")

    @app.get("/health")
    async def health(request: Request):
        authorize(request)
        return {"status": "ready", "engine": "paddleocr", "aiEnabled": app.state.ai.enabled, "pdf": app.state.pdf}

    @app.post("/v1/recognize")
    async def recognize(request: Request, assist: bool = False):
        authorize(request)
        if app.state.busy.locked():
            raise HTTPException(429, "다른 문서를 처리 중입니다.")
        async with app.state.busy:
            content = bytearray()
            async for chunk in request.stream():
                content.extend(chunk)
                if len(content) > MAX_BYTES:
                    raise HTTPException(413, "10MiB 이하의 이미지만 지원합니다.")
            def process():
                try:
                    with Image.open(io.BytesIO(content)) as source:
                        if source.format not in {"PNG", "JPEG"} or source.width * source.height > 10_000_000:
                            raise HTTPException(422, "PNG/JPEG 이미지 크기를 확인해 주세요.")
                        image = np.array(source.convert("RGB"))[:, :, ::-1].copy()
                except (UnidentifiedImageError, OSError, Image.DecompressionBombError):
                    raise HTTPException(422, "이미지를 읽을 수 없습니다.")
                try:
                    result = app.state.engine.extract(image)
                except Exception:
                    raise HTTPException(503, "문자 인식 처리를 완료하지 못했습니다.") from None
                if len(result["texts"]) > 500 or any(len(line["text"]) > 2000 for line in result["texts"]):
                    raise HTTPException(422, "인식할 문서의 범위를 줄여 주세요.")
                result["ai"] = app.state.ai.suggest(result["texts"]) if assist else {"status": "NOT_REQUESTED", "fields": {}}
                return result
            return await run_in_threadpool(process)

    @app.post("/v1/analyze-document")
    async def analyze_document(request: Request, kind: str = "AUTO", assist: bool = False):
        authorize(request)
        if app.state.busy.locked():
            raise HTTPException(429, "다른 문서를 처리 중입니다.")
        content = bytearray()
        async for chunk in request.stream():
            content.extend(chunk)
            if len(content) > MAX_BYTES:
                raise HTTPException(413, "파일당 10MiB 이하로 선택해 주세요.")
        if app.state.busy.locked():
            raise HTTPException(429, "다른 문서를 처리 중입니다.")
        analyzer = DocumentAnalyzer(app.state.engine)
        try:
            document = analyzer.open(bytes(content))
            document.close()
        except ValueError as error:
            raise HTTPException(422, str(error)) from None
        await app.state.busy.acquire()
        loop = asyncio.get_running_loop()
        def stream():
            try:
                pages = []
                for event in analyzer.events(bytes(content), kind):
                    if event['event'] == 'page':
                        pages.append(event)
                    if event['event'] == 'complete' and assist:
                        yield json.dumps({'event': 'stage', 'stage': 'AI_REVIEW', 'pageNumber': len(pages), 'pageCount': len(pages)}) + "\n"
                        result = DocumentAi(app.state.ai).analyze(pages)
                        yield json.dumps({'event': 'ai', **result}, ensure_ascii=False) + "\n"
                    yield json.dumps(event, ensure_ascii=False) + "\n"
            except ValueError:
                yield json.dumps({"event":"error", "code":"INVALID_DOCUMENT"}) + "\n"
            except Exception:
                yield json.dumps({"event":"error", "code":"DOCUMENT_ANALYSIS_FAILED"}) + "\n"
            finally:
                loop.call_soon_threadsafe(app.state.busy.release)
        return StreamingResponse(stream(), media_type="application/x-ndjson")

    @app.post("/v1/split-document")
    async def split_document(request: Request, pages: str):
        authorize(request)
        if app.state.busy.locked():
            raise HTTPException(429, "다른 문서를 처리 중입니다.")
        async with app.state.busy:
            content = bytearray()
            async for chunk in request.stream():
                content.extend(chunk)
                if len(content) > MAX_BYTES:
                    raise HTTPException(413, "파일당 10MiB 이하로 선택해 주세요.")
            def split():
                try:
                    selected = [int(value)-1 for value in pages.split(",")]
                    if not selected or len(selected)>12 or len(set(selected))!=len(selected):
                        raise ValueError()
                    with DocumentAnalyzer(app.state.engine).open(bytes(content)) as document:
                        if any(index<0 or index>=len(document) for index in selected): raise ValueError()
                        return document.split(selected)
                except Exception:
                    raise HTTPException(422, "분할할 페이지를 확인해 주세요.") from None
            result = await run_in_threadpool(split)
        from fastapi.responses import Response
        return Response(result, media_type="application/pdf")

    return app


app = create_app()

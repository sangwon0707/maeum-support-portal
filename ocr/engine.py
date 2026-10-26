"""PaddleOCR 출력만 전달한다. 업무 정답·신청 정보는 이 모듈에 전달하지 않는다."""
from importlib.metadata import version
import time
import os


class PaddleEngine:
    def __init__(self):
        from paddleocr import PaddleOCR

        self.recognizer = PaddleOCR(
            text_detection_model_name="PP-OCRv5_mobile_det",
            text_recognition_model_name="korean_PP-OCRv5_mobile_rec",
            use_doc_orientation_classify=False,
            use_doc_unwarping=False,
            use_textline_orientation=False,
            device="cpu",
            cpu_threads=max(1, min(4, int(os.getenv("OCR_CPU_THREADS", "4")))),
            enable_mkldnn=False,
        )

    def extract(self, image):
        started = time.monotonic()
        texts = []
        for prediction in self.recognizer.predict(image):
            result = prediction.json["res"]
            for text, score, polygon in zip(
                result["rec_texts"], result["rec_scores"], result["rec_polys"]
            ):
                texts.append({"text": text, "confidence": float(score), "polygon": polygon})
        return {"engine": "paddleocr", "modelVersion": "PP-OCRv5_mobile_det+korean_PP-OCRv5_mobile_rec",
                "packageVersion": version("paddleocr"), "imageWidth": image.shape[1], "imageHeight": image.shape[0],
                "durationMs": round((time.monotonic() - started) * 1000), "texts": texts}

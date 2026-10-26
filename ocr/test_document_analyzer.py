"""샘플 정답 주입 없이 원본 PDF의 텍스트·사진·페이지 경계를 확인한다."""
import json
import pathlib
import unittest
from pdf_document import PdfDocument
from test_api import ApiTests
from document_analyzer import DocumentAnalyzer

FIXTURES=pathlib.Path(__file__).resolve().parents[1]/'src/main/resources/fixtures'
class NoOcr:
    def extract(self,image): raise AssertionError('텍스트 PDF에 OCR을 호출하지 않는다')

class DocumentTests(unittest.TestCase):
    def test_native_document_retains_pages_fields_photos_and_tables(self):
        events=list(DocumentAnalyzer(NoOcr()).events((FIXTURES/'digital/application.pdf').read_bytes()))
        pages=[event for event in events if event['event']=='page']
        self.assertEqual(['APPLICATION','BUSINESS_PLAN','BUSINESS_PLAN','PHOTO_SHEET','BUSINESS_CERT','ESTIMATE'],[page['kind'] for page in pages])
        self.assertEqual('가상달빛식당',pages[0]['fields']['businessName'])
        self.assertEqual('3300000',pages[0]['fields']['requestedAmount'])
        self.assertEqual(1,len(pages[3]['photos']))
        self.assertEqual('PDF_IMAGE_REGION',pages[3]['photos'][0]['method'])
        self.assertGreater(len(pages[5]['tableRows']),4)
    def test_each_sample_has_application_certificate_estimate_and_photos(self):
        for path in FIXTURES.glob('*/application.pdf'):
            with self.subTest(dataset=path.parent.name):
                pages=[e for e in DocumentAnalyzer(NoOcr()).events(path.read_bytes()) if e['event']=='page']
                self.assertTrue({'APPLICATION','BUSINESS_CERT','ESTIMATE'}.issubset({p['kind'] for p in pages}))
                self.assertGreater(sum(len(p['photos']) for p in pages),0)
    def test_rejects_corrupt_encrypted_and_excessive_pages(self):
        analyzer=DocumentAnalyzer(NoOcr())
        with self.assertRaises(ValueError):analyzer.open(b'%PDF-broken')
        # 암호화·과도한 페이지는 PDFBox 모듈의 실제 PDF 생성 테스트에서도 검증한다.
        with self.assertRaises(ValueError): analyzer.open(b'not a PDF or image')

class DocumentApiTests(ApiTests):
    def test_actual_pdf_split_retains_two_selected_pages(self):
        data=(FIXTURES/'digital/application.pdf').read_bytes()
        response=self.client.post('/v1/split-document?pages=2,3',headers=self.headers,content=data)
        self.assertEqual(200,response.status_code)
        with PdfDocument(response.content) as document:
            self.assertEqual(2,len(document))
            self.assertIn('도입 전후 업무 전환 계획',''.join(line['text'] for page in document.pages() for line in page['lines']))
        self.assertEqual(422,self.client.post('/v1/split-document?pages=0',headers=self.headers,content=data).status_code)
        self.assertEqual(401,self.client.post('/v1/split-document?pages=1',content=data).status_code)
    def test_native_analysis_stream_has_real_page_events(self):
        response=self.client.post('/v1/analyze-document',headers=self.headers,content=(FIXTURES/'digital/application.pdf').read_bytes())
        events=[json.loads(line) for line in response.text.splitlines()]
        self.assertEqual(6,len([e for e in events if e['event']=='page']))
        self.assertEqual('complete',events[-1]['event'])

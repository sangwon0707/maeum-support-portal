import unittest
from document_ai import DocumentAi

class DocumentAiTests(unittest.TestCase):
    def page(self):
        return {'pageNumber':1,'kind':'APPLICATION','fields':{'businessName':'기존상호'},
                'lines':[{'text':'상호 가상가게','bbox':[0,0,100,20],'source':'PADDLE_OCR'}]}

    def test_source_verified_candidate_records_difference(self):
        metadata={'verifiedCount':0,'rejectedCount':0,'conflicts':[],'pageResults':[]}
        DocumentAi.reconcile([self.page()],{'pages':[{'pageNumber':1,'kind':'APPLICATION','kindLineIndex':0,
            'fields':{'businessName':{'value':'가상가게','lineIndex':0},
                      'representative':{'value':'없는이름','lineIndex':0}}}]},metadata)
        self.assertEqual(1,metadata['verifiedCount'])
        self.assertEqual(1,metadata['rejectedCount'])
        self.assertEqual('기존상호',metadata['conflicts'][0]['previous'])

    def test_duplicate_page_and_boolean_index_rejected(self):
        metadata={'verifiedCount':0,'rejectedCount':0,'conflicts':[],'pageResults':[]}
        DocumentAi.reconcile([self.page()],{'pages':[{'pageNumber':1,
            'fields':{'businessName':{'value':'가상가게','lineIndex':True}}}]},metadata)
        self.assertEqual(0,metadata['verifiedCount'])
        with self.assertRaises(ValueError):
            DocumentAi.reconcile([self.page()],{'pages':[{'pageNumber':False}]},metadata)

    def test_api_assist_emits_structuring_event_only_when_requested(self):
        import json, os
        from pathlib import Path
        from unittest.mock import patch
        from fastapi.testclient import TestClient
        from api import create_app
        class NativeEngine:
            def extract(self, image):
                raise AssertionError('텍스트 PDF 테스트')
        content=(Path(__file__).resolve().parents[1]/'src/main/resources/fixtures/digital/application.pdf').read_bytes()
        with patch.dict(os.environ, {'OCR_API_TOKEN':'t'*32,'AI_PROVIDER':'disabled'}):
            with TestClient(create_app(NativeEngine)) as client:
                headers={'Authorization':'Bearer '+'t'*32}
                with patch.object(DocumentAi, 'analyze', return_value={'status':'SUCCEEDED','verifiedCount':1}) as analyze:
                    plain=client.post('/v1/analyze-document',headers=headers,content=content)
                    analyze.assert_not_called()
                    assisted=client.post('/v1/analyze-document?assist=true',headers=headers,content=content)
                    analyze.assert_called_once()
                events=[json.loads(line) for line in assisted.text.splitlines()]
                self.assertTrue(any(e['event']=='ai' for e in events))
                self.assertEqual('complete',events[-1]['event'])
                self.assertEqual(6,len([e for e in events if e['event']=='page']))

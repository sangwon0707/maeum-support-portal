"""신청 문서의 페이지·텍스트·사진을 분리한다. 신청 정답 데이터는 읽지 않는다."""
from __future__ import annotations
import base64
import io
import re
import cv2
import numpy as np
from pdf_document import PdfDocument
from PIL import Image

MAX_PAGES = 12
KINDS = {'AUTO', 'APPLICATION', 'BUSINESS_PLAN', 'BUSINESS_CERT', 'ESTIMATE', 'PHOTO_SHEET', 'CONSENT', 'OTHER'}
LABELS = {
    'businessName': ['상호', '신청인', '사업장명', '업체명', '사업체명', '신청자', '신청인 / 상호'],
    'businessNo': ['사업자등록번호', '등록번호', '사업자번호', '등록번호 / 개업', '번호·개업'],
    'representative': ['대표자', '성명 (대표자)', '대표자명', '성명'],
    'openedOn': ['개업일', '개업연월일', '등록번호 / 개업', '번호·개업'],
    'phone': ['연락처', '전화번호', '주소·연락처'],
    'address': ['사업장', '소재지', '사업장소재지', '사업장 주소', '사업장 소재지', '주소·연락처', '주소·업종'],
    'businessType': ['업태', '업태·종목'],
    'businessItem': ['종목'],
    'requestedAmount': ['지원 신청액', '지원신청액', '지원 신청금액', '신청금액', '지원액'],
    'selfContribution': ['자부담', '자부담액'],
    'totalAmount': ['총 사업비', '총사업비', '합계', '합계금액', '총액', '총 견적금액'],
}

def compact(value):
    return re.sub(r'\s+', '', value).strip(' :：')

ALL_LABELS = {compact(label) for labels in LABELS.values() for label in labels}

def classify(lines, height, declared):
    if declared != 'AUTO':
        return declared
    heading = ''.join(line['text'] for line in lines if line['bbox'][1] < height * .28)
    heading = compact(heading)
    if '지원사업신청서' in heading or '지원신청서' in heading:
        return 'APPLICATION'
    if '신청서류확인표' in heading:
        return 'OTHER'
    for kind, words in [
        ('BUSINESS_CERT', ['사업자등록증']), ('ESTIMATE', ['견적서']),
        ('CONSENT', ['개인정보', '동의서']), ('PHOTO_SHEET', ['현황사진', '현장사진', '사진대지', '도입예정위치', '현장확인', '현황·', '사진과계획', '업무공간현황', '현황·작업', '설비현황', '촬영환경참고자료']),
        ('APPLICATION', ['지원사업신청서', '지원신청서', '사업신청서']),
        ('BUSINESS_PLAN', ['계획', '전환계획', '시공', '개선내용', '산출', '추진일정', '교체비용', '과업서'])]:
        if any(word in heading for word in words):
            return kind
    return 'OTHER'


def field_value(lines, labels):
    for line in lines:
        key = compact(line['text'])
        label = next((word for word in labels if compact(word) == key), None)
        if label is None:
            continue
        x0, y0, x1, y1 = line['bbox']
        height = max(y1-y0, 8)
        options = []
        for other in lines:
            if other is line or compact(other['text']) in ALL_LABELS:
                continue
            a,b,c,d = other['bbox']
            if a >= x1-2 and abs((b+d-y0-y1)/2) < height * .8:
                options.append((a-x1, other))
        if not options:
            for other in lines:
                if other is line or compact(other['text']) in ALL_LABELS:
                    continue
                a,b,c,d = other['bbox']
                if 0 <= b-y1 < height*3 and abs(a-x0) < max(90, x1-x0):
                    options.append(((b-y1)*3+abs(a-x0), other))
        if options:
            return min(options,key=lambda pair:pair[0])[1]
    # 이미지 OCR은 같은 영역에 라벨과 값을 함께 반환할 수 있다.
    for line in lines:
        for label in labels:
            match = re.match(r'^'+re.escape(label)+r'(?:\s*[:：]\s*|\s+)(.+)$', line['text'])
            if match:
                return {**line, 'text':match.group(1)}
    return None


def extract_fields(lines, kind):
    if kind not in {'APPLICATION','BUSINESS_CERT','BUSINESS_PLAN','ESTIMATE'}:
        return {}, {}
    values, evidence = {}, {}
    for field, labels in LABELS.items():
        found = field_value(lines, labels)
        if not found:
            continue
        value = found['text'].strip()
        if field == 'businessNo':
            value = re.sub(r'\D', '', value.split('/')[0])
            if len(value) != 10: continue
        elif field == 'openedOn':
            parts = re.findall(r'\d+', value.split('/')[-1])
            if len(parts) < 3: continue
            try: value = f'{int(parts[0]):04d}-{int(parts[1]):02d}-{int(parts[2]):02d}'
            except ValueError: continue
        elif field == 'phone' and '/' in value:
            value = value.split('/')[-1].strip()
        elif field == 'address' and '/' in value:
            value = value.split('/')[0].strip()
        elif field in {'requestedAmount','selfContribution','totalAmount'}:
            value = re.sub(r'[^\d]', '', value)
            if not value or len(value)>10: continue
        elif field == 'businessType' and '/' in value:
            business_type, business_item = value.split('/',1)
            values['businessItem'] = business_item.strip()
            value = business_type.strip()
        if value and len(value) <= 200:
            values[field] = value
            evidence[field] = {'bbox':found['bbox'],'source':found['source'],'confidence':found.get('confidence')}
    if kind == 'BUSINESS_PLAN':
        paragraphs = [line['text'] for line in lines if len(line['text']) > 28
                      and not any(word in line['text'] for word in ['가상 자료', '공식 서식', '실제 지원 자격', '가상 계획', '뜻이 아닙니다'])]
        if paragraphs:
            values['planText'] = '\n'.join(paragraphs)[:2000]
    return values, evidence


def photo_like(image):
    if min(image.shape[:2]) < 64: return False
    small=cv2.resize(image,(120,120))
    gray=cv2.cvtColor(small,cv2.COLOR_BGR2GRAY)
    saturation=cv2.cvtColor(small,cv2.COLOR_BGR2HSV)[:,:,1]
    return float(np.mean(gray<220)) > .28 and (float(np.mean(saturation>35))>.06 or float(np.mean(gray<200))>.65)


def detect_photo_boxes(image):
    height,width=image.shape[:2]
    gray=cv2.cvtColor(image,cv2.COLOR_BGR2GRAY)
    edges=cv2.Canny(cv2.GaussianBlur(gray,(3,3),0),40,140)
    edges=cv2.dilate(edges,np.ones((3,3),np.uint8),iterations=2)
    contours,_=cv2.findContours(edges,cv2.RETR_TREE,cv2.CHAIN_APPROX_SIMPLE)
    candidates=[]
    for contour in contours:
        x,y,w,h=cv2.boundingRect(contour)
        ratio=w*h/(width*height)
        if not .025 < ratio < .72 or not .45 < w/h < 3: continue
        if cv2.contourArea(contour)/(w*h)<.8: continue
        box=[x+5,y+5,x+w-5,y+h-5]
        if not photo_like(image[box[1]:box[3],box[0]:box[2]]): continue
        if any(max(0,min(box[2],b[2])-max(box[0],b[0]))*max(0,min(box[3],b[3])-max(box[1],b[1])) > .7*min((box[2]-box[0])*(box[3]-box[1]),(b[2]-b[0])*(b[3]-b[1])) for b in candidates): continue
        candidates.append(box)
    return sorted(candidates,key=lambda box:(box[1],box[0]))[:8]


def png_base64(image):
    return base64.b64encode(cv2.imencode('.png',image)[1]).decode()


class DocumentAnalyzer:
    def __init__(self, engine): self.engine=engine

    def open(self, content):
        return PdfDocument(content)

    def events(self, content, declared='AUTO'):
        if declared not in KINDS: raise ValueError('문서 종류를 확인해 주세요.')
        with self.open(content) as document:
            count=len(document)
            yield {'event':'metadata','pageCount':count}
            for page in document.pages():
                index=page['pageNumber']-1
                image=cv2.imdecode(np.frombuffer(base64.b64decode(page['preview']),dtype=np.uint8),cv2.IMREAD_COLOR)
                if image is None: raise ValueError('페이지 이미지를 읽을 수 없습니다.')
                height,width=image.shape[:2]
                yield {'event':'stage','stage':'READING','pageNumber':index+1,'pageCount':count}
                lines=page['lines']
                if sum(len(line['text']) for line in lines)<30:
                    recognized=self.engine.extract(image)
                    lines=[{'text':line['text'],'bbox':[min(p[0] for p in line['polygon']),min(p[1] for p in line['polygon']),max(p[0] for p in line['polygon']),max(p[1] for p in line['polygon'])], 'source':'PADDLE_OCR','confidence':line.get('confidence')} for line in recognized['texts'] if len(line.get('polygon',[]))>=3]
                if len(lines)>1500: raise ValueError('페이지의 텍스트가 너무 많습니다.')
                kind=classify(lines,height,declared)
                fields,evidence=extract_fields(lines,kind)
                yield {'event':'stage','stage':'IMAGES','pageNumber':index+1,'pageCount':count}
                photos=[]
                for info in page['images']:
                    x0,y0,x1,y1=[round(v) for v in info['bbox']]
                    x0,y0=max(0,x0),max(0,y0);x1,y1=min(width,x1),min(height,y1)
                    ratio=max(0,x1-x0)*max(0,y1-y0)/(width*height)
                    if not .025 < ratio < .8: continue
                    cropped=image[y0:y1,x0:x1]
                    if not photo_like(cropped): continue
                    photos.append({'bbox':[x0,y0,x1,y1],'method':'PDF_IMAGE_REGION','image':png_base64(cropped)})
                if not content.startswith(b'%PDF-') and declared == 'PHOTO_SHEET' and photo_like(image):
                    photos = [{'bbox':[0,0,width,height], 'method':'UPLOADED_PHOTO', 'image':png_base64(image)}]
                if not photos and kind in {'PHOTO_SHEET','OTHER','BUSINESS_PLAN'}:
                    for box in detect_photo_boxes(image):
                        photos.append({'bbox':box,'method':'OPENCV_CANDIDATE','image':png_base64(image[box[1]:box[3],box[0]:box[2]])})
                table_rows=[]
                if kind in {'ESTIMATE','BUSINESS_PLAN'}:
                    for line in sorted(lines,key=lambda item:(item['bbox'][1],item['bbox'][0])):
                        row=next((row for row in table_rows if abs(row['y']-line['bbox'][1])<6),None)
                        if row is None:
                            row={'y':line['bbox'][1], 'cells':[]}; table_rows.append(row)
                        row['cells'].append({'text':line['text'],'bbox':line['bbox']})
                    table_rows=[row for row in table_rows if len(row['cells'])>=2]
                yield {'event':'page','pageNumber':index+1,'pageCount':count,'kind':kind,
                       'width':width,'height':height,'textSource':'PDF_TEXT' if any(line['source']=='PDF_TEXT' for line in lines) else 'PADDLE_OCR',
                       'tableRows':table_rows,'lines':lines,'fields':fields,'evidence':evidence,'photos':photos[:8],'preview':png_base64(image)}
            yield {'event':'complete','pageCount':count}

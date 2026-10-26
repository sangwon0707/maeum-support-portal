#!/usr/bin/env python3
"""실제 브라우저에서 촬영한 화면과 단계 기록으로 업무 흐름 페이지를 만든다."""
import html
import json
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'output/process-flow'
DESTINATION = ROOT / 'output/visual-direction/process-flow'


def main():
    evidence = json.loads((SOURCE / 'steps.json').read_text())
    DESTINATION.mkdir(parents=True, exist_ok=True)
    navigation, slides = [], []
    for index, step in enumerate(evidence['steps']):
        filename = step['name'] + '.jpg'
        image = SOURCE / filename
        if not image.exists():
            raise FileNotFoundError(image)
        shutil.copy2(image, DESTINATION / filename)
        title, role, note = (html.escape(step[key]) for key in ['title', 'role', 'note'])
        navigation.append(f'<button class="step-link" data-step="{index}"><b>{index+1:02}</b><span><small>{role}</small>{title}</span></button>')
        loading = '' if index == 0 else 'loading="lazy"'
        slides.append(f'<article class="slide" id="step-{index+1}" {"" if index==0 else "hidden"}><div class="slide-title"><span class="role">{role}</span><h2>{index+1:02}. {title}</h2><a href="{filename}" target="_blank">원본 크게 보기 ↗</a></div><p>{note}</p><a class="screen" href="{filename}" target="_blank"><img src="{filename}" alt="{role} 실제 화면: {title}" {loading}></a></article>')
    serialized = json.dumps({'lastStage': evidence['lastStage'], 'receipt': evidence['receipt'], 'steps':len(evidence['steps'])}, ensure_ascii=False)
    page = '''<!doctype html><html lang="ko"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>마음이음 실제 신청·검토 흐름</title><style>
    *{box-sizing:border-box}body{margin:0;font-family:system-ui,sans-serif;color:#23364e;background:#f2f5f9;line-height:1.6}button,a{font:inherit}header{padding:30px max(24px,calc((100vw - 1420px)/2));background:#fff;border-bottom:1px solid #dce4ee}h1{margin:0;font-size:28px}header p{margin:8px 0;color:#63778c;font-size:14px}.route{display:flex;align-items:center;gap:10px;flex-wrap:wrap;font-size:12px;margin-top:16px}.route span{padding:7px 12px;background:#edf3fb;border-radius:4px}.route i{font-style:normal;color:#91a1b4}.evidence{padding:12px 16px;background:#eaf1fb;border-radius:6px;max-width:1420px;margin:20px auto;font-size:13px}.layout{max-width:1420px;margin:0 auto 50px;display:grid;grid-template-columns:260px minmax(0,1fr);gap:24px;padding:0 20px}aside{position:sticky;top:16px;align-self:start;max-height:calc(100vh - 32px);overflow:auto;padding:10px;background:#fff;border:1px solid #e0e6ee;border-radius:10px}.step-link{width:100%;display:flex;gap:12px;text-align:left;padding:11px 10px;border:0;background:#fff;border-radius:5px;color:#51657b;cursor:pointer;font-size:12px}.step-link b{font-size:13px;color:#7690aa;padding-top:15px}.step-link small{display:block;font-size:10px;color:#8093a6}.step-link[aria-current="step"]{background:#e9f1ff;color:#154e9a;font-weight:700}.content{min-width:0}.controls{display:flex;justify-content:space-between;align-items:center;margin-bottom:14px;gap:15px}.controls button{background:#fff;border:1px solid #cbd8e8;border-radius:6px;padding:9px 16px;color:#31567d;cursor:pointer;font-size:13px}.controls button:disabled{opacity:.35;cursor:default}.controls span{font-size:13px;color:#687f96}.slide{background:#fff;padding:24px;border:1px solid #dce4ef;border-radius:10px}.slide[hidden]{display:none}.slide-title{display:flex;align-items:center;gap:12px;flex-wrap:wrap}.slide h2{font-size:20px;margin:0;flex:1}.role{font-size:11px;color:#20549b;background:#ecf3ff;border-radius:4px;padding:4px 8px}.slide-title>a{font-size:11px;color:#607994}.slide>p{font-size:13px;color:#6b7f94;margin:15px 0 20px}.screen{display:block;border:1px solid #e0e5ec;background:#f9fafc}.screen img{display:block;width:100%;height:auto}button:focus-visible,a:focus-visible{outline:3px solid #4d86e2;outline-offset:3px}@media(max-width:800px){header{padding:24px}h1{font-size:23px}.evidence{margin:16px 20px}.layout{grid-template-columns:1fr}aside{position:static;display:flex;overflow:auto;max-height:none;gap:6px}.step-link{flex:0 0 180px}.slide{padding:15px}.slide h2{font-size:17px}.route{gap:6px}.route span{padding:5px 8px}}</style><header><h1>신청부터 보완·검토까지, 실제 화면으로 따라가기</h1><p>로컬 Java 서버에서 직접 로그인·작성·업로드·검토한 가상 신청 한 건의 기록입니다.</p><div class="route"><span>로그인</span><i>→</i><span>신청서 작성</span><i>→</i><span>서류 업로드</span><i>→</i><span>실제 OCR·접수</span><i>→</i><span>담당자 검토</span><i>→</i><span>보완·재제출</span><i>→</i><span>결과 확인</span></div></header>'''
    page += f'<div class="evidence">접수번호 <strong>{html.escape(evidence["receipt"])}</strong> · {len(evidence["steps"])}개 실제 촬영 화면 · {html.escape(evidence["ocr"])}<br>마지막 단계: {html.escape(evidence["lastStage"])} · 실제 지원금 지급·운영 배포 기록이 아닙니다.</div>'
    page += '<main class="layout"><aside aria-label="촬영 단계">'+''.join(navigation)+'</aside><div class="content"><nav class="controls" aria-label="단계 이동"><button id="previous">← 이전 화면</button><span id="position"></span><button id="next">다음 화면 →</button></nav>'+''.join(slides)+'</div></main>'
    page += '''<script>const slides=[...document.querySelectorAll('.slide')],links=[...document.querySelectorAll('.step-link')];let active=0;function show(index){active=Math.max(0,Math.min(slides.length-1,index));slides.forEach((slide,i)=>slide.hidden=i!==active);links.forEach((link,i)=>{if(i===active)link.setAttribute('aria-current','step');else link.removeAttribute('aria-current')});document.querySelector('#position').textContent=`${active+1} / ${slides.length}`;document.querySelector('#previous').disabled=active===0;document.querySelector('#next').disabled=active===slides.length-1;history.replaceState(null,'',`#step-${active+1}`)}links.forEach((link,i)=>link.addEventListener('click',()=>show(i)));document.querySelector('#previous').addEventListener('click',()=>show(active-1));document.querySelector('#next').addEventListener('click',()=>show(active+1));const initial=Number(location.hash.replace('#step-',''));show(Number.isFinite(initial)&&initial>0?initial-1:0);</script></html>'''
    (DESTINATION / 'index.html').write_text(page)
    (DESTINATION / 'summary.json').write_text(serialized)
    print(DESTINATION / 'index.html')


if __name__ == '__main__':
    main()

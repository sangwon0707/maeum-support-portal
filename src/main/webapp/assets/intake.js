/* 업로드는 원본 저장만 요청한다. OCR 시작은 다음 화면의 별도 POST다. */
(() => {
  'use strict';
  const upload=document.querySelector('[data-intake-upload]');
  if(!upload)return;
  const dialog=document.querySelector('[data-intake-samples]');
  const fileInput=upload.querySelector('[name="files"]');
  const button=upload.querySelector('[data-intake-submit]');
  const feedback=upload.querySelector('[data-sample-feedback]');
  const name=upload.querySelector('[data-intake-selection]');
  const preview=upload.querySelector('[data-intake-selection-preview]');
  const empty=upload.querySelector('[data-intake-preview-empty]');
  const state={sample:null,key:crypto.randomUUID(),busy:false};
  const paint=()=>{
    button.disabled=state.busy||(!state.sample&&!fileInput.files.length);
    name.textContent=state.sample?.dataset.filename||fileInput.files[0]?.name||'선택한 파일이 없습니다.';
    upload.querySelectorAll('[data-intake-sample-url]').forEach(file=>file.setAttribute('aria-pressed',String(file===state.sample)));
    preview.hidden=!state.sample;empty.hidden=Boolean(state.sample);
    if(state.sample)preview.src=state.sample.dataset.preview;
  };
  const setBusy=busy=>{
    state.busy=busy;fileInput.disabled=busy;
    upload.querySelectorAll('button:not([data-intake-submit])').forEach(control=>control.disabled=busy);
    button.textContent=busy?'업로드 중…':'업로드';paint();
  };
  const clear=()=>{state.sample=null;state.key=crypto.randomUUID();fileInput.value='';fileInput.required=true;feedback.textContent='';paint();};
  fileInput.addEventListener('change',()=>{
    state.sample=null;state.key=crypto.randomUUID();fileInput.setCustomValidity('');
    if(fileInput.files[0]?.size>10*1024*1024)fileInput.setCustomValidity('10MiB 이하의 PDF를 선택해 주세요.');
    feedback.textContent=fileInput.validationMessage||'다음 화면에서 원본을 확인할 수 있습니다.';paint();
  });
  upload.querySelectorAll('[data-intake-sample-url]').forEach(file=>file.addEventListener('click',()=>{
    if(state.busy)return;
    state.sample=file;state.key=crypto.randomUUID();fileInput.value='';fileInput.required=false;fileInput.setCustomValidity('');
    feedback.textContent='파일을 선택했습니다. 업로드하면 다음 화면에서 원본을 확인합니다.';paint();
  }));
  upload.addEventListener('submit',async event=>{
    event.preventDefault();if(state.busy)return;
    if(!state.sample&&!fileInput.reportValidity())return;
    if(!state.sample&&!fileInput.files.length)return;
    const body=new FormData(upload);const selected=state.sample;let file=fileInput.files[0];
    body.set('requestKey',state.key);body.set('layout','BUNDLE');body.delete('files');body.delete('kinds');body.append('kinds','AUTO');
    setBusy(true);feedback.textContent='파일을 업로드하고 있습니다. 다음 화면에서 서류를 확인해 주세요.';
    try {
      if(selected){
        const source=await fetch(selected.dataset.intakeSampleUrl,{credentials:'same-origin',signal:AbortSignal.timeout(15000)});
        if(!source.ok)throw new Error('선택한 파일을 가져오지 못했습니다. 다시 시도해 주세요.');
        file=new File([await source.blob()],selected.dataset.filename,{type:'application/pdf'});
      }
      body.append('files',file);
      const response=await fetch(upload.action,{method:'POST',body,credentials:'same-origin',signal:AbortSignal.timeout(30000)});
      if(new URL(response.url).pathname.endsWith('/login'))throw new Error('로그인이 만료됐습니다. 다시 로그인해 주세요.');
      if(!response.ok)throw new Error('업로드하지 못했습니다. 파일 형식·용량과 신청 상태를 확인해 주세요.');
      location.assign(response.url);
    } catch(error) {
      feedback.textContent=error.name==='TimeoutError'?'응답이 늦습니다. 다시 업로드해도 같은 요청의 파일은 중복 저장하지 않습니다.':error.message;
    } finally {setBusy(false);}
  });
  document.querySelector('[data-program-upload-open]')?.addEventListener('click',()=>dialog.showModal());
  upload.querySelectorAll('[data-intake-sample-close]').forEach(close=>close.addEventListener('click',()=>dialog.close()));
  upload.querySelectorAll('[data-entry-mode]').forEach(mode=>mode.addEventListener('click',clear));
  dialog.addEventListener('cancel',event=>{if(state.busy)event.preventDefault();});
  addEventListener('pageshow',event=>{if(event.persisted)setBusy(false);});
  paint();
})();

/* 실제 인식은 엔진 이벤트, 준비된 결과는 서버의 명시적인 재생 상태를 표시한다. */
(() => {
  'use strict';
  const progress=document.querySelector('[data-intake-progress]');
  if(progress) {
    const prepared=document.querySelector('[data-intake-mode="PREPARED"]');
    const names=prepared?{LOADING:'저장된 문서 구성을 불러오고 있습니다.',READING:'페이지별 인식 결과를 불러오고 있습니다.',IMAGES:'문서·사진 자료를 불러오고 있습니다.',ASSEMBLING:'검토 화면을 구성하고 있습니다.',COMPLETE:'검토할 자료를 정리했습니다.'}:{AI_REVIEW:'AI가 문서 내용을 해석하고 원문과 대조하고 있습니다.',UPLOADED:'파일 접수를 완료했습니다.',READING:'페이지의 내용을 읽고 있습니다.',IMAGES:'문서에서 사진을 분리하고 있습니다.',ASSEMBLING:'검토할 문서를 구성하고 있습니다.',COMPLETE:'검토할 자료를 정리했습니다.'};
    const paint=(state,stage,percent,detail)=>{
      progress.dataset.status=state;
      progress.setAttribute('aria-busy',String(['QUEUED','PROCESSING'].includes(state)));
      progress.querySelector('[data-intake-stage]').textContent=state==='FAILED'?'문서를 처리하지 못했습니다.':names[stage]||'문서를 처리하고 있습니다.';
      progress.querySelector('[data-intake-percent]').textContent=`${percent}%`;
      if(detail)progress.querySelector('[data-intake-detail]').textContent=detail;
      const track=progress.querySelector('[role="progressbar"]');track.setAttribute('aria-valuenow',percent);track.firstElementChild.style.width=`${percent}%`;
      const index=percent===100?4:stage==='ASSEMBLING'?3:stage==='IMAGES'?2:1;
      progress.querySelectorAll('.intake-stages li').forEach((li,i)=>{li.classList.toggle('is-complete',i<index);li.classList.toggle('is-current',i===index);});
    };
    const poll=async()=>{
      try {
        const response=await fetch(progress.dataset.url,{credentials:'same-origin',cache:'no-store',signal:AbortSignal.timeout(10000)});
        if(!response.ok||!response.headers.get('content-type')?.includes('application/json'))throw new Error('status unavailable');
        const data=await response.json();paint(data.status,data.stage,data.progress,data.detail);
        if(['READY','APPLIED','FAILED'].includes(data.status)){location.reload();return;}
      } catch(_) {progress.querySelector('[data-intake-detail]').textContent='진행 상태를 확인하지 못했습니다. 연결을 다시 확인하고 있습니다.';}
      setTimeout(poll,1800);
    };
    paint(progress.dataset.status,progress.dataset.stage,Number(progress.querySelector('[role="progressbar"]').getAttribute('aria-valuenow')));
    if(['QUEUED','PROCESSING'].includes(progress.dataset.status))setTimeout(poll,900);
  }
  const preview=document.querySelector('[data-intake-preview]');
  document.querySelectorAll('[data-intake-page]').forEach(button=>button.addEventListener('click',()=>{
    preview.src=button.dataset.preview;const sourceBadge=document.querySelector('[data-intake-text-source]');if(sourceBadge)sourceBadge.textContent=document.querySelector('[data-intake-mode="PREPARED"]')?'OCR 검토 데모':button.dataset.textSource==='PDF_TEXT'?'PDF 원문':'실제 OCR';document.querySelector('[data-intake-preview-label]').textContent=`${button.dataset.sourceName} · ${button.dataset.pageNumber}쪽`;
    document.querySelectorAll('[data-intake-page]').forEach(item=>item.setAttribute('aria-pressed',String(item===button)));
  }));
  document.querySelectorAll('[data-evidence-source]').forEach(button=>button.addEventListener('click',()=>{
    const page=[...document.querySelectorAll('[data-intake-page]')].find(item=>item.dataset.sourceId===button.dataset.evidenceSource&&item.dataset.pageNumber===button.dataset.evidencePage);
    if(page){page.click();preview.scrollIntoView({block:'center',behavior:matchMedia('(prefers-reduced-motion: reduce)').matches?'instant':'smooth'});}
  }));
})();

(() => {
  const tabs=[...document.querySelectorAll('[data-intake-tab]')];
  const select=tab=>{
    tabs.forEach(item=>{const active=item===tab;item.setAttribute('aria-selected',String(active));item.tabIndex=active?0:-1;});
    document.querySelectorAll('[data-intake-panel]').forEach(panel=>panel.hidden=panel.dataset.intakePanel!==tab.dataset.intakeTab);
  };
  tabs.forEach((tab,index)=>{
    tab.addEventListener('click',()=>select(tab));
    tab.addEventListener('keydown',event=>{if(!['ArrowLeft','ArrowRight','Home','End'].includes(event.key))return;event.preventDefault();const target=event.key==='Home'?tabs[0]:event.key==='End'?tabs.at(-1):tabs[(index+(event.key==='ArrowRight'?1:tabs.length-1))%tabs.length];select(target);target.focus();});
  });
  document.querySelector('[data-intake-review]')?.addEventListener('invalid',event=>{
    const panel=event.target.closest('[data-intake-panel]');if(panel)select(tabs.find(tab=>tab.dataset.intakeTab===panel.dataset.intakePanel));
  },true);
})();
(() => {
  const button=document.querySelector('[data-intake-zoom]');
  button?.addEventListener('click',()=>{const frame=document.querySelector('.intake-preview-frame');const zoomed=frame.classList.toggle('is-zoomed');button.setAttribute('aria-pressed',String(zoomed));button.textContent=zoomed?'쪽 맞춤':'확대';});
})();

(() => {
  const edit=document.querySelector('[data-intake-edit]');
  if(!edit)return;
  const form=document.querySelector('[data-intake-review]');
  const fields=[...form.querySelectorAll('.intake-field-row input, #intake-plan')];
  const cancel=form.querySelector('[data-intake-edit-cancel]');
  const save=form.querySelector('[data-intake-save]');
  const initial=new Map(fields.map(field=>[field,field.value]));
  const setEditing=editing=>{
    fields.forEach(field=>{field.readOnly=!editing;field.required=editing;});
    edit.hidden=editing;cancel.hidden=!editing;save.hidden=!editing;
    document.querySelector('[data-intake-edit-hint]').textContent=editing?'수정 후 저장해 주세요. 취소하면 이전 값으로 돌아갑니다.':'신청 내용 수정 버튼으로 편집';
    form.classList.toggle('is-editing',editing);
  };
  edit.addEventListener('click',()=>{
    document.querySelector('[data-intake-tab="content"]').click();setEditing(true);fields[0]?.focus();
  });
  cancel.addEventListener('click',()=>{fields.forEach(field=>field.value=initial.get(field));setEditing(false);edit.focus();});
  if(location.hash==='#edit')edit.click();
})();

(() => {
  const dialog=document.querySelector('[data-program-upload-dialog]');
  if(!dialog)return;
  const opener=document.querySelector('[data-program-upload-open]');

  dialog.addEventListener('close',()=>opener?.focus());
  dialog.querySelectorAll('[data-entry-mode]').forEach(tab=>tab.addEventListener('click',()=>{
    dialog.querySelectorAll('[data-entry-mode]').forEach(item=>item.setAttribute('aria-pressed',String(item===tab)));
    dialog.querySelectorAll('[data-entry-panel]').forEach(panel=>panel.hidden=panel.dataset.entryPanel!==tab.dataset.entryMode);
  }));
})();

(() => {
  'use strict';
  const choices=document.querySelectorAll('[data-review-mode]');
  if(!choices.length)return;
  const update=()=>{
    const live=[...choices].some(input=>input.checked&&input.value==='LIVE');
    document.querySelector('[data-live-review-warning]').hidden=!live;
    const checkbox=document.querySelector('[data-slow-acknowledgment]');
    checkbox.required=live;
    if(!live)checkbox.checked=false;
    document.querySelector('[data-prepared-review-help]').hidden=live;
  };
  choices.forEach(input=>input.addEventListener('change',update));
  update();
})();

/* 포트폴리오 안내는 화면 보조다. 인증·파일 검증·업무 상태는 기존 Java 경로가 처리한다. */
(() => {
  'use strict';

  const notice = document.querySelector('.site-notice');
  if (notice) {
    // 고지 줄의 실제 높이를 확보해 모바일 줄바꿈과 본문 겹침을 함께 처리한다.
    const reserveNoticeSpace = () => document.documentElement.style.setProperty('--site-notice-height', `${notice.getBoundingClientRect().height}px`);
    reserveNoticeSpace();
    new ResizeObserver(reserveNoticeSpace).observe(notice);
  }
  const picker = document.getElementById('sample-picker');
  const pickerState = { kind: null, selected: null, opener: null, busy: false, previewReady: false, attachAfterPreview: false };
  const halo = document.querySelector('.portfolio-guide-halo');
  const cue = document.querySelector('.portfolio-guide-cue');
  const widget = document.querySelector('.portfolio-guide-widget');
  const guideStatus = document.querySelector('[data-tour-status]');
  const locateButton = document.querySelector('[data-tour-locate]');
  const completedLinks = new WeakSet();
  const preferenceKey = 'maeum.portfolio-guide';
  let enabled = true;
  let activeTarget = null;
  let framePending = false;

  try {
    enabled = sessionStorage.getItem(preferenceKey) !== 'off';
  } catch (_) {
    // 저장소가 차단된 브라우저에서도 현재 화면의 안내는 사용할 수 있다.
  }

  const text = (selector, value) => {
    const element = document.querySelector(selector);
    if (element && element.textContent !== value) element.textContent = value;
  };
  const visible = (element) => element && element.getClientRects().length > 0 && !element.disabled && !element.closest('details:not([open])') && (typeof element.checkVisibility !== 'function' || element.checkVisibility({checkVisibilityCSS:true}));
  const hint = (element, message) => element && visible(element) ? { element, message } : null;
  const firstVisible = (selector) => [...document.querySelectorAll(selector)].find(visible);

  function selectFolder(kind) {
    if (pickerState.busy) return;
    pickerState.kind = kind;
    pickerState.selected = null;
    pickerState.attachAfterPreview = false;
    pickerState.previewReady = false;
    picker.querySelectorAll('[data-picker-folder]').forEach((button) => {
      button.setAttribute('aria-pressed', String(button.dataset.pickerFolder === kind));
    });
    picker.querySelectorAll('[data-picker-file]').forEach((button) => {
      button.hidden = button.dataset.kind !== kind;
      button.setAttribute('aria-pressed', 'false');
    });
    const preview = picker.querySelector('[data-picker-preview]');
    preview.hidden = true;
    preview.removeAttribute('src');
    picker.querySelector('[data-picker-empty]').hidden = false;
    picker.querySelector('[data-picker-attach]').disabled = true;
    text('[data-picker-folder-label]', kind === 'BUSINESS_CERT' ? '사업자등록증' : '견적서');
    text('[data-picker-count]', `파일 ${picker.querySelectorAll('[data-picker-file]:not([hidden])').length}개`);
    text('[data-picker-selected]', '선택한 파일 없음');
    text('[data-picker-preview-caption]', '');
    text('[data-picker-feedback]', '');
    scheduleGuide();
  }

  function selectFile(button) {
    if (pickerState.busy) return;
    if (pickerState.selected === button) return;
    pickerState.attachAfterPreview = false;
    pickerState.selected = button;
    pickerState.previewReady = false;
    picker.querySelectorAll('[data-picker-file]').forEach((file) => {
      file.setAttribute('aria-pressed', String(file === button));
    });
    const preview = picker.querySelector('[data-picker-preview]');
    picker.querySelector('[data-picker-attach]').disabled = true;
    picker.querySelector('[data-picker-empty]').hidden = true;
    preview.hidden = false;
    preview.onload = () => {
      pickerState.previewReady = true;
      picker.querySelector('[data-picker-attach]').disabled = false;
      if (pickerState.attachAfterPreview) attachSelectedFile();
      scheduleGuide();
    };
    preview.onerror = () => {
      pickerState.previewReady = false;
      text('[data-picker-feedback]', '미리보기를 불러오지 못했어요. 창을 다시 열어 주세요.');
      scheduleGuide();
    };
    preview.src = button.dataset.url;
    text('[data-picker-selected]', button.dataset.filename);
    text('[data-picker-preview-caption]', button.querySelector('small').textContent);
    text('[data-picker-feedback]', '문서를 확인한 뒤 선택해서 첨부해 주세요.');
    scheduleGuide();
  }

  async function attachSelectedFile() {
    if (pickerState.busy || !pickerState.selected || !pickerState.previewReady) return;
    const selected = pickerState.selected;
    const form = document.querySelector(`[data-document-card="${pickerState.kind}"] form.upload-form`);
    if (!form) return;
    pickerState.busy = true;
    picker.querySelectorAll('button').forEach((button) => button.disabled = true);
    text('[data-picker-feedback]', '선택한 예시 파일을 첨부하고 있어요…');
    text('[data-picker-attach]', '첨부 중…');
    scheduleGuide();
    try {
      const download = await fetch(selected.dataset.url, { credentials: 'same-origin' });
      if (!download.ok || !download.headers.get('content-type')?.startsWith('image/png')) {
        throw new Error('예시 파일을 가져오지 못했어요. 잠시 후 다시 선택해 주세요.');
      }
      // 파일 선택의 편의만 제공한다. 기존 multipart 폼의 CSRF·서류 종류와 실제 이미지 바이트를 보낸다.
      const body = new FormData(form);
      body.set('file', new File([await download.blob()], selected.dataset.filename, { type: 'image/png' }));
      const response = await fetch(form.action, { method: 'POST', body, credentials: 'same-origin' });
      if (new URL(response.url).pathname.endsWith('/login')) {
        throw new Error('로그인이 만료됐어요. 다시 로그인한 뒤 첨부해 주세요.');
      }
      if (!response.ok) {
        throw new Error('첨부하지 못했어요. 요청받은 서류와 보완 기한을 확인해 주세요.');
      }
      location.assign(response.url);
    } catch (error) {
      pickerState.busy = false;
      picker.querySelectorAll('button').forEach((button) => button.disabled = false);
      // 보완 요청 밖의 폴더는 오류 후에도 다시 활성화하지 않는다.
      picker.querySelectorAll('[data-picker-folder]').forEach((button) => {
        button.disabled = !document.querySelector(`[data-sample-open="${button.dataset.pickerFolder}"]`);
      });
      text('[data-picker-attach]', '선택해서 첨부');
      text('[data-picker-feedback]', error instanceof TypeError ? '연결이 끊겼어요. 새로고침해서 첨부 결과를 먼저 확인해 주세요.' : error.message || '연결을 확인해 주세요. 처리 결과는 새로고침해서 확인할 수 있어요.');
      scheduleGuide();
    }
  }

  function closePicker() {
    if (pickerState.busy) return;
    picker.close();
    pickerState.opener?.focus();
    scheduleGuide();
  }

  if (picker) {
    document.querySelectorAll('[data-sample-open]').forEach((button) => {
      button.addEventListener('click', () => {
        pickerState.opener = button;
        selectFolder(button.dataset.sampleOpen);
        picker.showModal();
        picker.querySelector('[data-picker-file]:not([hidden])')?.focus();
        scheduleGuide();
      });
    });
    picker.querySelectorAll('[data-picker-folder]').forEach((button) => {
      button.addEventListener('click', () => selectFolder(button.dataset.pickerFolder));
    });
    picker.querySelectorAll('[data-picker-file]').forEach((button) => {
      button.addEventListener('click', () => selectFile(button));
      button.addEventListener('dblclick', () => {
        if (pickerState.previewReady) attachSelectedFile();
        else pickerState.attachAfterPreview = true;
      });
    });
    picker.querySelectorAll('[data-picker-close]').forEach((button) => button.addEventListener('click', closePicker));
    picker.querySelector('[data-picker-attach]').addEventListener('click', attachSelectedFile);
    picker.addEventListener('cancel', (event) => {
      if (pickerState.busy) event.preventDefault();
    });
    picker.addEventListener('close', scheduleGuide);
    picker.addEventListener('click', (event) => {
      const bounds = picker.getBoundingClientRect();
      if (event.target === picker && (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom)) closePicker();
    });
  }

  function nextGuideTarget() {
    if (picker?.open) {
      if (pickerState.busy) return null;
      return pickerState.selected
        ? hint(picker.querySelector('[data-picker-attach]'), '미리보기를 확인했다면 이 버튼으로 첨부하세요.')
        : hint(picker.querySelector('[data-picker-file]:not([hidden])'), '준비된 예시 파일을 눌러 선택하세요.');
    }
    const path = location.pathname;
    if (/\/applications\/\d+\/intake$/.test(path)) {
      const modal = document.querySelector('[data-intake-samples]');
      if (modal?.open) return hint(firstVisible('[data-intake-sample-url]'), '업로드할 신청 서류를 선택해 주세요.');
      const progress = document.querySelector('[data-intake-progress]');
      if (progress && ['QUEUED','PROCESSING'].includes(progress.dataset.status)) return null;
      const start = firstVisible('form[action$="/start"] button');
      if(start)return hint(start,'올린 서류를 확인하고 문서 검토를 요청해 주세요.');
      const edit = firstVisible('[data-intake-edit]');
      if (edit) return hint(edit, '제출 전에는 신청 내용을 수정하고 저장할 수 있어요.');
      const apply = firstVisible('[data-intake-review] button:not([type])');
      if (apply) return hint(apply, '원본·분류·신청 내용·사진을 검토한 뒤 반영하세요.');
      return hint(firstVisible('[data-intake-sample-open]'), '준비된 신청서 PDF를 선택하거나 직접 파일을 업로드하세요.');
    }
    if (path.endsWith('/login')) {
      const emptyField = [...document.querySelectorAll('#username,#password')].find((field) => !field.value);
      const fillTestAccount = document.querySelector('[data-fill-test-account]');
      if (emptyField && !document.getElementById('username').value && fillTestAccount) return hint(fillTestAccount, '테스트 계정을 입력한 뒤 로그인하세요.');
      return emptyField ? hint(emptyField, emptyField.id === 'username' ? '신청자 테스트 계정 아이디를 입력하세요.' : '비밀번호를 입력한 뒤 로그인하세요.')
        : hint(document.querySelector('form[action$="/login"] button[type="submit"]'), '이 버튼으로 로그인해 신청을 시작하세요.');
    }
    if (/\/applications\/\d+\/edit$/.test(path)) {
      const name = document.getElementById('businessName');
      const examples = document.querySelector('details.example-help');
      if (name && !name.value && examples) {
        return examples.open ? hint(examples.querySelector('button'), '이 사업의 가상 신청 정보를 불러올 수 있어요.')
          : hint(examples.querySelector('summary'), '작성할 자료가 없다면 여기를 열어 예시를 불러오세요.');
      }
      const emptyField = [...document.querySelectorAll('form[action$="/save"] input:not([type="hidden"]),form[action$="/save"] textarea')]
        .find((field) => !field.readOnly && !field.disabled && !field.value);
      if (emptyField) return hint(emptyField, '이 항목을 입력하세요. 가상 정보를 사용해 주세요.');
    }
    if (/\/applications\/\d+\/documents$/.test(path)) {
      const missing = [...document.querySelectorAll('[data-document-state]')].find((state) => state.dataset.attached !== 'true' && document.querySelector(`[data-sample-open="${state.dataset.documentState}"]`));
      if (missing) return hint(document.querySelector(`[data-sample-open="${missing.dataset.documentState}"]`), '파일이 없어도 여기서 가상 문서를 골라 첨부할 수 있어요.');
      // 보완 단계에서는 누락 여부가 아니라 서버가 허용한 교체 항목을 먼저 안내한다.
      const replacement = document.querySelector('[data-replacement-pending="true"]');
      if (replacement) return hint(document.querySelector(`[data-sample-open="${replacement.dataset.documentState}"]`), '요청받은 예시 서류를 다시 선택해 새 버전으로 첨부하세요.');
    }
    const ocr = document.querySelector('[data-portfolio-ocr]');
    if (ocr) {
      if (['PENDING', 'RUNNING'].includes(ocr.dataset.status)) return null;
      if (ocr.dataset.confirmed !== 'true') {
        if (!ocr.dataset.status) {
          const request = firstVisible('form[action$="/ocr"] button');
          if (request) return hint(request, '첨부한 사업자등록증의 문자 인식을 요청하세요.');
        }
        return hint(firstVisible('form[action$="/confirm"] button'), '원본과 네 항목을 대조한 뒤 확인값을 저장하세요.');
      }
    }
    const acknowledgment = document.querySelector('form[action$="/submit"] input[name="acknowledged"]');
    if (acknowledgment && !acknowledgment.checked) return hint(acknowledgment.closest('label'), '신청 내용과 서류를 확인한 뒤 체크하세요.');
    const response = document.querySelector('#responseText');
    if (response && !response.value) return hint(response, '보완한 내용을 적은 뒤 다시 제출하세요.');
    const decision = document.getElementById('decisionReason');
    if (decision) {
      if (decision.value) {
        const checked = document.querySelector('input[name="checked"]');
        return checked && !checked.checked ? hint(checked.closest('label'), '서류 검토를 마쳤다면 확인해 주세요.')
          : hint(firstVisible('form[action$="/decision"] button'), '선택한 결과와 사유를 확인하고 확정하세요.');
      }
      const reason = document.getElementById('supplementReason');
      return reason?.value ? hint(firstVisible('form[action$="/supplement"] button'), '보완 항목·사유·기한을 확인하고 요청을 보내세요.')
        : hint(reason, '보완이 필요하면 사유를 적으세요. 바로 결과를 확정할 수도 있어요.');
    }
    return [...document.querySelectorAll('[data-tour]')].filter(visible)
      .filter((element) => !completedLinks.has(element)).map((element) => ({ element, message: element.dataset.tour }))[0] || null;
  }

  function updateGuide() {
    framePending = false;
    if (!widget) return;
    document.querySelectorAll('[data-portfolio-guide-toggle]').forEach((button) => button.setAttribute('aria-pressed', String(enabled)));
    text('[data-tour-toggle-label]', enabled ? '안내 켜짐' : '안내 꺼짐');
    document.querySelectorAll('[data-guide-setting-label]').forEach(label => label.textContent=enabled?'켜짐':'꺼짐');
    const inlineToggle = document.querySelector('.picker-guide-toggle');
    if (inlineToggle) text('.picker-guide-toggle', enabled ? '안내 끄기' : '안내 켜기');
    widget.classList.toggle('guide-off', !enabled);
    activeTarget?.classList.remove('portfolio-guide-target');
    locateButton.hidden = true;
    cue.hidden = true;
    halo.hidden = true;
    if (!enabled) return;
    const next = nextGuideTarget();
    activeTarget = next?.element || null;
    text('[data-tour-status]', next?.message || (document.querySelector('[data-ocr-pending]') ? '문자 인식 중이에요. 잠시 기다려 주세요.' : '이 단계의 안내를 마쳤어요. 화면의 진행 상태를 확인하세요.'));
    if (!next) return;
    const box = activeTarget.getBoundingClientRect();
    if (box.bottom < 0 || box.top > innerHeight - 16) {
      locateButton.hidden = false;
      return;
    }
    activeTarget.classList.add('portfolio-guide-target');
    const host = picker?.open ? picker : document.body;
    if (cue.parentNode !== host) host.append(cue);
    if (halo.parentNode !== host) host.append(halo);
    if (activeTarget.matches('button,a,summary')) {
      halo.style.left = `${box.left - 5}px`;
      halo.style.top = `${box.top - 5}px`;
      halo.style.width = `${box.width + 10}px`;
      halo.style.height = `${box.height + 10}px`;
      halo.style.borderRadius = `${(parseFloat(getComputedStyle(activeTarget).borderTopLeftRadius) || 0) + 5}px`;
      halo.hidden = false;
    }
    // 화살표는 다음 조작 대상의 네 면 바로 옆에서만 위치를 고른다.
    const size = 32, gap = 6;
    const centerX = box.left + box.width / 2, centerY = box.top + box.height / 2;
    const positions = [
      {x: centerX - size / 2, y: box.top - size - gap, angle: 0},
      {x: box.left - size - gap, y: centerY - size / 2, angle: -90},
      {x: box.right + gap, y: centerY - size / 2, angle: 90},
      {x: centerX - size / 2, y: box.bottom + gap, angle: 180}
    ];
    const scope = picker?.open ? picker : document.querySelector('main');
    const occupied = [];
    const walker = document.createTreeWalker(scope, NodeFilter.SHOW_TEXT);
    while (walker.nextNode()) {
      const node = walker.currentNode;
      if (!node.textContent.trim() || cue.contains(node) || !node.parentElement.getClientRects().length) continue;
      const range = document.createRange();
      range.selectNodeContents(node);
      occupied.push(...range.getClientRects());
    }
    scope.querySelectorAll('input,textarea,button,a,[data-picker-preview]').forEach(element => {
      if (visible(element)) occupied.push(element.getBoundingClientRect());
    });
    const bounds = picker?.open ? picker.getBoundingClientRect() : {left: 0, right: innerWidth, top: 0, bottom: innerHeight};
    const inBounds = positions.filter(p => p.x >= Math.max(2,bounds.left) && p.x + size <= Math.min(innerWidth-2,bounds.right) && p.y >= Math.max(2,bounds.top) && p.y + size <= Math.min(innerHeight-2,bounds.bottom));
    const spot = inBounds.find(p => !occupied.some(r => p.x < r.right + 2 && p.x + size > r.left - 2 && p.y < r.bottom + 2 && p.y + size > r.top - 2)) || inBounds[0];
    if (spot) {
      cue.style.left = `${spot.x}px`;
      cue.style.top = `${spot.y}px`;
      cue.style.setProperty('--guide-angle', `${spot.angle}deg`);
      cue.hidden = false;
    }

  }

  function scheduleGuide() {
    if (framePending) return;
    framePending = true;
    requestAnimationFrame(updateGuide);
  }

  document.querySelectorAll('[data-portfolio-guide-toggle]').forEach((button) => {
    button.addEventListener('click', () => {
      enabled = !enabled;
      try { sessionStorage.setItem(preferenceKey, enabled ? 'on' : 'off'); } catch (_) { /* 현재 화면에서는 계속 동작한다. */ }
      scheduleGuide();
    });
  });
  locateButton?.addEventListener('click', () => {
    activeTarget?.scrollIntoView({ behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth', block: 'center' });
  });
  document.addEventListener('click', (event) => {
    const link = event.target.closest('[data-tour-catalog]');
    if (link) completedLinks.add(link);
    scheduleGuide();
  });
  ['input', 'change', 'toggle'].forEach((event) => document.addEventListener(event, scheduleGuide, true));
  addEventListener('scroll', scheduleGuide, { passive: true });
  addEventListener('resize', scheduleGuide);
  const main = document.getElementById('content');
  if (main) new MutationObserver((records) => {
    if (records.some(record => record.target !== cue && !cue.contains(record.target) && record.target !== halo && !halo.contains(record.target))) scheduleGuide();
  }).observe(main, { childList: true, subtree: true, attributes: true, attributeFilter: ['open', 'hidden', 'disabled'] });
  scheduleGuide();
})();

/* 연출은 서버 작업 상태와 실제 추출 근거만 사용한다. 사용자 확인·신청값을 바꾸지 않는다. */
(() => {
  'use strict';
  const panel = document.querySelector('[data-ocr-experience]');
  if (!panel) return;
  const state = panel.dataset.state;
  const sample = panel.dataset.mode === 'SAMPLE';
  const stage = document.querySelector('[data-ocr-stage]');
  const rows = [...document.querySelectorAll('[data-ocr-field]')];
  const key = `maeum.ocr-reading.${panel.dataset.document}`;
  const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)').matches;
  const set = (selector, value) => { document.querySelector(selector).textContent = value; };
  const pending = ['PENDING', 'RUNNING'].includes(state);
  panel.dataset.phase = pending ? 'reading' : state === 'SUCCEEDED' ? 'done' : state === 'FAILED' ? 'failed' : 'ready';
  if (pending) {
    set('[data-ocr-title]', sample ? '샘플 결과를 준비하고 있어요' : state === 'PENDING' ? '문자 인식 순서를 기다리고 있어요' : '원본의 글자를 읽고 있어요');
    set('[data-ocr-description]', sample ? '이 모드는 실제 인식 대신 고정된 샘플 응답을 사용합니다.' : '결과가 도착하면 상호·사업자번호·대표자·개업일을 정리합니다.');
    set('[data-ocr-badge]', state === 'PENDING' ? '처리 대기' : '처리 중');
    try { sessionStorage.setItem(key, 'pending'); } catch (_) { /* 저장소가 없어도 상태 표시는 유지한다. */ }
  } else if (state === 'FAILED') {
    set('[data-ocr-title]', '서류를 읽지 못했어요');
    set('[data-ocr-description]', '아래 오류와 원본을 확인해 주세요. 직접 확인값을 입력하거나 첨부 서류를 교체할 수 있어요.');
    set('[data-ocr-badge]', '확인 필요');
    try { sessionStorage.removeItem(key); } catch (_) { /* 실패 상태에 완료 연출을 남기지 않는다. */ }
  } else if (state === 'SUCCEEDED') {
    let needsReview = 0;
    rows.forEach((row, index) => {
      const extracted = row.querySelector('[data-ocr-extracted]').dataset.value;
      const submitted = row.querySelector('[data-ocr-application]').textContent;
      const normalize = value => row.dataset.ocrField === 'businessNo' || row.dataset.ocrField === 'openedOn'
        ? value.replace(/\D/g, '') : value.normalize('NFKC').replace(/\s+/g, '').trim();
      const matches = Boolean(extracted) && normalize(extracted) === normalize(submitted) && row.dataset.reviewRequired !== 'true';
      const badge = row.querySelector('[data-ocr-match]');
      badge.hidden = false;
      badge.textContent = matches ? '✓ 일치' : extracted ? '원본 확인 필요' : '직접 입력 필요';
      row.dataset.match = matches ? 'yes' : 'review';
      row.querySelectorAll('[data-ocr-legacy-mismatch]').forEach(label => label.hidden = true);
      row.style.setProperty('--ocr-order', index);
      if (!matches) needsReview++;
    });
    set('[data-ocr-title]', sample ? '샘플 결과를 정리했어요' : '문자 인식을 완료했어요');
    set('[data-ocr-description]', `4개 항목 중 ${needsReview}개 추가 확인 필요 · 일치한 값도 원본을 보고 최종 확인해 주세요.`);
    set('[data-ocr-badge]', sample ? '샘플 고정 응답' : '인식 완료');
    let reveal = false;
    try {
      reveal = sessionStorage.getItem(key) === 'pending';
      sessionStorage.removeItem(key);
    } catch (_) { /* 결과는 애니메이션 없이도 즉시 읽을 수 있다. */ }
    if (reveal && !reducedMotion) panel.closest('main').classList.add('ocr-reveal');
    // 좌표가 없는 항목은 추측해서 원본에 표시하지 않는다.
    const overlay = document.querySelector('[data-ocr-overlay]');
    const width = Number(stage.dataset.imageWidth), height = Number(stage.dataset.imageHeight);
    if (!sample && width > 0 && height > 0) {
      overlay.setAttribute('viewBox', `0 0 ${width} ${height}`);
      rows.forEach((row, index) => {
        let polygon;
        try { polygon = JSON.parse(row.dataset.polygon); } catch (_) { return; }
        if (!Array.isArray(polygon) || polygon.length < 3 || !polygon.every(point => Array.isArray(point) && point.length === 2 && point.every(Number.isFinite))) return;
        const shape = document.createElementNS('http://www.w3.org/2000/svg', 'polygon');
        shape.setAttribute('points', polygon.map(point => point.join(',')).join(' '));
        shape.style.setProperty('--ocr-order', index);
        shape.dataset.match = row.dataset.match;
        overlay.append(shape);
        row.addEventListener('mouseenter', () => shape.classList.add('is-active'));
        row.addEventListener('mouseleave', () => shape.classList.remove('is-active'));
      });
    }
  }
  document.querySelectorAll('form[action$="/ocr"]').forEach(form => {
    form.addEventListener('submit', () => {
      try { sessionStorage.setItem(key, 'pending'); } catch (_) { /* 요청과 결과의 정확성에는 영향이 없다. */ }
    });
  });
})();

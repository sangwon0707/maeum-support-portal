/* 보조 동작만 담당한다. 검증·권한·상태 변경은 서버가 다시 검사한다. */
document.querySelector('[data-error-summary]')?.focus();
document.querySelectorAll('[data-back]').forEach((button) => {
  button.addEventListener('click', () => {
    if (history.length > 1) history.back();
    else location.assign('./');
  });
});
const confirmationDialog = document.querySelector('[data-action-confirmation]');
if (confirmationDialog) {
  const accept = confirmationDialog.querySelector('[data-action-confirm]');
  let pending = null;
  let approvedForm = null;
  let returnFocus = null;
  const submitting = new WeakSet();

  document.querySelectorAll('form[data-confirm]').forEach(form => {
    form.addEventListener('submit', event => {
      if (submitting.has(form)) { event.preventDefault(); return; }
      if (approvedForm === form) {
        submitting.add(form);
        return;
      }
      event.preventDefault();
      if (confirmationDialog.open) return;
      pending = { form, submitter: event.submitter };
      returnFocus = event.submitter || document.activeElement;
      confirmationDialog.querySelector('[data-action-confirmation-title]').textContent = form.dataset.confirmTitle || '변경 내용을 적용할까요?';
      confirmationDialog.querySelector('[data-action-confirmation-message]').textContent = form.dataset.confirm;
      const result = form.querySelector('#decision');
      const summary = confirmationDialog.querySelector('[data-action-confirmation-summary]');
      summary.hidden = !result;
      summary.textContent = result ? `선택한 결과: ${result.selectedOptions[0].textContent}\n확정 사유: ${form.querySelector('#decisionReason').value.trim()}` : '';
      accept.textContent = form.dataset.confirmLabel || '확인하고 적용';
      accept.disabled = false;
      confirmationDialog.showModal();
      confirmationDialog.querySelector('.action-confirmation-actions [data-action-cancel]').focus();
    });
  });
  confirmationDialog.querySelectorAll('[data-action-cancel]').forEach(button => {
    button.addEventListener('click', () => confirmationDialog.close());
  });
  confirmationDialog.addEventListener('close', () => {
    pending = null;
    if (!approvedForm) returnFocus?.focus();
  });
  accept.addEventListener('click', () => {
    if (!pending || accept.disabled) return;
    const { form, submitter } = pending;
    accept.disabled = true;
    // 원래 제출 버튼과 브라우저 검증을 유지한다. 모달 확인은 서버 검증을 대신하지 않는다.
    approvedForm = form;
    confirmationDialog.close();
    if (submitter) form.requestSubmit(submitter);
    else form.requestSubmit();
    approvedForm = null;
  });
  window.addEventListener('pageshow', () => {
    document.querySelectorAll('form[data-confirm]').forEach(form => submitting.delete(form));
    pending = null;
    approvedForm = null;
    accept.disabled = false;
    if (confirmationDialog.open) confirmationDialog.close();
  });
}
// OCR 대기 화면만 새로고침한다. 완료된 결과 화면은 자동으로 이동하지 않는다.
if (document.querySelector('[data-ocr-pending]')) {
  window.setTimeout(() => location.reload(), 1800);
}

// 사업비 미리보기는 입력 보조다. 저장·제출 금액은 서버에서 별도로 검증한다.
const requestedAmount = document.getElementById('requestedAmount');
const selfContribution = document.getElementById('selfContribution');
const totalProjectCost = document.getElementById('total-project-cost');
if (requestedAmount && selfContribution && totalProjectCost) {
  const updateTotal = () => {
    const requested = Number(requestedAmount.value);
    const contribution = Number(selfContribution.value);
    const complete = requestedAmount.value !== '' && selfContribution.value !== '';
    totalProjectCost.textContent = complete && Number.isSafeInteger(requested) && Number.isSafeInteger(contribution) && requested >= 0 && contribution >= 0
      ? `${(requested + contribution).toLocaleString('ko-KR')}원` : '신청금액과 자부담을 입력해 주세요.';
  };
  requestedAmount.addEventListener('input', updateTotal);
  selfContribution.addEventListener('input', updateTotal);
  updateTotal();
}

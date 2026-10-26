/* 보조 동작만 담당한다. 검증·권한·상태 변경은 서버가 다시 검사한다. */
document.querySelector('[data-error-summary]')?.focus();
document.querySelectorAll('[data-back]').forEach((button) => {
  button.addEventListener('click', () => {
    if (history.length > 1) history.back();
    else location.assign('./');
  });
});
document.querySelectorAll('form[data-confirm]').forEach((form) => {
  form.addEventListener('submit', (event) => {
    if (!window.confirm(form.dataset.confirm)) event.preventDefault();
  });
});
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

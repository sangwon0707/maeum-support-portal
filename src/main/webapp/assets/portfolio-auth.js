/* 공개된 샘플 자격 증명만 입력한다. 인증은 기존 로그인 경로에서 수행한다. */
(() => {
  'use strict';
  const fillButton = document.querySelector('[data-fill-test-account]');
  fillButton?.addEventListener('click', () => {
    const username = document.getElementById('username');
    const password = document.getElementById('password');
    username.value = 'applicant';
    password.value = 'Portfolio2026!';
    [username, password].forEach(input => input.dispatchEvent(new Event('input', { bubbles: true })));
    document.querySelector('[data-test-account-status]').textContent = '신청자 테스트 계정을 입력했습니다. 로그인 버튼을 눌러 주세요.';
    fillButton.closest('form').querySelector('button[type="submit"], button:not([type])').focus();
  });

  const loginForm = document.querySelector('[data-demo-login]');
  const loginDialog = document.querySelector('[data-demo-login-dialog]');
  if (loginForm && loginDialog) {
    const acknowledgment = loginDialog.querySelector('[data-demo-login-acknowledgment]');
    const confirmButton = loginDialog.querySelector('[data-demo-login-confirm]');
    let noticeConfirmed = false;
    let submitting = false;
    let returnFocus;

    loginForm.addEventListener('submit', event => {
      if (noticeConfirmed && !submitting) {
        submitting = true;
        loginForm.querySelector('button[type="submit"]').disabled = true;
        return;
      }
      event.preventDefault();
      if (submitting || loginDialog.open) return;
      returnFocus = document.activeElement;
      acknowledgment.checked = false;
      confirmButton.disabled = true;
      loginDialog.showModal();
      acknowledgment.focus();
    });
    acknowledgment.addEventListener('change', () => {
      confirmButton.disabled = !acknowledgment.checked;
    });
    loginDialog.querySelectorAll('[data-demo-login-cancel]').forEach(button => {
      button.addEventListener('click', () => loginDialog.close());
    });
    loginDialog.addEventListener('close', () => {
      if (!noticeConfirmed) returnFocus?.focus();
    });
    confirmButton.addEventListener('click', () => {
      if (!acknowledgment.checked || submitting) return;
      // 안내 확인은 이번 로그인에만 적용하며 개인정보 수집 동의로 저장하지 않는다.
      noticeConfirmed = true;
      loginDialog.close();
      loginForm.requestSubmit();
      noticeConfirmed = false;
    });
    // 뒤로가기로 복원된 로그인 화면에서도 확인 절차를 다시 거친다.
    window.addEventListener('pageshow', () => {
      submitting = false;
      noticeConfirmed = false;
      loginForm.querySelector('button[type="submit"]').disabled = false;
      acknowledgment.checked = false;
      confirmButton.disabled = true;
      if (loginDialog.open) loginDialog.close();
    });
  }

  const preview = document.querySelector('[data-registration-preview]');
  if (!preview) return;
  const password = preview.querySelector('#new-password');
  const confirmation = preview.querySelector('#passwordConfirmation');
  const status = preview.querySelector('[data-registration-status]');
  preview.querySelectorAll('input').forEach(input => {
    input.addEventListener('input', () => {
      confirmation.setCustomValidity('');
      status.textContent = '';
    });
  });
  preview.querySelector('[data-check-registration]').addEventListener('click', () => {
    confirmation.setCustomValidity(password.value === confirmation.value ? '' : '비밀번호가 일치하지 않습니다.');
    const invalid = [...preview.querySelectorAll('input')].find(input => !input.checkValidity());
    if (invalid) {
      invalid.reportValidity();
      return;
    }
    // 폼 전송·저장소 기록 없이 브라우저 안에서만 입력 형식을 확인한다.
    status.textContent = '입력 형식을 확인했습니다. 계정 생성 없이 테스트 계정으로 로그인해 주세요.';
  });
})();

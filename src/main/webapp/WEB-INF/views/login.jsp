<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="로그인">
  <div class="narrow">
    <div class="page-heading">
      <h1>로그인</h1>
      <p>신청 내역과 배정된 업무를 안전하게 확인하세요.</p>
    </div>
    <c:if test="${not empty param.error}">
      <div class="alert error" role="alert">아이디 또는 비밀번호를 확인해 주세요.</div>
    </c:if>
    <script src="${base}/assets/portfolio-auth.js?v=3" defer></script>
    <form method="post" action="${base}/login" class="content-panel" data-demo-login>
      <div class="test-account-entry">
        <div class="test-account-buttons">
          <button type="button" class="krds-btn medium secondary" data-fill-test-account="applicant">사용자 테스트 계정</button>
          <button type="button" class="krds-btn medium secondary" data-fill-test-account="admin">관리자 테스트 계정</button>
        </div>
        <p class="field-help" data-test-account-status role="status">사용자 또는 관리자 계정을 선택하면 아이디와 비밀번호를 채웁니다.</p>
      </div>
      <t:csrf />
      <div class="field"><label for="username">아이디</label><input class="krds-input" id="username"
          name="username" autocomplete="username" required></div>
      <div class="field"><label for="password">비밀번호</label><input class="krds-input" id="password"
          name="password" type="password" autocomplete="current-password" required></div><button type="submit"
        class="krds-btn large primary full">로그인</button>
    </form>
    <dialog class="demo-login-dialog" data-demo-login-dialog aria-labelledby="demo-login-title" aria-describedby="demo-login-description">
      <div class="demo-login-heading">
        <span class="demo-login-badge">공개 계정 안내</span>
        <button type="button" class="demo-login-close" data-demo-login-cancel aria-label="안내 닫기">×</button>
      </div>
      <h2 id="demo-login-title">로그인 전에 확인해 주세요</h2>
      <p id="demo-login-description">이 사이트는 <strong>포트폴리오용 데모 사이트</strong>입니다. 공개 계정에는 여러 사람이 접근할 수 있습니다.</p>
      <ul class="demo-login-notice">
        <li>같은 계정으로 로그인한 다른 방문자가 신청 내용과 첨부 문서를 조회하거나 수정할 수 있습니다.</li>
        <li><strong>주민등록번호, 계좌번호, 실제 계정 비밀번호 등 중요한 개인정보를 절대 입력하거나 업로드하지 마세요.</strong></li>
        <li>제공된 문서 또는 가상 정보만 사용해 주세요. 공개 계정의 신청 자료는 매일 오전 4시(한국시간)에 초기화됩니다. 처리 중인 문서는 완료 후 정리됩니다.</li>
      </ul>
      <p class="demo-login-responsibility">안내를 확인한 후에도 실제 개인정보나 민감한 정보를 입력·업로드하여 발생하는 노출에 대해서는 이용자 본인에게 책임이 있습니다. 다만, 운영자의 고의·과실에 따른 법적 책임은 제외되지 않습니다.</p>
      <label class="checkbox-label demo-login-acknowledgment"><input type="checkbox" data-demo-login-acknowledgment><span>공개 계정의 자료 공유와 정보 입력 제한을 확인했으며, 실제 개인정보나 민감한 정보를 입력·업로드하지 않겠습니다.</span></label>
      <p class="demo-login-policy"><a href="${base}/terms" target="_blank" rel="noopener">이용약관</a><span aria-hidden="true"> · </span><a href="${base}/privacy" target="_blank" rel="noopener">개인정보 처리방침</a></p>
      <div class="demo-login-actions"><button type="button" class="krds-btn medium secondary" data-demo-login-cancel>취소</button><button type="button" class="krds-btn medium primary" data-demo-login-confirm disabled>확인하고 로그인</button></div>
    </dialog>
    <p>처음 이용하시나요? <a href="${base}/register">신청자 회원가입</a></p>
    <p class="field-help">관리자 계정에서는 접수 검토·보완 요청·결과 처리를 확인할 수 있습니다.</p>
    <a href="${base}/guide">신청 절차와 준비 서류 확인</a>
  </div>
</t:layout>
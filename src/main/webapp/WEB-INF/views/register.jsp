<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="회원가입">
  <div class="narrow">
    <div class="page-heading"><p class="eyebrow">신청자 계정</p><h1>회원가입</h1>
      <p>회원가입 화면의 입력 항목을 확인할 수 있습니다.</p></div>
    <p class="field-help">계정은 생성되지 않습니다. 서비스 이용은 로그인 화면의 테스트 계정을 이용해 주세요.</p>
    <script src="${base}/assets/portfolio-auth.js?v=1" defer></script>
    <section class="content-panel" data-registration-preview aria-label="회원가입 입력 예시">
      <div class="field"><label for="loginId">아이디</label><input class="krds-input" id="loginId" name="loginId" value="<c:out value='${loginId}'/>" pattern="[a-z][a-z0-9_-]{3,29}" maxlength="30" autocomplete="username" required><p class="field-help">영문 소문자로 시작하는 4~30자. 숫자·밑줄·하이픈을 사용할 수 있습니다.</p></div>
      <div class="field"><label for="displayName">표시 이름</label><input class="krds-input" id="displayName" name="displayName" value="<c:out value='${displayName}'/>" maxlength="50" autocomplete="nickname" required></div>
      <div class="field"><label for="new-password">비밀번호</label><input class="krds-input" id="new-password" name="password" type="password" minlength="10" maxlength="72" autocomplete="new-password" required><p class="field-help">10자 이상으로 입력해 주세요. 다른 서비스에서 사용 중인 비밀번호는 사용하지 마세요.</p></div>
      <div class="field"><label for="passwordConfirmation">비밀번호 확인</label><input class="krds-input" id="passwordConfirmation" name="passwordConfirmation" type="password" minlength="10" maxlength="72" autocomplete="new-password" required></div>
      <button type="button" class="krds-btn large primary full" data-check-registration>입력 확인</button>
      <p class="field-help" data-registration-status role="status" aria-live="polite"></p>
    </section><p>테스트 계정으로 이용하시겠어요? <a href="${base}/login">로그인</a></p>
  </div>
</t:layout>

<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="처리 안내">
  <div class="narrow error-page">
    <p class="eyebrow">처리 안내 · ${errorStatus}</p>
    <h1>요청을 처리하지 못했어요.</h1>
    <div class="alert error" role="alert" tabindex="-1" data-error-summary>
      <c:out value="${errorMessage}" />
    </div>
    <c:choose><c:when test="${errorStatus==503}"><p>서비스 연결 상태를 확인 중입니다. 요청이 반영되었는지는 연결 복구 후 나의 신청에서 확인해 주세요.</p></c:when>
      <c:otherwise><p>입력 내용과 현재 업무 상태를 확인한 뒤 다시 시도해 주세요.</p></c:otherwise></c:choose>
    <div class="actions"><button class="krds-btn medium secondary" data-back>이전 화면으로</button><a
        class="krds-btn medium primary" href="${base}/my">나의 업무로</a></div>
  </div>
</t:layout>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="제출본 확인">
  <div class="page-heading">
    <p class="eyebrow">
      <c:out value="${application.receiptNo}" />
    </p>
    <h1>제출본 ${revision.revision_no}</h1>
    <p>제출 당시의 정보와 서류입니다. 현재 수정본과 다를 수 있습니다.</p>
  </div>
  <section class="content-panel">
    <h2>신청 정보</h2>
    <dl class="definition-list">
      <dt>상호</dt>
      <dd>
        <c:out value="${snapshot.businessName}" />
      </dd>
      <dt>대표자</dt>
      <dd>
        <c:out value="${snapshot.representative}" />
      </dd>
      <dt>신청금액</dt>
      <dd>
        <c:out value="${snapshot.requestedAmount}" />원
      </dd>
      <dt>사업자등록번호</dt><dd><c:out value="${snapshot.businessNo}" default="미입력" /></dd>
      <dt>개업일</dt><dd><c:out value="${snapshot.openedOn}" default="미입력" /></dd>
      <dt>연락처</dt><dd><c:out value="${snapshot.phone}" default="미입력" /></dd>
      <dt>사업장 주소</dt><dd><c:out value="${snapshot.address}" default="미입력" /></dd>
      <dt>업태</dt><dd><c:out value="${snapshot.businessType}" default="미입력" /></dd>
      <dt>종목</dt><dd><c:out value="${snapshot.businessItem}" default="미입력" /></dd>
      <dt>자부담 (원)</dt><dd><c:out value="${snapshot.selfContribution}" default="미입력" /></dd>
      <dt>총사업비 (원)</dt><dd><c:out value="${snapshot.totalProjectCost}" default="미입력" /></dd>
      <dt>개선 계획</dt>
      <dd>
        <c:out value="${snapshot.planText}" />
      </dd>
    </dl>
  </section>
  <section class="content-panel">
    <h2>제출한 서류</h2>
    <c:forEach var="document" items="${documents}">
      <p><a href="${base}/documents/${document.id}/file" target="_blank" rel="noopener">${document.typeLabel}
          · 버전 ${document.version} ↗</a></p>
    </c:forEach>
  </section>
  <c:if test="${not empty snapshot.sourceIntakeId}"><p><a href="${base}/applications/${application.id}/intake?intakeId=${snapshot.sourceIntakeId}">제출에 사용한 원본·분할 문서·추출 사진 확인 →</a></p></c:if>
  <a class="krds-btn medium secondary" href="${base}/applications/${application.id}">진행 현황으로</a>
</t:layout>
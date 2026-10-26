<%@ taglib prefix="p" uri="urn:maeum:portal" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="신청 진행 현황">
  <div class="page-heading">
    <p class="eyebrow">
      <c:out value="${application.receiptNo}" default="접수번호 발급 전" />
    </p>
    <h1>신청 진행 현황</h1>
    <p>
      <c:out value="${application.programTitle}" />
    </p>
  </div>
  <section class="status-panel">
    <t:status status="${application.status}" />
    <h2>
      <c:out value="${application.businessName}" />
    </h2>
    <p>${application.status=='DRAFT'?'작성 내용을 확인한 후 제출해 주세요.':application.status=='NEEDS_SUPPLEMENT'?'담당자가 요청한
      항목을 기한 내 보완해 주세요.':application.status=='APPROVED'?'선정 결과가 확정되었습니다. 실제 지급은 이루어지지
      않습니다.':application.status=='REJECTED'?'미선정 결과가 확정되었습니다. 사유를 확인해 주세요.':'담당자가 제출한 내용을 확인합니다.'}</p>
    <c:if test="${not empty review.reason}">
      <p><strong>처리 사유</strong>
        <c:out value="${review.reason}" />
      </p>
    </c:if>
    <c:if test="${not empty supplement}">
      <p>
        <c:out value="${supplement.reason}" /> · 기한 ${supplement.dueAtLabel} (한국 시간)
      </p>
    </c:if>
    <c:if
      test="${actor.role=='APPLICANT' && (application.status=='DRAFT' || application.status=='NEEDS_SUPPLEMENT')}">
      <a class="krds-btn medium primary" href="${base}/applications/${application.id}/edit">신청 작성·보완</a>
    </c:if>
  </section>
  <div class="detail-layout">
    <section class="content-panel">
      <h2>처리 이력</h2>
      <ol class="timeline">
        <c:forEach var="event" items="${history}">
          <li><strong>
              <c:out value="${event.reason}" />
            </strong>
            <p>
              <c:out value="${event.display_name}" /> · ${p:koreanTime(event.created_at)} (한국 시간)
            </p>
          </li>
        </c:forEach>
      </ol>
      <c:if test="${empty history}">
        <p>아직 제출하지 않았습니다.</p>
      </c:if>
    </section>
    <aside class="content-panel">
      <h2>제출본</h2>
      <c:forEach var="revision" items="${revisions}">
        <p><a href="${base}/applications/${application.id}/revisions/${revision.id}">제출본
            ${revision.revision_no} · ${p:koreanTime(revision.submitted_at)} (한국 시간) →</a></p>
      </c:forEach>
      <p class="field-help">제출 당시의 정보와 서류 버전을 함께 보존합니다.</p>
      <h3>현재 첨부</h3>
      <c:forEach var="document" items="${documents}">
        <p><a href="${base}/documents/${document.id}/file" target="_blank"
            rel="noopener">${document.typeLabel} v${document.version} ↗</a></p>
      </c:forEach>
    </aside>
  </div>
</t:layout>
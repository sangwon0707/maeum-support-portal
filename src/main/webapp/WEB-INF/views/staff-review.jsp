<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="신청 서류 검토">
  <div class="page-heading">
    <p class="eyebrow">
      <c:out value="${application.receiptNo}" default="미제출" />
    </p>
    <h1>
      <c:out value="${application.businessName}" /> 신청 검토
    </h1>
    <t:status status="${application.status}" />
  </div>
  <div class="review-layout">
    <section class="content-panel">
      <h2>신청 정보·서류</h2>
      <dl class="definition-list">
        <dt>대표자</dt>
        <dd>
          <c:out value="${application.representative}" />
        </dd>
        <dt>신청금액</dt>
        <dd>
          <fmt:formatNumber value="${application.requestedAmount}" />원
        </dd>
      <dt>사업자등록번호</dt><dd><c:out value="${application.businessNo}" default="미입력" /></dd>
      <dt>개업일</dt><dd><c:out value="${application.openedOn}" default="미입력" /></dd>
      <dt>연락처</dt><dd><c:out value="${application.phone}" default="미입력" /></dd>
      <dt>사업장 주소</dt><dd><c:out value="${application.address}" default="미입력" /></dd>
      <dt>업태</dt><dd><c:out value="${application.businessType}" default="미입력" /></dd>
      <dt>종목</dt><dd><c:out value="${application.businessItem}" default="미입력" /></dd>
      <dt>자부담 (원)</dt><dd><c:out value="${application.selfContribution}" default="미입력" /></dd>
      <dt>총사업비 (원)</dt><dd><c:out value="${application.totalProjectCost}" default="미입력" /></dd>
        <dt>개선 계획</dt>
        <dd>
          <c:out value="${application.planText}" />
        </dd>
      </dl>
      <c:forEach var="document" items="${documents}">
        <h3>${document.typeLabel} · v${document.version}</h3><a href="${base}/documents/${document.id}/file"
          target="_blank" rel="noopener"><img class="document-preview"
            src="${base}/documents/${document.id}/file" alt="${document.typeLabel} 원본 열기"></a>
      </c:forEach><a class="krds-btn medium secondary" href="${base}/applications/${application.id}/ocr">OCR
        추출·확인값 보기</a>
    </section>
    <aside>
      <c:if test="${application.status=='SUBMITTED'}">
        <section class="content-panel">
          <h2>검토 시작</h2>
          <p>신청 내용과 첨부 서류를 확인한 뒤 보완 요청 또는 결과 확정을 진행하세요.</p>
          <form method="post" action="${base}/staff/applications/${application.id}/start" class="inline-form">
            <t:csrf /><input type="hidden" name="version" value="${application.version}"><button
              class="krds-btn medium primary" data-tour="접수된 신청의 검토를 시작하세요.">검토 시작</button>
          </form>
        </section>
      </c:if>
      <c:if test="${application.status=='UNDER_REVIEW'}">
        <section class="content-panel">
          <h2>보완 요청</h2>
          <form method="post" action="${base}/staff/applications/${application.id}/supplement">
            <t:csrf /><input type="hidden" name="version" value="${application.version}">
            <div class="field"><label for="target">보완 항목</label><select class="krds-form-select" id="target"
                name="target">
                <option value="ESTIMATE">견적서</option>
                <option value="BUSINESS_CERT">사업자등록증</option>
                <option value="businessName">상호</option>
                <option value="planText">개선 계획</option>
              </select></div>
            <div class="field"><label for="supplementReason">요청 사유</label><textarea class="krds-input"
                id="supplementReason" name="reason" rows="3" maxlength="1000" required></textarea></div>
            <div class="field"><label for="dueAt">보완 기한 (한국 시간)</label><input class="krds-input" id="dueAt"
                name="dueAt" type="datetime-local" value="${defaultDueAt}" required></div><button
              class="krds-btn medium secondary">보완 요청 보내기</button>
          </form>
        </section>
        <section class="content-panel">
          <h2>결과 확정</h2>
          <form method="post" action="${base}/staff/applications/${application.id}/decision"
            data-confirm="결과 확정 후에는 수정할 수 없습니다. 확정할까요?">
            <t:csrf /><input type="hidden" name="version" value="${application.version}">
            <div class="field"><label for="decision">결과</label><select class="krds-form-select" id="decision"
                name="decision">
                <option value="APPROVED">선정</option>
                <option value="REJECTED">미선정</option>
              </select></div>
            <div class="field"><label for="decisionReason">확정 사유</label><textarea class="krds-input"
                id="decisionReason" name="reason" rows="3" maxlength="1000" required></textarea></div><label
              class="checkbox-label"><input type="checkbox" name="checked" value="true" required> 서류와 신청 정보를
              검토했습니다.</label><button class="krds-btn medium primary">결과 확정</button>
          </form>
        </section>
      </c:if>
      <c:if test="${not empty supplement}">
        <section class="content-panel">
          <h2>보완 응답 대기</h2>
          <p>
            <c:out value="${supplement.reason}" />
          </p>
          <p>${supplement.dueAtLabel}까지 (한국 시간)</p>
        </section>
      </c:if>
      <c:if test="${not empty review.decision}">
        <section class="content-panel">
          <h2>확정 결과</h2>
          <t:status status="${review.decision}" />
          <p>
            <c:out value="${review.reason}" />
          </p>
        </section>
      </c:if><a class="krds-btn medium secondary" href="${base}/staff/applications">← 접수 검토 목록</a>
    </aside>
  </div>
  <p><a href="${base}/applications/${application.id}/intake">원본·분할 서류·추출 사진 검토 →</a></p>
</t:layout>
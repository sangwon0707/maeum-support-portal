<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="지원사업 상세">
  <link rel="stylesheet" href="${base}/assets/intake.css?v=11">
  <script src="${base}/assets/intake.js?v=12" defer></script>
  <div class="page-heading"><span class="status">${program.open?'접수 중':'접수 대기·마감'}</span>
    <h1>
      <c:out value="${program.title}" />
    </h1>
    <p>사업 내용을 확인하고 필요한 서류를 준비해 주세요.</p>
  </div>
  <nav class="page-toc" aria-label="사업 안내 목차"><a href="#program-overview">사업 개요</a><a href="#program-target">대상·지원 내용</a><a href="#program-documents">제출 서류</a><a href="#program-contact">문의·절차</a></nav>
  <div class="detail-layout">
    <section>
      <div class="content-panel">
        <h2 id="program-overview">사업 안내</h2>
        <p class="prose">
          <c:out value="${program.body}" />
        </p>
        <h2 id="program-target">지원 대상</h2><p class="prose"><c:out value="${program.targetText}" default="지원 대상이 등록되지 않았습니다. 운영기관에 확인해 주세요." /></p>
        <h2>지원 항목과 사업비</h2><p class="prose"><c:out value="${program.supportText}" default="지원 항목이 등록되지 않았습니다." /></p>
        <h2>지원 제외 사항</h2><p class="prose"><c:out value="${program.exclusionText}" default="제외 사항이 등록되지 않았습니다." /></p>
        <h2>검토 기준</h2><p class="prose"><c:out value="${program.selectionText}" default="검토 기준이 등록되지 않았습니다." /></p>
        <h2 id="program-documents">준비할 서류</h2>
        <ul class="check-list">
          <li>신청서·계획서·사진·증빙을 한데 모은 PDF 한 개</li>
          <li>필수 증빙: 사업자등록증 · 개선 항목별 견적서</li>
        </ul>
        <p class="muted">각 10MiB 이하 · 포트폴리오 검증에는 가상 자료만 사용해 주세요.</p>
        <c:forEach var="sampleOption" items="${sampleOptions}"><p><strong><c:out value="${sampleOption.label}" /></strong><br><a href="${base}/samples/${sampleOption.code}/business-cert">사업자등록증 예시</a> · <a href="${base}/samples/${sampleOption.code}/estimate">견적서 예시</a></p></c:forEach>
        <h2 id="program-contact">문의처</h2><p><c:out value="${program.contactText}" default="운영기관 문의처가 등록되지 않았습니다." /></p>
        <h2>진행 절차</h2>
        <ol class="process-list">
          <li>신청서 작성</li>
          <li>서류 첨부·확인</li>
          <li>담당자 검토·보완</li>
          <li>결과 안내</li>
        </ol>
      </div>
    </section>
    <aside class="summary-panel">
      <h2>접수 정보</h2>
      <dl>
        <dt>접수 기간 (한국 시간)</dt>
        <dd>${program.opensAtLabel}<br>~ ${program.closesAtLabel}</dd>
        <dt>지원 한도</dt>
        <dd>최대
          <fmt:formatNumber value="${program.maxAmount}" />원
        </dd>
        <dt>제출 서류</dt>
        <dd>사업자등록증 · 견적서</dd>
      </dl>
      <c:choose>
        <c:when test="${empty actor}"><a class="krds-btn medium primary" data-tour="신청하려면 먼저 신청자 계정으로 로그인하세요." href="${base}/login">로그인 후 신청하기</a>
        </c:when>
        <c:when test="${actor.role=='APPLICANT' && program.open}">
          <form method="post" action="${base}/applications" class="inline-form">
            <t:csrf /><input type="hidden" name="programId" value="${program.id}">
            <p class="field-help">신청 방식을 선택해 주세요.</p>
            <div class="entry-methods"><button type="button" data-program-upload-open class="krds-btn medium primary" data-tour="직접 입력하거나 작성한 문서를 업로드해 신청할 수 있어요.">작성 문서 업로드</button>
              <button name="entry" value="FORM" class="krds-btn medium secondary">직접 입력</button></div>
          </form>
        </c:when>
        <c:otherwise>
          <p>접수 기간과 신청자 역할을 확인해 주세요.</p>
        </c:otherwise>
      </c:choose><a class="krds-btn medium secondary" href="${base}/guide">신청 방법·서류 확인</a>
    </aside>
  </div>
  <c:if test="${actor.role=='APPLICANT' && program.open}"><t:intake-picker options="${sampleOptions}" action="${base}/programs/${program.id}/intake" /></c:if>
</t:layout>
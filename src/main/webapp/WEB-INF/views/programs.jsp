<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="지원사업 안내">
  <div class="portal-page">
    <section class="campaign-banner" aria-labelledby="campaign-title">
      <img class="campaign-image" src="${base}/assets/images/neighborhood-renewal-v1.webp" alt="" width="1536" height="1024" fetchpriority="high">
      <div class="campaign-copy">
        <p class="campaign-eyebrow">소상공인의 새로운 시작을 잇다</p>
        <h1 id="campaign-title">우리 가게의 내일,<br><em>마음이음과 함께.</em></h1>
        <p>가게에 필요한 지원을 찾고,<br>서류 준비부터 온라인 신청까지 한곳에서.</p>
        <div class="campaign-actions"><a class="krds-btn medium primary" href="#announcements" data-tour-catalog data-tour="먼저 이 버튼으로 지원사업 목록을 살펴보세요.">지원사업 살펴보기 <span class="arrow-right" aria-hidden="true"></span></a><a href="${base}/guide">신청 방법 알아보기</a></div>
      </div>
      <small class="campaign-credit">AI 제작 안내 이미지 · 실제 지원 사례 아님</small>
    </section>
    <nav class="visual-shortcuts" aria-label="신청 업무 바로가기">
      <a href="#announcements"><img src="${base}/assets/images/guide-apply-v1.webp" alt="" width="96" height="96"><span><strong>사업 알아보기</strong><small>대상과 지원 조건 확인</small></span><span class="arrow-right" aria-hidden="true"></span></a>
      <a href="${base}/my"><img src="${base}/assets/images/guide-upload-v1.webp" alt="" width="96" height="96"><span><strong>신청서 작성</strong><small>작성·저장·서류 첨부</small></span><span class="arrow-right" aria-hidden="true"></span></a>
      <a href="${base}/guide#documents"><img src="${base}/assets/images/guide-review-v1.webp" alt="" width="96" height="96"><span><strong>서류 확인</strong><small>준비할 서류와 대조 항목</small></span><span class="arrow-right" aria-hidden="true"></span></a>
      <a href="${base}/my"><img src="${base}/assets/images/guide-result-v1.webp" alt="" width="96" height="96"><span><strong>진행 현황</strong><small>보완 요청·결과 확인</small></span><span class="arrow-right" aria-hidden="true"></span></a>
    </nav>
    <section id="announcements" aria-label="지원사업 공고">
      <div class="announcement-toolbar">
        <div><p class="eyebrow">우리 가게에 맞는 지원 찾기</p><h2>지원사업 공고 <span class="catalog-count">${programs.size()}건</span></h2></div>
        <form class="compact-search" method="get" action="${base}/programs#announcements" role="search">
          <label class="sr-only" for="state">접수 상태</label>
          <select id="state" name="state"><option value="">전체 상태</option><option value="OPEN" ${state=='OPEN'?'selected':''}>접수 중</option><option value="CLOSED" ${state=='CLOSED'?'selected':''}>마감</option></select>
          <label class="sr-only" for="keyword">사업명 검색</label><input id="keyword" name="keyword" value="<c:out value='${keyword}'/>" placeholder="사업명으로 검색" type="search" maxlength="100">
          <button type="submit" aria-label="공고 검색"><span class="search-symbol" aria-hidden="true"></span></button>
        </form>
      </div>
      <c:if test="${not empty programs}">
        <c:set var="featuredProgram" value="${programs[0]}" />
        <article class="featured-program visual-featured">
          <div class="featured-photo"><t:program-visual programKey="${featuredProgram.seedKey}" /></div>
          <div class="feature-story">
            <div class="feature-meta"><span class="status ${featuredProgram.open?'status-OPEN':'status-CLOSED'}">${featuredProgram.open?'접수 중':'접수 대기·마감'}</span><span>온라인 신청</span><span class="feature-divider"></span><span>가상 사업 공고</span></div>
            <h2><a href="${base}/programs/${featuredProgram.id}"><c:out value="${featuredProgram.title}" /></a></h2>
            <p class="feature-description"><c:out value="${featuredProgram.body}" default="지원 내용과 신청 조건을 상세 공고에서 확인해 주세요." /></p>
            <a class="feature-detail" href="${base}/programs/${featuredProgram.id}">공고 상세 확인 <span class="arrow-right" aria-hidden="true"></span></a>
          </div>
          <aside class="feature-application" aria-label="지원 금액과 접수 안내">
            <p class="funding-label">지원 한도</p>
            <p class="funding-amount"><span>최대</span><strong><fmt:formatNumber value="${featuredProgram.maxAmount}" /></strong><span>원</span></p>
            <dl class="application-facts"><div><dt>접수 시작</dt><dd>${featuredProgram.opensAtLabel}</dd></div><div><dt>접수 마감</dt><dd>${featuredProgram.closesAtLabel}</dd></div><div><dt>제출 서류</dt><dd>사업자등록증 · 견적서</dd></div></dl>
            <p class="application-timezone">접수 시작·마감은 한국 시간 기준</p>
            <a class="krds-btn medium primary full" data-tour="공고의 대상과 조건을 확인하고 신청으로 이동하세요." href="${base}/programs/${featuredProgram.id}">${featuredProgram.open?'사업 확인하고 신청하기':'접수 일정 확인하기'}<span class="arrow-right" aria-hidden="true"></span></a>
            <a class="application-guide-link" href="${base}/guide">신청 방법이 궁금하신가요?</a>
          </aside>
        </article>
        <c:if test="${programs.size() > 1}">
          <div class="catalog-grid">
            <c:forEach var="program" items="${programs}" begin="1">
              <article class="catalog-card visual-catalog-card">
                <a class="catalog-image-link" href="${base}/programs/${program.id}" tabindex="-1" aria-hidden="true"><t:program-visual programKey="${program.seedKey}" /></a>
                <div class="catalog-card-content">
                <div class="catalog-card-heading"><span class="status ${program.open?'status-OPEN':'status-CLOSED'}">${program.open?'접수 중':'접수 대기·마감'}</span><span>가상 사업 공고</span></div>
                <h3><a href="${base}/programs/${program.id}"><c:out value="${program.title}" /></a></h3>
                <p class="catalog-support"><c:out value="${program.supportText}" /></p>
                <dl><div><dt>지원 한도</dt><dd>최대 <fmt:formatNumber value="${program.maxAmount}" />원</dd></div><div><dt>접수 마감</dt><dd>${program.closesAtLabel}</dd></div></dl>
                <a class="catalog-detail" href="${base}/programs/${program.id}">지원 대상·내용 확인 <span class="arrow-right" aria-hidden="true"></span></a>
                </div>
              </article>
            </c:forEach>
          </div>
        </c:if>
        <section class="program-overview" aria-labelledby="overview-title">
          <div class="editorial-heading"><p class="eyebrow">지원 내용 확인</p><h2 id="overview-title">신청 전에<br>꼭 확인하세요.</h2><p>선택한 공고의 대상과 조건을<br>먼저 살펴보세요.</p><a href="${base}/programs/${featuredProgram.id}">상세 공고 보기 <span class="arrow-right" aria-hidden="true"></span></a></div>
          <dl class="overview-grid">
            <div><dt><span>01</span>지원 대상</dt><dd><c:out value="${featuredProgram.targetText}" default="상세 공고에서 지원 대상을 확인해 주세요." /></dd></div>
            <div><dt><span>02</span>지원 내용·사업비</dt><dd><c:out value="${featuredProgram.supportText}" default="상세 공고에서 지원 항목을 확인해 주세요." /></dd></div>
            <div><dt><span>03</span>지원 제외 사항</dt><dd><c:out value="${featuredProgram.exclusionText}" default="상세 공고에서 제외 사항을 확인해 주세요." /></dd></div>
            <div><dt><span>04</span>검토 기준</dt><dd><c:out value="${featuredProgram.selectionText}" default="상세 공고에서 검토 기준을 확인해 주세요." /></dd></div>
          </dl>
        </section>
      </c:if>
      <c:if test="${empty programs}">
        <div class="empty-state"><h2>조건에 맞는 공고가 없습니다.</h2><p>검색어 또는 접수 상태를 바꿔 다시 찾아보세요.</p><a class="krds-btn medium secondary" href="${base}/programs">전체 공고 보기</a></div>
      </c:if>
    </section>
    <section class="document-preparation" aria-labelledby="documents-title">
      <div class="document-intro"><img class="preparation-illustration" src="${base}/assets/images/guide-upload-v1.webp" alt="" width="180" height="180" loading="lazy"><p class="eyebrow">제출 서류</p><h2 id="documents-title">서류는 두 가지,<br>미리 준비해 주세요.</h2><p>작성한 신청서는 PDF로 올리고,<br>직접 작성할 때는 증빙 이미지를 첨부하세요.</p><a href="${base}/guide#documents">파일 준비 방법 <span class="arrow-right" aria-hidden="true"></span></a></div>
      <div class="document-requirements">
        <article><div class="document-heading"><span class="portal-icon icon-file" aria-hidden="true"></span><h3>사업자등록증</h3><span class="requirement-badge">필수</span></div><p>상호 · 사업자등록번호 · 대표자 · 개업일을 확인합니다.</p><div class="document-row-bottom"><span>문자 인식 후 원본과 직접 대조</span><a href="${base}/guide#examples">사업별 예시 고르기 <span class="portal-icon icon-download" aria-hidden="true"></span></a></div></article>
        <article><div class="document-heading"><span class="portal-icon icon-file" aria-hidden="true"></span><h3>개선 항목별 견적서</h3><span class="requirement-badge">필수</span></div><p>개선 항목 · 공급가액 · 부가세 · 총액을 확인합니다.</p><div class="document-row-bottom"><span>견적 내용과 신청 사업비 대조</span><a href="${base}/guide#examples">사업별 예시 고르기 <span class="portal-icon icon-download" aria-hidden="true"></span></a></div></article>
        <p class="document-note"><span class="portal-icon icon-information" aria-hidden="true"></span>신청서와 증빙을 묶은 PDF를 올리면 문서별로 나누어 확인할 수 있습니다. 파일당 10MiB 이하의 가상 자료를 사용해 주세요.</p>
      </div>
    </section>
    <section class="application-journey" aria-labelledby="journey-title">
      <div class="section-heading"><div><p class="eyebrow">진행 절차</p><h2 id="journey-title">신청은 이렇게 진행돼요.</h2></div><a class="subtle-link" href="${base}/guide">전체 이용 안내 <span class="arrow-right" aria-hidden="true"></span></a></div>
      <ol class="journey-list"><li><img class="journey-illustration" src="${base}/assets/images/guide-apply-v1.webp" alt="" width="120" height="120" loading="lazy"><span class="journey-step">01</span><h3>신청서 작성</h3><p>사업자 정보와 개선 계획,<br>신청금액·자부담 입력</p></li><li><img class="journey-illustration" src="${base}/assets/images/guide-upload-v1.webp" alt="" width="120" height="120" loading="lazy"><span class="journey-step">02</span><h3>서류 첨부·제출</h3><p>필수 서류 업로드 후<br>내용을 확인하고 최종 제출</p></li><li><img class="journey-illustration" src="${base}/assets/images/guide-review-v1.webp" alt="" width="120" height="120" loading="lazy"><span class="journey-step">03</span><h3>검토·보완</h3><p>요청받은 항목과 기한 확인,<br>필요한 내용 수정·재제출</p></li><li><img class="journey-illustration" src="${base}/assets/images/guide-result-v1.webp" alt="" width="120" height="120" loading="lazy"><span class="journey-step">04</span><h3>결과 확인</h3><p>나의 신청에서<br>검토 결과와 처리 이력 확인</p></li></ol>
    </section>
    <div class="application-help"><span class="portal-icon icon-information" aria-hidden="true"></span><p><strong>이미 신청서를 작성하셨나요?</strong> 임시저장한 신청과 보완 요청은 나의 신청에서 확인할 수 있습니다.</p><a href="${base}/my">나의 신청으로 <span class="arrow-right" aria-hidden="true"></span></a></div>
  </div>
</t:layout>

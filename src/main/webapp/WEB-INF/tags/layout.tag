<%@ tag pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="title" required="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="base" value="${pageContext.request.contextPath}" scope="request" />
<!doctype html>
<html lang="ko" data-krds-mode="light">

<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>
    <c:out value="${title}" /> | 마음이음 지원사업
  </title>
  <link rel="icon" href="data:,">
  <link rel="stylesheet" href="${base}/assets/krds/css/common/common.css">
  <link rel="stylesheet" href="${base}/assets/krds/css/component/component.css">
  <link rel="stylesheet" href="${base}/assets/app.css?v=20261026-confirm">
  <link rel="stylesheet" href="${base}/assets/portal-theme.css?v=20261026-admin-review">
  <link rel="stylesheet" href="${base}/assets/portfolio-guide.css?v=12">
  <script src="${base}/assets/app.js?v=20261026-confirm" defer></script>
  <script src="${base}/assets/portfolio-guide.js?v=12" defer></script>
</head>

<body data-actor-role="${empty actor?'VISITOR':actor.role}"><a class="skip" href="#content">본문 바로가기</a>
  <div class="site-notice">
    <div class="wrap notice-content"><span class="demo-safety-text">포트폴리오용 데모 사이트 · 중요한 개인정보는 절대 입력·업로드하지 마세요. 실제 지원금은 지급하지 않습니다.</span>
      <aside class="portfolio-guide-widget" aria-label="포트폴리오 사용 안내">
        <a class="project-intro-link" href="${base}/about">프로젝트 소개</a>
        <button type="button" class="portfolio-guide-toggle" data-portfolio-guide-toggle aria-pressed="true"><span class="guide-switch" aria-hidden="true"></span><span data-tour-toggle-label>안내 켜짐</span></button>
        <span class="sr-only" data-tour-status role="status" aria-live="polite"></span>
        <button type="button" class="portfolio-guide-locate" data-tour-locate hidden>안내 위치 보기</button>
      </aside>
    </div>
  </div>
  <header class="site-header">
    <div class="wrap masthead">
      <a href="${base}/" class="brand"><img src="${base}/assets/brand/maeum-logo-a-v1.png" alt="마음이음" width="2056" height="765"><span class="sr-only">소상공인 지원사업</span></a>
      <nav class="main-nav" aria-label="주 메뉴">
        <a href="${base}/programs" aria-current="${title.startsWith('지원사업')?'page':'false'}">지원사업 안내</a>
        <a href="${base}/guide" aria-current="${title=='신청·서류 안내'?'page':'false'}">신청·서류 안내</a>
        <a href="${base}/${actor.role=='STAFF' || actor.role=='ADMIN'?'my':'mypage'}" aria-current="${title=='나의 신청' || title=='마이페이지'?'page':'false'}">${actor.role=='STAFF'?'담당 접수함':actor.role=='ADMIN'?'공고·배정 관리':'마이페이지'}</a>
        <c:if test="${actor.role=='STAFF'}"><a href="${base}/staff/reports">업무 통계</a></c:if>
        <c:if test="${actor.role=='ADMIN'}"><a href="${base}/staff/applications">접수 검토</a><a href="${base}/admin/reports">업무 통계</a><a href="${base}/admin/audit">감사 이력</a></c:if>
      </nav>
      <div class="account">
        <c:choose>
          <c:when test="${not empty actor}"><span><c:out value="${actor.displayName}" /></span><form method="post" action="${base}/logout"><t:csrf /><button class="text-button">로그아웃</button></form></c:when>
          <c:otherwise><a href="${base}/login">로그인</a><a href="${base}/register">회원가입</a></c:otherwise>
        </c:choose>
      </div>
    </div>
  </header>
  <main id="content" class="wrap main-content">
    <div class="breadcrumb" ${title=='지원사업 안내'?'hidden':''}><a href="${base}/">홈</a><span aria-hidden="true">/</span>
      <c:out value="${title}" />
    </div>
    <c:if test="${not empty notice}">
      <div class="alert success" role="status">
        <c:out value="${notice}" />
      </div>
    </c:if>
    <jsp:doBody />
  </main>
  <footer class="site-footer">
    <div class="wrap"><strong>마음이음 · 소상공인 지원사업</strong>
      <p>신청부터 서류 보완과 결과 확인까지, 하나의 흐름으로.</p>
      <nav class="footer-links" aria-label="서비스 정책"><a href="${base}/terms">이용약관</a><a class="privacy-link" href="${base}/privacy">개인정보 처리방침</a><a href="${base}/guide">신청·서류 안내</a></nav>
      <div class="footer-bottom"><span>마음이음 · 지원사업 신청·접수 관리</span><span>포트폴리오용 테스트 서비스 · © 이상원</span>
      </div>
    </div>
  </footer>
  <dialog class="action-confirmation" data-action-confirmation role="alertdialog" aria-labelledby="action-confirmation-title" aria-describedby="action-confirmation-message">
    <div class="action-confirmation-heading"><span class="action-confirmation-icon" aria-hidden="true">!</span><button type="button" class="text-button" data-action-cancel aria-label="확인창 닫기">×</button></div>
    <h2 id="action-confirmation-title" data-action-confirmation-title>변경 내용을 적용할까요?</h2>
    <p id="action-confirmation-message" data-action-confirmation-message></p>
    <p class="action-confirmation-summary" data-action-confirmation-summary hidden></p>
    <div class="action-confirmation-actions"><button type="button" class="krds-btn medium secondary" data-action-cancel>취소</button><button type="button" class="krds-btn medium primary" data-action-confirm>확인하고 적용</button></div>
  </dialog>
  <div class="portfolio-guide-halo" hidden aria-hidden="true"></div>
  <div class="portfolio-guide-cue" hidden aria-hidden="true"><svg viewBox="0 0 56 64" class="guide-arrow"><path d="M18 4H38V32H51L28 59L5 32H18Z" fill="#2667e8" stroke="white" stroke-width="4" stroke-linejoin="round" /></svg></div>
</body>

</html>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="마이페이지">
  <link rel="stylesheet" href="${base}/assets/mypage.css?v=1">
  <div class="page-heading"><h1>마이페이지</h1><p><c:out value="${actor.displayName}" />님의 신청 현황과 이용 설정을 확인하세요.</p></div>
  <t:account-nav active="profile" />
  <section class="mypage-overview" aria-labelledby="my-overview-title">
    <div class="mypage-section-heading"><h2 id="my-overview-title">나의 신청 현황</h2><a href="${base}/my">전체 ${summary.total}건 보기 →</a></div>
    <div class="mypage-counts">
      <a href="${base}/my?status=DRAFT"><span>작성 중</span><strong>${summary.DRAFT}<small>건</small></strong><span class="mypage-count-link">이어서 작성 →</span></a>
      <a href="${base}/my?status=SUBMITTED"><span>접수 완료</span><strong>${summary.SUBMITTED}<small>건</small></strong><span class="mypage-count-link">접수 내역 확인 →</span></a>
      <a href="${base}/my?status=NEEDS_SUPPLEMENT" class="${summary.NEEDS_SUPPLEMENT>0?'needs-attention':''}"><span>보완 요청</span><strong>${summary.NEEDS_SUPPLEMENT}<small>건</small></strong><span class="mypage-count-link">요청 사항 확인 →</span></a>
    </div>
    <p class="mypage-count-note">검토 중 ${summary.UNDER_REVIEW}건 · 선정 ${summary.APPROVED}건 · 미선정 ${summary.REJECTED}건</p>
  </section>
  <div class="mypage-details">
    <section class="mypage-panel" aria-labelledby="my-account-title">
      <h2 id="my-account-title">내 정보</h2>
      <dl class="mypage-account"><div><dt>아이디</dt><dd><c:out value="${actor.username}" /></dd></div><div><dt>이름</dt><dd><c:out value="${actor.displayName}" /></dd></div><div><dt>이용 권한</dt><dd><span class="mypage-role">신청자</span></dd></div></dl>
      <p class="mypage-account-notice"><c:choose><c:when test="${publicAccount}">누구나 이용할 수 있는 공개 테스트 계정입니다. 계정 정보 변경과 회원 탈퇴는 제공하지 않습니다. 신청·문서에는 가상 정보를 사용해 주세요. 신청 자료는 매일 오전 4시(한국시간)에 초기화되며, 처리 중인 문서는 완료 후 정리됩니다.</c:when><c:otherwise>계정 정보 변경은 현재 제공하지 않습니다. 신청·문서에는 가상 정보를 사용해 주세요.</c:otherwise></c:choose></p>
    </section>
    <section class="mypage-panel" aria-labelledby="my-settings-title">
      <h2 id="my-settings-title">이용 설정</h2>
      <div class="mypage-guide-setting"><div><h3>화면 이용 안내</h3><p>다음 버튼을 가리키는 화살표와<br>강조 테두리를 표시합니다.</p></div><button type="button" class="portfolio-guide-toggle mypage-guide-toggle" data-portfolio-guide-toggle aria-label="화면 이용 안내" aria-pressed="true"><span class="guide-switch" aria-hidden="true"></span><span data-guide-setting-label>켜짐</span></button></div>
      <p class="mypage-setting-note">설정은 이 탭에서 다른 화면으로 이동해도 유지됩니다.</p>
      <a class="mypage-help-link" href="${base}/guide">신청·서류 이용 안내 →</a>
    </section>
  </div>
</t:layout>

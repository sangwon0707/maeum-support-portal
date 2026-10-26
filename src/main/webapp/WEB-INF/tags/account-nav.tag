<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="active" required="true" type="java.lang.String" %>
<nav class="account-section-nav" aria-label="마이페이지 메뉴">
  <a href="${base}/mypage" ${active=='profile'?'aria-current="page"':''}>내 정보·설정</a>
  <a href="${base}/my" ${active=='applications'?'aria-current="page"':''}>나의 신청</a>
</nav>

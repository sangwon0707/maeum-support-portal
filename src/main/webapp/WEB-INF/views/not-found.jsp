<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="페이지를 찾을 수 없습니다">
  <link rel="stylesheet" href="${base}/assets/not-found.css?v=1">
  <section class="not-found" aria-labelledby="not-found-title">
    <div class="not-found-copy">
      <p class="not-found-code">404 <span>PAGE NOT FOUND</span></p>
      <h1 id="not-found-title">앗, 다른 골목으로<br>들어왔나 봐요.</h1>
      <p class="not-found-description">찾으시는 페이지가 없거나 주소가 바뀌었어요.<br>주소를 확인하거나 아래에서 다시 시작해 주세요.</p>
      <div class="not-found-actions">
        <a class="krds-btn medium primary" href="${base}/">홈으로 가기 <span aria-hidden="true">→</span></a>
        <a class="krds-btn medium secondary" href="${base}/programs">지원사업 둘러보기</a>
      </div>
      <button class="not-found-back" type="button" data-back>← 이전 화면으로</button>
    </div>
    <div class="not-found-art"><img src="${base}/assets/images/not-found-neighborhood-v1.webp" width="1536" height="1024" alt="지도에서 길을 찾는 가게 주인들과 서류 위에 앉은 고양이"></div>
  </section>
</t:layout>

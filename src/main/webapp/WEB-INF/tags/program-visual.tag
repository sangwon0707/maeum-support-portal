<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="programKey" required="false" type="java.lang.String" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- 관리자가 만든 공고에는 특정 업종의 사진을 임의로 연결하지 않는다. --%>
<c:set var="visualName" value="neighborhood-renewal-v1" />
<c:choose>
  <c:when test="${programKey=='local-digital-store-v1'}"><c:set var="visualName" value="program-digital-v1" /></c:when>
  <c:when test="${programKey=='local-energy-saving-v1'}"><c:set var="visualName" value="program-energy-v1" /></c:when>
  <c:when test="${programKey=='local-accessible-store-v1'}"><c:set var="visualName" value="program-access-v1" /></c:when>
  <c:when test="${programKey=='local-online-channel-v1'}"><c:set var="visualName" value="program-online-v1" /></c:when>
  <c:when test="${programKey=='local-store-safety-v1'}"><c:set var="visualName" value="program-safety-v1" /></c:when>
</c:choose>
<figure class="program-visual">
  <img src="${base}/assets/images/${visualName}.webp" alt="" loading="lazy" decoding="async" width="1536" height="1024">
  <figcaption>AI 제작 예시 이미지</figcaption>
</figure>

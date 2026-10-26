<%@ tag pageEncoding="UTF-8" %><%@ attribute name="status" required="true" type="java.lang.String" %><%@ taglib prefix="c" uri="jakarta.tags.core" %><span
  class="status status-${status}">
  <c:choose>
    <c:when test="${status=='DRAFT'}">작성 중</c:when>
    <c:when test="${status=='SUBMITTED'}">접수 완료</c:when>
    <c:when test="${status=='UNDER_REVIEW'}">검토 중</c:when>
    <c:when test="${status=='NEEDS_SUPPLEMENT'}">보완 요청</c:when>
    <c:when test="${status=='APPROVED'}">선정</c:when>
    <c:when test="${status=='REJECTED'}">미선정</c:when>
    <c:otherwise>
      <c:out value="${status}" />
    </c:otherwise>
  </c:choose>
</span>
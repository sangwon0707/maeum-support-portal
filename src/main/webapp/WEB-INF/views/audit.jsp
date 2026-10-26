<%@ taglib prefix="p" uri="urn:maeum:portal" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="감사 이력">
  <div class="page-heading">
    <h1>처리·접근 이력</h1>
    <p>현재 운영 공간의 최근 100개 이력입니다. 원문 서류와 인증 정보는 기록하지 않습니다.</p>
  </div>
  <form method="get" class="search-panel">
    <div class="field"><label for="auditKeyword">행위·내용 검색</label><input class="krds-input" id="auditKeyword"
        name="keyword" value="<c:out value='${param.keyword}'/>"></div><button
      class="krds-btn medium primary">조회</button>
  </form>
  <div class="table-wrap">
    <table>
      <caption>업무·파일 접근 감사 기록</caption>
      <thead>
        <tr>
          <th>시각 (한국 시간)</th>
          <th>행위자</th>
          <th>업무</th>
          <th>내용</th>
          <th>요청 식별자</th>
        </tr>
      </thead>
      <tbody>
        <c:forEach var="event" items="${events}">
          <tr>
            <td>${p:koreanTime(event.created_at)}</td>
            <td>
              <c:out value="${event.display_name}" />
            </td>
            <td>
              <c:out value="${event.action}" />
            </td>
            <td>
              <c:out value="${event.summary}" />
            </td>
            <td class="small">
              <c:out value="${event.request_id}" />
            </td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
  </div>
</t:layout>
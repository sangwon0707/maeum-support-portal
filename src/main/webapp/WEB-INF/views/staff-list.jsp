<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="담당 업무">
  <div class="page-heading">
    <h1>${actor.role=='ADMIN'?'전체 접수 검토':'담당 업무'}</h1>
    <p>${actor.role=='ADMIN'?'담당자 배정 없이 접수된 신청을 직접 검토하고 보완·결과를 처리할 수 있습니다.':'배정된 신청의 서류를 확인하고 보완과 결과 처리를 진행하세요.'}</p>
  </div>
  <form method="get" class="search-panel">
    <div class="field"><label for="keyword">상호·접수번호</label><input class="krds-input" id="keyword"
        name="keyword" value="<c:out value='${param.keyword}'/>"></div>
    <div class="field"><label for="status">상태</label><select class="krds-form-select" name="status"
        id="status">
        <option value="">전체</option>
        <c:forEach var="item"
          items="${['SUBMITTED','UNDER_REVIEW','NEEDS_SUPPLEMENT','APPROVED','REJECTED']}">
          <option value="${item}" ${param.status==item?'selected':''}>${item=='SUBMITTED'?'접수
            완료':item=='UNDER_REVIEW'?'검토 중':item=='NEEDS_SUPPLEMENT'?'보완 요청':item=='APPROVED'?'선정':'미선정'}
          </option>
        </c:forEach>
      </select></div>
    <div class="field"><label for="from">제출 시작일</label><input class="krds-input" id="from" name="from"
        type="date" value="<c:out value='${param.from}'/>"></div>
    <div class="field"><label for="until">제출 종료일</label><input class="krds-input" id="until" name="until"
        type="date" value="<c:out value='${param.until}'/>"></div><button
      class="krds-btn medium primary">조회</button>
  </form>
  <p>총 ${pagination.totalRecordCount}건</p>
  <div class="table-wrap">
    <table>
      <caption>${actor.role=='ADMIN'?'전체 신청 목록':'배정된 신청 목록'}</caption>
      <thead>
        <tr>
          <th>접수번호</th>
          <th>상호</th>
          <th>현재 상태</th>
          <th>최근 변경 (한국 시간)</th>
          <th>처리</th>
        </tr>
      </thead>
      <tbody>
        <c:forEach var="application" items="${applications}">
          <tr>
            <td>
              <c:out value="${application.receiptNo}" default="미제출" />
            </td>
            <td>
              <c:out value="${application.businessName}" />
            </td>
            <td>
              <t:status status="${application.status}" />
            </td>
            <td>${application.updatedAtLabel}</td>
            <td><a class="table-link" href="${base}/staff/applications/${application.id}">검토하기 →</a></td>
          </tr>
        </c:forEach>
        <c:if test="${empty applications}">
          <tr>
            <td colspan="5" class="empty-state">조건에 맞는 신청이 없습니다.</td>
          </tr>
        </c:if>
      </tbody>
    </table>
  </div>
  <nav class="pagination" aria-label="페이지 이동">
    <c:forEach var="number" begin="${pagination.firstPageNoOnPageList}"
      end="${pagination.lastPageNoOnPageList}">
      <c:url var="pageLink" value="/staff/applications">
        <c:param name="page" value="${number}" />
        <c:param name="keyword" value="${param.keyword}" />
        <c:param name="status" value="${param.status}" />
        <c:param name="from" value="${param.from}" />
        <c:param name="until" value="${param.until}" />
      </c:url><a href="${pageLink}" ${number==pagination.currentPageNo?'aria-current="page"':''}>${number}</a></c:forEach></nav>
</t:layout>
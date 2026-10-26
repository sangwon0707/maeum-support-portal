<%@ taglib prefix="p" uri="urn:maeum:portal" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="업무 통계">
  <div class="page-heading">
    <h1>업무 통계</h1>
    <p>${actor.demo?'현재 체험 공간':'운영 공간'}의 ${actor.role=='STAFF'?'본인 배정':'전체'} 신청 기준입니다. 운영 통계에 다른 데모 공간은 포함되지
      않습니다.</p>
  </div>
  <div class="report-grid">
    <section class="content-panel">
      <h2>현재 상태별 신청</h2>
      <div class="table-wrap">
        <table>
          <caption>상태별 신청 건수</caption>
          <thead>
            <tr>
              <th>상태</th>
              <th>건수</th>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="row" items="${statusCounts}">
              <tr>
                <td>
                  <t:status status="${row.status}" />
                </td>
                <td>${row.total}</td>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </div>
    </section>
    <section class="content-panel">
      <h2>담당자별 배정</h2>
      <div class="table-wrap">
        <table>
          <caption>담당자별 배정 건수</caption>
          <thead>
            <tr>
              <th>담당자</th>
              <th>건수</th>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="row" items="${staffCounts}">
              <tr>
                <td>
                  <c:out value="${row.staff_name}" />
                </td>
                <td>${row.total}</td>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </div>
    </section>
  </div>
  <section class="content-panel">
    <h2>보완 기한 초과</h2>
    <div class="table-wrap">
      <table>
        <caption>기한이 지난 열린 보완 요청</caption>
        <thead>
          <tr>
            <th>접수번호</th>
            <th>상호</th>
            <th>기한 (한국 시간)</th>
          </tr>
        </thead>
        <tbody>
          <c:forEach var="row" items="${overdue}">
            <tr>
              <td>
                <c:out value="${row.receipt_no}" />
              </td>
              <td>
                <c:out value="${row.business_name}" />
              </td>
              <td>${p:koreanTime(row.due_at)}</td>
            </tr>
          </c:forEach>
          <c:if test="${empty overdue}">
            <tr>
              <td colspan="3">기한이 지난 보완 요청이 없습니다.</td>
            </tr>
          </c:if>
        </tbody>
      </table>
    </div>
  </section>
</t:layout>
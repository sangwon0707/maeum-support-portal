<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="나의 신청">
  <link rel="stylesheet" href="${base}/assets/my-applications.css?v=1">
  <link rel="stylesheet" href="${base}/assets/mypage.css?v=1">
  <div class="page-heading">
    <h1>나의 신청</h1>
    <p>접수번호별 신청 내역과 진행 상태를 확인하세요.</p>
  </div>
  <t:account-nav active="applications" />
  <c:if test="${actor.username == 'applicant'}"><p class="field-help">공개 계정의 신청 자료는 매일 오전 4시(한국시간)에 초기화됩니다. 처리 중인 문서는 완료 후 정리됩니다.</p></c:if>
  <c:if test="${not empty selectedStatus}"><div class="my-status-filter"><t:status status="${selectedStatus}" /><a href="${base}/my">전체 신청 보기 ×</a></div></c:if>
  <div class="my-applications-heading"><p>총 <strong>${pagination.totalRecordCount}</strong>건</p><span>최근 수정 순</span></div>
  <c:choose>
    <c:when test="${not empty applications}">
      <div class="my-applications-scroll" role="region" aria-label="신청 내역 표" tabindex="0">
        <table class="my-applications-table">
          <caption>나의 신청 내역과 신청별 진행 메뉴</caption>
          <colgroup><col class="my-col-number"><col class="my-col-receipt"><col><col class="my-col-status"><col class="my-col-date"><col class="my-col-actions"></colgroup>
          <thead><tr><th scope="col">번호</th><th scope="col">접수번호</th><th scope="col">지원사업 / 상호</th><th scope="col">진행 상태</th><th scope="col">최근 수정</th><th scope="col">신청 관리</th></tr></thead>
          <tbody>
            <c:forEach var="application" items="${applications}" varStatus="row">
              <tr>
                <td class="my-row-number">${pagination.totalRecordCount-pagination.firstRecordIndex-row.index}</td>
                <td class="my-receipt"><c:choose><c:when test="${not empty application.receiptNo}"><c:out value="${application.receiptNo}" /></c:when><c:otherwise><span>발급 전</span></c:otherwise></c:choose></td>
                <th scope="row" class="my-business"><a href="${base}/applications/${application.id}"><c:out value="${application.programTitle}" /></a><span><c:out value="${application.businessName}" default="상호 미입력" /></span></th>
                <td><t:status status="${application.status}" /></td>
                <td class="my-modified">${application.updatedAtLabel}</td>
                <td><div class="my-applications-actions">
                  <c:choose>
                    <c:when test="${application.status=='DRAFT'}"><a class="krds-btn small primary" href="${base}/applications/${application.id}/edit" data-tour="작성 중인 신청서를 이어서 작성하세요.">이어서 작성</a></c:when>
                    <c:when test="${application.status=='NEEDS_SUPPLEMENT'}"><a class="krds-btn small primary" href="${base}/applications/${application.id}/edit" data-tour="담당자가 요청한 항목과 기한을 확인하세요.">보완하기</a></c:when>
                  </c:choose>
                  <a class="krds-btn small secondary" href="${base}/applications/${application.id}" aria-label="${not empty application.receiptNo?application.receiptNo:'작성 중 신청'} 진행 내역 보기" data-tour="접수 상태와 처리 결과를 확인하세요.">진행 내역</a>
                </div></td>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </div>
      <c:if test="${pagination.totalPageCount>1}"><nav class="pagination" aria-label="신청 내역 페이지 이동"><c:forEach var="number" begin="${pagination.firstPageNoOnPageList}" end="${pagination.lastPageNoOnPageList}"><c:url var="pageLink" value="/my"><c:param name="page" value="${number}" /><c:param name="status" value="${selectedStatus}" /></c:url><a href="${pageLink}" ${number==pagination.currentPageNo?'aria-current="page"':''}>${number}</a></c:forEach></nav></c:if>
    </c:when>
    <c:otherwise>
      <div class="empty-state"><c:choose><c:when test="${not empty selectedStatus}"><h2>해당 상태의 신청 내역이 없습니다.</h2><p>전체 신청 목록에서 다른 신청 내역을 확인해 주세요.</p><a class="krds-btn medium secondary" href="${base}/my">전체 신청 보기</a></c:when><c:otherwise><h2>아직 신청한 사업이 없어요.</h2><p>접수 중인 사업을 선택해 신청서를 작성해 주세요.</p><a class="krds-btn medium secondary" href="${base}/programs" data-tour="신청할 사업을 먼저 골라 주세요.">지원사업 살펴보기</a></c:otherwise></c:choose></div>
    </c:otherwise>
  </c:choose>
</t:layout>

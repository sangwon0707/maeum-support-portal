<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="공고·배정 관리">
  <div class="page-heading">
    <h1>공고·담당자 배정 관리</h1>
    <p>관리자는 담당자 배정 없이 직접 검토할 수 있습니다. 공고와 담당 업무를 관리합니다. 접수 시작 후 기간과 지원 한도 변경은 제한됩니다.</p>
  </div>
  <p><a class="krds-btn medium primary" href="${base}/staff/applications">접수 검토하기</a></p>
  <section class="content-panel">
    <div class="section-heading">
      <h2>공고 관리</h2><a class="krds-btn medium primary" href="${base}/admin/programs/new">새 공고 작성</a>
    </div>
    <div class="table-wrap">
      <table>
        <caption>관리 공고 목록</caption>
        <thead>
          <tr>
            <th>공고명</th>
            <th>게시 상태</th>
            <th>관리</th>
          </tr>
        </thead>
        <tbody>
          <c:forEach var="program" items="${programs}">
            <tr>
              <td>
                <c:out value="${program.title}" />
              </td>
              <td>
                <c:out value="${program.status}" />
              </td>
              <td><a href="${base}/admin/programs/${program.id}">수정 →</a></td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </div>
  </section>
  <section class="content-panel">
    <h2>담당자 배정</h2>
    <p>운영 공간의 담당자 계정에 배정합니다.</p>
    <div class="table-wrap">
      <table>
        <caption>신청별 담당 배정</caption>
        <thead>
          <tr>
            <th>신청</th>
            <th>상태</th>
            <th>배정</th>
          </tr>
        </thead>
        <tbody>
          <c:forEach var="application" items="${applications}">
            <tr>
              <td>
                <a href="${base}/staff/applications/${application.id}"><c:out value="${application.businessName}" /> (#${application.id}) · 검토하기 →</a>
              </td>
              <td>
                <t:status status="${application.status}" />
              </td>
              <td>
                <form method="post" action="${base}/admin/assign">
                  <t:csrf /><input type="hidden" name="applicationId" value="${application.id}"><label
                    for="staff-${application.id}">담당자</label><select class="krds-form-select"
                    id="staff-${application.id}" name="staffId">
                    <c:forEach var="staff" items="${staffAccounts}">
                      <option value="${staff.id}">
                        <c:out value="${staff.displayName}" />
                      </option>
                    </c:forEach>
                  </select><label class="sr-only" for="assign-${application.id}">배정 사유</label><input
                    class="krds-input" id="assign-${application.id}" name="reason" placeholder="배정 사유"
                    maxlength="1000" required><button class="krds-btn small secondary">담당자 배정</button>
                </form>
              </td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </div>
  </section>
</t:layout>
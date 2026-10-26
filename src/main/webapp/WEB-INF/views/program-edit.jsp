<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="공고 작성·수정">
  <div class="page-heading">
    <h1>공고 작성·수정</h1>
    <p>이 서비스는 사업자등록증과 견적서를 필수 서류로 사용합니다.</p>
  </div>
  <form method="post" action="${base}/admin/programs" class="content-panel">
    <t:csrf /><input type="hidden" name="id" value="${program.id}"><input type="hidden" name="version"
      value="${empty program.version?0:program.version}">
    <div class="field"><label for="title">공고명</label><input class="krds-input" id="title" name="title"
        value="<c:out value='${program.title}'/>" maxlength="200" required></div>
    <div class="field"><label for="body">공고 내용</label><textarea class="krds-input" id="body" name="body"
        rows="6" maxlength="10000" required><c:out value="${program.body}"/></textarea></div>
    <div class="field"><label for="targetText">지원 대상</label><textarea class="krds-input" id="targetText" name="targetText" rows="3" maxlength="5000"><c:out value="${program.targetText}" /></textarea></div>
    <div class="field"><label for="supportText">지원 항목과 사업비</label><textarea class="krds-input" id="supportText" name="supportText" rows="3" maxlength="5000"><c:out value="${program.supportText}" /></textarea></div>
    <div class="field"><label for="exclusionText">지원 제외 사항</label><textarea class="krds-input" id="exclusionText" name="exclusionText" rows="3" maxlength="5000"><c:out value="${program.exclusionText}" /></textarea></div>
    <div class="field"><label for="selectionText">검토 기준</label><textarea class="krds-input" id="selectionText" name="selectionText" rows="3" maxlength="5000"><c:out value="${program.selectionText}" /></textarea></div>
    <div class="field"><label for="contactText">문의처</label><textarea class="krds-input" id="contactText" name="contactText" rows="3" maxlength="500"><c:out value="${program.contactText}" /></textarea></div>
    <div class="form-grid">
      <div class="field"><label for="opensAt">접수 시작 (한국 시간)</label><input class="krds-input" id="opensAt"
          name="opensAt" type="datetime-local" value="${program.opensAt}" step="any" required></div>
      <div class="field"><label for="closesAt">접수 종료 (한국 시간)</label><input class="krds-input" id="closesAt"
          name="closesAt" type="datetime-local" value="${program.closesAt}" step="any" required></div>
      <div class="field"><label for="maxAmount">지원 한도 (원)</label><input class="krds-input" id="maxAmount"
          name="maxAmount" type="number" max="10000000" min="1" value="${program.maxAmount}" required></div>
      <div class="field"><label for="programStatus">게시 상태</label><select class="krds-form-select"
          id="programStatus" name="status">
          <c:forEach var="state" items="${['DRAFT','PUBLISHED','CLOSED']}">
            <option value="${state}" ${program.status==state?'selected':''}>
              ${state=='DRAFT'?'초안':state=='PUBLISHED'?'게시':'종료'}</option>
          </c:forEach>
        </select></div>
    </div><button class="krds-btn medium primary">공고 저장</button>
  </form>
</t:layout>
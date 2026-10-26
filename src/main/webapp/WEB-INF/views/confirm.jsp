<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="제출 전 확인">
  <div class="page-heading">
    <h1>마지막으로 한 번 더 확인해 주세요.</h1>
    <p>제출 후에는 일반 수정이 제한되며, 담당자가 요청한 항목만 보완할 수 있습니다.</p>
  </div>
  <ol class="steps">
    <li>1 신청 정보</li>
    <li>2 서류 첨부</li>
    <li>3 서류 확인</li>
    <li class="active">4 최종 제출</li>
  </ol>
  <section class="content-panel">
    <h2>신청 내용</h2>
    <dl class="definition-list">
      <dt>상호 / 대표자</dt>
      <dd>
        <c:out value="${application.businessName}" /> /
        <c:out value="${application.representative}" />
      </dd>
      <dt>신청금액</dt>
      <dd>
        <fmt:formatNumber value="${application.requestedAmount}" />원
      </dd>
      <dt>사업자등록번호</dt><dd><c:out value="${application.businessNo}" default="미입력" /></dd>
      <dt>개업일</dt><dd><c:out value="${application.openedOn}" default="미입력" /></dd>
      <dt>연락처</dt><dd><c:out value="${application.phone}" default="미입력" /></dd>
      <dt>사업장 주소</dt><dd><c:out value="${application.address}" default="미입력" /></dd>
      <dt>업태</dt><dd><c:out value="${application.businessType}" default="미입력" /></dd>
      <dt>종목</dt><dd><c:out value="${application.businessItem}" default="미입력" /></dd>
      <dt>자부담 (원)</dt><dd><c:out value="${application.selfContribution}" default="미입력" /></dd>
      <dt>총사업비 (원)</dt><dd><c:out value="${application.totalProjectCost}" default="미입력" /></dd>
      <dt>개선 계획</dt>
      <dd>
        <c:out value="${application.planText}" />
      </dd>
      <dt>첨부 서류</dt>
      <dd>
        <c:forEach var="document" items="${documents}">${document.typeLabel} (v${document.version})<br>
        </c:forEach>
        <c:if test="${documents.size()<2}"><strong class="error-text">필수 서류가 누락되었습니다.</strong></c:if>
      </dd>
    </dl>
  </section>
  <c:if test="${not empty sourceIntakeId}"><section class="content-panel"><h2>문서에서 구성한 신청 자료</h2><p><a href="${base}/applications/${application.id}/intake?intakeId=${sourceIntakeId}">원본·분할 문서·추출 내용 확인 →</a></p><div class="application-photo-list"><c:forEach var="page" items="${intakeAnalysis.pages}"><c:forEach var="photo" items="${page.photos}"><c:if test="${photo.selected}"><figure><img src="${base}/intakes/${sourceIntakeId}/assets/${photo.storageKey}" alt="신청에 포함한 원본 ${page.pageNumber}쪽 사진"><figcaption>원본 ${page.pageNumber}쪽</figcaption></figure></c:if></c:forEach></c:forEach></div></section></c:if>
  <form method="post" action="${base}/applications/${application.id}/submit" class="content-panel">
    <t:csrf /><input type="hidden" name="version" value="${application.version}"><input type="hidden"
      name="submissionKey" value="${submissionKey}">
    <c:if test="${not empty supplement}">
      <div class="field"><label for="responseText">보완 답변</label><textarea class="krds-input" id="responseText"
          name="responseText" rows="3" maxlength="2000" required></textarea></div>
    </c:if><label class="checkbox-label"><input type="checkbox" name="acknowledged" value="true" required> 신청
      내용과 첨부 서류를 확인했습니다.</label>
    <div class="actions"><a class="krds-btn medium secondary"
        href="${base}/applications/${application.id}/${not empty sourceIntakeId?'intake':'edit'}${not empty sourceIntakeId?'?intakeId=':''}${sourceIntakeId}${not empty sourceIntakeId?'#edit':''}">신청 내용 수정</a><button
        class="krds-btn medium primary" data-tour="확인한 신청서를 제출하세요. 접수번호가 발급됩니다.">${not empty supplement?'보완 재제출':'신청 제출'}</button></div>
  </form>
</t:layout>
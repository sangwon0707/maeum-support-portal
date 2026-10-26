<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="서류 첨부">
  <div class="page-heading">
    <h1>필요한 서류를 준비해 주세요.</h1>
    <p>사업자등록증과 견적서가 필요합니다. 이전 제출본은 교체 후에도 보존돼요.</p>
  </div>
  <ol class="steps">
    <li>1 신청 정보</li>
    <li class="active">2 서류 첨부</li>
    <li>3 서류 확인</li>
    <li>4 최종 제출</li>
  </ol>
  <div class="document-grid">
    <c:forEach var="type" items="${['BUSINESS_CERT','ESTIMATE']}">
      <section class="content-panel" data-document-card="${type}">
        <h2>${type=='BUSINESS_CERT'?'사업자등록증':'견적서'} <span class="required">필수</span></h2>
        <c:set var="found" value="false" />
        <c:forEach var="document" items="${documents}">
          <c:if test="${document.typeCode==type}">
            <c:set var="found" value="true" /><a href="${base}/documents/${document.id}/file" target="_blank"
              rel="noopener"><img class="document-thumb" src="${base}/documents/${document.id}/file"
                alt="${document.typeLabel} 가상 서류 미리보기"></a>
            <p>
              <c:out value="${document.originalName}" /> · 버전 ${document.version}
            </p>
          </c:if>
        </c:forEach>
        <c:if test="${!found}">
          <div class="empty-document">아직 첨부하지 않았어요.</div>
        </c:if>
        <span hidden data-document-state="${type}" data-attached="${found}" data-replacement-pending="${pendingReplacementTypes.contains(type)}"></span>
        <c:if test="${editableTypes.contains(type)}">
          <c:if test="${actor.demo}">
          <form method="post" action="${base}/applications/${application.id}/documents/fixture"
            class="inline-form">
            <t:csrf /><input type="hidden" name="type" value="${type}">
            <label for="dataset-${type}">예시 서류</label>
            <select id="dataset-${type}" name="dataset" class="krds-input">
              <option value="original">기본 예시 · 가상상점</option>
              <option value="comparison">비교 예시 · 다른 입력값</option>
            </select>
            <button class="krds-btn medium secondary">가상 서류 선택·교체</button>
          </form>
          </c:if>
          <c:if test="${!actor.demo}">
            <c:if test="${not empty sampleOptions}">
              <button type="button" class="krds-btn medium primary sample-open-button" data-sample-open="${type}">
                <span class="portal-icon icon-file" aria-hidden="true"></span>${type=='BUSINESS_CERT'?'사업자등록증':'견적서'} 예시 선택
              </button>
              <p class="field-help">파일이 없어도 준비된 가상 문서를 골라 첨부할 수 있어요.</p>
            </c:if>
            <details class="direct-upload"><summary>내 파일 직접 업로드</summary>
            <form method="post" action="${base}/applications/${application.id}/documents/upload"
              enctype="multipart/form-data" class="upload-form">
              <t:csrf /><input type="hidden" name="type" value="${type}"><label for="file-${type}">${type=='BUSINESS_CERT'?'사업자등록증':'견적서'} 파일 선택</label><input id="file-${type}" type="file" name="file" accept="image/png,image/jpeg"
                required><button class="krds-btn medium primary">선택한 파일 업로드</button>
            </form>
            </details>
          </c:if>
        </c:if>
        <c:if test="${not empty sampleOptions}"><details class="example-help"><summary>이 사업의 예시 서류 내려받기</summary>
          <p>예시 이미지를 내려받은 뒤 위에서 파일을 선택해 업로드해 주세요.</p>
          <c:forEach var="sampleOption" items="${sampleOptions}"><p><a href="${base}/samples/${sampleOption.code}/${type=='BUSINESS_CERT'?'business-cert':'estimate'}"><c:out value="${sampleOption.label}" /> 내려받기</a></p></c:forEach>
        </details></c:if>
      </section>
    </c:forEach>
  </div>
  <c:if test="${not actor.demo && not empty sampleOptions && not empty editableTypes}">
    <t:sample-picker options="${sampleOptions}" editableTypes="${editableTypes}" />
  </c:if>
  <div class="alert">PNG·JPEG, 각 10MiB·1천만 픽셀 이하. 업로드 후 파일명과 미리보기를 확인해 주세요. 서류를 교체해도 이전 제출본은 유지됩니다. 이 포트폴리오에는 가상 정보로 만든 문서를 사용해 주세요.</div>
  <p>다음 단계에서 사업자등록증의 문자를 읽고 신청 정보와 대조합니다. 견적서는 담당자가 개선 항목과 사업비를 검토합니다.</p>
  <div class="actions"><a class="krds-btn medium secondary"
      href="${base}/applications/${application.id}/edit">← 신청 정보</a><a class="krds-btn medium primary"
      href="${base}/applications/${application.id}/ocr" data-tour="두 서류를 첨부했다면 원본과 인식 결과를 확인하세요.">서류 내용 확인 →</a></div>
</t:layout>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="신청서 작성">
  <div class="page-heading">
    <p class="eyebrow">
      <c:out value="${program.title}" />
    </p>
    <h1>${not empty supplement?'요청받은 내용을 보완해 주세요.':'신청 정보를 입력해 주세요.'}</h1>
    <p>작성 중인 내용은 임시저장할 수 있습니다.</p>
  </div>
  <p><a href="${base}/applications/${application.id}/intake">작성한 신청서·계획서·사진을 업로드해 내용 불러오기 →</a></p>
  <ol class="steps">
    <li class="active">1 신청 정보</li>
    <li>2 서류 첨부</li>
    <li>3 서류 확인</li>
    <li>4 최종 제출</li>
  </ol>
  <c:if test="${not empty supplement}">
    <div class="alert"><strong>보완 요청</strong>
      <p>
        <c:out value="${supplement.reason}" />
      </p>
      <p>보완 기한: ${supplement.dueAtLabel} (한국 시간)</p>
      <p>수정 가능 항목:
        <c:out value="${allowedTargets}" />
      </p>
    </div>
  </c:if>
  <c:if test="${not empty validationError}">
    <div class="alert error" role="alert" tabindex="-1" data-error-summary>
      <c:out value="${validationError}" />
    </div>
  </c:if>
  <c:if test="${empty supplement && not empty sampleOptions}">
    <details class="content-panel example-help"><summary>작성 예시가 필요하신가요?</summary>
      <p>빈 신청서에 직접 입력하거나 아래 예시 정보를 불러와 수정할 수 있습니다. 예시를 불러오면 현재 저장된 입력값을 바꿉니다. 서류는 별도로 첨부해야 합니다.</p>
      <form method="post" action="${base}/applications/${application.id}/example" data-confirm="현재 저장된 신청 정보를 선택한 예시로 바꿀까요?">
        <t:csrf /><input type="hidden" name="version" value="${application.version}">
        <label for="application-example">예시 정보</label><select class="krds-form-select" id="application-example" name="dataset"><c:forEach var="sampleOption" items="${sampleOptions}"><option value="${sampleOption.code}"><c:out value="${sampleOption.label}" /></option></c:forEach></select>
        <button class="krds-btn small secondary">예시 정보 불러오기</button>
      </form>
    </details>
  </c:if>
  <form method="post" action="${base}/applications/${application.id}/save" class="content-panel">
    <t:csrf /><input type="hidden" name="version" value="${application.version}">
    <h2>사업자 정보</h2><p class="field-help">사업자등록증에 기재된 정보를 입력해 주세요. 필수 항목은 최종 제출 전에 모두 작성해야 합니다.</p>
    <div class="form-grid">
      <div class="field"><label for="businessName">상호 <span class="required">필수</span></label><input
          class="krds-input" id="businessName" name="businessName" type="text"
          value="<c:out value='${form.businessName}'/>" maxlength="100" ${not empty supplement &&
          !allowedTargets.contains('businessName')?'readonly':''}></div>
      <div class="field"><label for="businessNo">사업자등록번호 (숫자 10자리) <span
            class="required">필수</span></label><input class="krds-input" id="businessNo" name="businessNo"
          type="text" value="<c:out value='${form.businessNo}'/>" maxlength="10" ${not empty supplement &&
          !allowedTargets.contains('businessNo')?'readonly':''}></div>
      <div class="field"><label for="representative">대표자 <span class="required">필수</span></label><input
          class="krds-input" id="representative" name="representative" type="text"
          value="<c:out value='${form.representative}'/>" maxlength="50" ${not empty supplement &&
          !allowedTargets.contains('representative')?'readonly':''}></div>
      <div class="field"><label for="openedOn">개업일 <span class="required">필수</span></label><input
          class="krds-input" id="openedOn" name="openedOn" type="date"
          value="<c:out value='${form.openedOn}'/>" ${not empty supplement &&
          !allowedTargets.contains('openedOn')?'readonly':''}></div>
      <div class="field"><label for="phone">연락처 <span class="required">필수</span></label><input
          class="krds-input" id="phone" name="phone" type="text" value="<c:out value='${form.phone}'/>"
          maxlength="20" ${not empty supplement && !allowedTargets.contains('phone')?'readonly':''}></div>

    </div>
    <h2>사업장과 개선 계획</h2>
    <div class="field"><label for="address">사업장 주소 <span class="required">필수</span></label><input class="krds-input" id="address" name="address" value="<c:out value='${form.address}'/>" maxlength="300" placeholder="예: 가상시 예시구 도움로 123, 1층" ${not empty supplement && !allowedTargets.contains('address')?'readonly':''}></div>
    <div class="field"><label for="businessType">업태 <span class="required">필수</span></label><input class="krds-input" id="businessType" name="businessType" value="<c:out value='${form.businessType}'/>" maxlength="100" placeholder="예: 도매 및 소매업" ${not empty supplement && !allowedTargets.contains('businessType')?'readonly':''}></div>
    <div class="field"><label for="businessItem">종목 <span class="required">필수</span></label><input class="krds-input" id="businessItem" name="businessItem" value="<c:out value='${form.businessItem}'/>" maxlength="100" placeholder="예: 생활잡화 소매" ${not empty supplement && !allowedTargets.contains('businessItem')?'readonly':''}></div>
    <div class="field"><label for="planText">개선 계획 <span class="required">필수</span></label><textarea
        class="krds-input" id="planText" name="planText" rows="5" maxlength="2000" ${not empty supplement &&
        !allowedTargets.contains('planText')?'readonly':''}><c:out value="${form.planText}"/></textarea>
      <p class="field-help">개선할 항목과 필요한 이유를 2,000자 이내로 작성해 주세요.</p>
    </div>
    <h2>사업비 계획</h2>
    <p class="field-help">신청금액은 지원받으려는 금액입니다. 견적서 총액과 대조할 수 있도록 자부담을 함께 입력해 주세요.</p>
    <div class="form-grid">
      <div class="field"><label for="requestedAmount">신청금액 (원) <span class="required">필수</span></label><input
          class="krds-input" id="requestedAmount" name="requestedAmount" type="number" min="1" max="${program.maxAmount}" step="1"
          value="<c:out value='${form.requestedAmount}'/>" ${not empty supplement &&
          !allowedTargets.contains('requestedAmount')?'readonly':''}></div>
      <div class="field"><label for="selfContribution">자부담 (원) <span class="required">필수</span></label><input class="krds-input" id="selfContribution" name="selfContribution" type="number" min="0" max="100000000" step="1" value="<c:out value='${form.selfContribution}'/>" ${not empty supplement && !allowedTargets.contains('selfContribution')?'readonly':''}><p class="field-help">자부담이 없으면 0을 입력해 주세요.</p></div>
      <div class="field"><span>총사업비 (신청금액 + 자부담)</span><output id="total-project-cost" for="requestedAmount selfContribution" aria-label="총사업비" aria-live="polite" class="cost-output">입력 후 계산됩니다.</output></div>
    </div>
    <div class="actions"><button class="krds-btn medium secondary" name="next"
        value="stay">임시저장</button><button class="krds-btn medium primary" name="next" value="documents" data-tour="입력을 마쳤다면 저장하고 서류 첨부로 이동하세요.">저장하고
        서류 첨부 →</button></div>
  </form>
</t:layout>
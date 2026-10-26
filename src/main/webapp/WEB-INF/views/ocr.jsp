<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="서류 내용 확인">
  <link rel="stylesheet" href="${base}/assets/ocr-experience.css?v=2">
  <script src="${base}/assets/ocr-experience.js?v=2" defer></script>
  <span hidden data-portfolio-ocr data-status="${job.status}" data-confirmed="${not empty confirmed}"></span>
  <div class="page-heading">
    <h1>서류와 신청 내용을 대조해 주세요.</h1>
    <p>추출값을 그대로 믿기보다 원본을 보고 직접 확인해 주세요.</p>
  </div>
  <ol class="steps">
    <li>1 신청 정보</li>
    <li>2 서류 첨부</li>
    <li class="active">3 서류 확인</li>
    <li>4 최종 제출</li>
  </ol>
  <div class="alert">문자 인식 결과는 확인을 돕는 후보입니다. 서류 진위나 지원 자격을 판정하지 않습니다.
    문자 인식은 첨부한 사업자등록증에서 상호·번호·대표자·개업일을 읽습니다. 신청서와 다른 값은 직접 확인해 주세요.</div>
  <c:choose>
    <c:when test="${empty certificate}">
      <div class="empty-state">사업자등록증을 먼저 첨부해 주세요.</div><a class="krds-btn medium secondary"
        href="${base}/applications/${application.id}/documents">서류 첨부하기</a>
    </c:when>
    <c:otherwise>
      <section class="ocr-progress-panel" data-ocr-experience data-job="${job.id}" data-document="${certificate.id}" data-state="${job.status}" data-mode="${job.providerMode}" aria-label="문자 인식 진행 상태">
        <div class="ocr-progress-symbol" aria-hidden="true">▤</div>
        <div role="status" aria-live="polite"><span class="ocr-progress-eyebrow">서류 읽기 · 항목 정리</span><h2 data-ocr-title>인식할 서류가 준비됐어요</h2><p data-ocr-description>인식을 요청하면 원본에서 사업자 정보를 읽어 검토표로 정리합니다.</p></div>
        <span class="ocr-progress-badge" data-ocr-badge>요청 전</span>
      </section>
      <div class="review-layout">
        <div class="content-panel">
          <h2>원본 서류</h2>
          <div class="ocr-document-stage" data-ocr-stage data-image-width="${recognition.imageWidth}" data-image-height="${recognition.imageHeight}">
            <img class="document-preview" src="${base}/documents/${certificate.id}/file" alt="테스트용 가상 사업자등록증">
            <svg class="ocr-evidence-overlay" data-ocr-overlay aria-hidden="true" preserveAspectRatio="none"></svg>
          </div>
        </div>
        <div class="content-panel">
          <h2>추출 결과와 확인값</h2>
          <c:if test="${(application.status=='DRAFT' || application.status=='NEEDS_SUPPLEMENT') && (actor.demo || liveEnabled)}">
            <form method="post" action="${base}/documents/${certificate.id}/ocr">
              <t:csrf />
              <div class="field"><label for="ocr-mode">인식 방식</label>
                <select id="ocr-mode" name="mode" class="krds-input">
                  <c:if test="${actor.demo}"><option value="SAMPLE">샘플 · 고정 응답 체험</option></c:if>
                  <c:if test="${liveEnabled}"><option value="LIVE" ${empty job || job.providerMode=='LIVE'?'selected':''}>문자 자동 인식</option></c:if>
                  <c:if test="${aiEnabled}"><option value="LIVE_AI" ${job.providerMode=='LIVE_AI'?'selected':''}>문자 인식 + AI 항목 대조</option></c:if>
                </select>
              </div>
              <c:if test="${aiEnabled}"><p class="field-help">AI 보조를 선택하면 OCR 원문을 외부 AI 서비스로 전송합니다.
                문서 이미지·신청값은 전송하지 않습니다. 민감정보가 없는 가상 문서로 이용해 주세요. 자동 대체 공급자는 사용하지 않습니다.</p></c:if>
              <button class="krds-btn medium primary">선택한 방식으로 인식 요청</button>
              <p class="field-help">같은 서류·같은 방식의 중복 요청은 재실행하지 않습니다. 재시도는 서류를 다시 첨부해 주세요.</p>
            </form>
          </c:if>
          <c:if test="${not empty job}"><p>현재 결과: <strong><c:out value="${job.providerMode=='SAMPLE'?'샘플 고정 응답':job.providerMode=='LIVE_AI'?'실제 OCR + AI 보조':'실제 OCR'}" /></strong></p></c:if>
          <c:if test="${!liveEnabled && !actor.demo}"><p class="alert">현재 문자 인식 서버가 연결되지 않았습니다. 원본을 보고 아래 확인값을 직접 입력할 수 있습니다.</p></c:if>
          <c:choose>
            <c:when test="${empty job}">
              <p>아직 서류를 읽지 않았습니다. 위에서 인식 방식을 선택하고 요청해 주세요.</p>

            </c:when>
            <c:when test="${job.status=='PENDING' || job.status=='RUNNING'}">
              <p role="status" data-ocr-pending>문자를 인식하고 있습니다. AI 보조를 포함하면 최대 3분이 걸릴 수 있습니다.</p><a
                href="${base}/applications/${application.id}/ocr" class="krds-btn small secondary">상태 새로고침</a>
            </c:when>
            <c:when test="${job.status=='FAILED'}">
              <div class="alert error" role="status">서류를 읽지 못했습니다. 원본을 보고 직접 입력할 수 있어요. 오류: <c:out value="${job.errorCode}" /></div>
            </c:when>
            <c:otherwise><span class="status">추출 완료 · 원본 확인 필요</span></c:otherwise>
          </c:choose>
          <div class="table-wrap">
            <table>
              <caption>신청 정보와 OCR 추출값 비교</caption>
              <thead>
                <tr>
                  <th>항목</th>
                  <th>신청값</th>
                  <th>추출값</th>
                  <th>사용자 확인값</th>
                </tr>
              </thead>
              <tbody>
                <c:forEach var="field" items="${['businessName','businessNo','representative','openedOn']}">
                  <tr data-ocr-field="${field}" data-review-required="${evidence[field].reviewRequired}" data-polygon="<c:out value='${evidence[field].polygon}'/>">
                    <th>
                      ${field=='businessName'?'상호':field=='businessNo'?'사업자번호':field=='representative'?'대표자':'개업일'}
                    </th>
                    <td>
                      <span data-ocr-application><c:out value="${application[field]}" /></span>
                    </td>
                    <td>
                      <span data-ocr-extracted data-value="<c:out value='${extracted[field]}'/>"><c:out value="${extracted[field]}" default="미추출" /></span>
                      <span class="ocr-match-badge" data-ocr-match hidden></span>
                      <c:if test="${not empty evidence[field]}">
                        <small class="field-help ocr-field-evidence"><c:out value="${evidence[field].source=='AI'?'AI 보조 후보':'규칙 추출'}" /> · 원본 대조 필요</small>
                        <c:if test="${evidence[field].reviewRequired}"><span class="mismatch">규칙·AI 후보 충돌</span></c:if>
                      </c:if>
                      <c:if test="${not empty extracted[field] && extracted[field] != application[field]}">
                        <span class="mismatch" data-ocr-legacy-mismatch>확인 필요</span></c:if>
                    </td>
                    <td><c:out value="${confirmed[field]}" default="미확인" /></td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
          <c:if test="${not empty recognition && job.providerMode!='SAMPLE'}">
            <p class="field-help ocr-metadata">인식 모델: <c:out value="${recognition.modelVersion}" /> · <c:out value="${recognition.durationMs}" />ms</p>
            <c:if test="${job.providerMode=='LIVE_AI'}"><p>AI 보조 상태: <c:out value="${recognition.ai.status}" /> ·
              <c:out value="${recognition.ai.provider}" /> / <c:out value="${recognition.ai.model}" /></p>
              <p class="field-help">AI가 실패해도 로컬 OCR 결과는 유지됩니다. 후보가 비어 있으면 직접 확인해 주세요.</p></c:if>
            <details><summary>인식 원문과 근거 위치 보기</summary>
              <ol><c:forEach var="line" items="${recognition.texts}"><li><c:out value="${line.text}" />
                <small>영역 <c:out value="${line.polygon}" /> · 인식 점수 <c:out value="${line.confidence}" /></small></li></c:forEach></ol>
              <p class="field-help">인식 점수는 모델 출력이며 항목 정확도나 서류 진위를 뜻하지 않습니다.</p>
            </details>
          </c:if>
          <c:if test="${application.status=='DRAFT' || application.status=='NEEDS_SUPPLEMENT'}">
            <form method="post" action="${base}/documents/${certificate.id}/confirm">
              <t:csrf />
              <c:forEach var="field" items="${['businessName','businessNo','representative','openedOn']}">
                <div class="field"><label for="confirm-${field}">확인한
                    ${field=='businessName'?'상호':field=='businessNo'?'사업자번호':field=='representative'?'대표자':'개업일'}</label><input
                    class="krds-input" id="confirm-${field}" name="${field}"
                    value="<c:out value='${not empty confirmed[field]?confirmed[field]:not empty extracted[field]?extracted[field]:application[field]}'/>"
                    required></div>
              </c:forEach><button class="krds-btn medium secondary">확인값 저장</button>
            </form>
            <c:if test="${not empty confirmed}">
              <form method="post" action="${base}/documents/${certificate.id}/apply-confirmed" data-confirm="저장한 확인값으로 신청서의 상호·번호·대표자·개업일을 바꿀까요?">
                <t:csrf /><input type="hidden" name="version" value="${application.version}"><button class="krds-btn medium primary">확인한 사업자 정보를 신청서에 반영</button>
              </form>
            </c:if>
            <p class="field-help">확인값은 추출 결과와 별도로 저장합니다. 신청서 내용은 신청 정보 화면에서 수정할 수 있어요.</p>
          </c:if>
        </div>
      </div>
      <div class="actions"><a class="krds-btn medium secondary"
          href="${base}/applications/${application.id}/documents">← 첨부 서류</a><a
          class="krds-btn medium primary" data-tour="확인값을 저장했다면 최종 제출 내용을 확인하세요." href="${base}/applications/${application.id}/confirm">최종 확인으로 →</a>
      </div>
    </c:otherwise>
  </c:choose>
</t:layout>
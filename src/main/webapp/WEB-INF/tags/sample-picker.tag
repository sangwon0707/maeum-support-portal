<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="options" required="true" type="java.util.List" %>
<%@ attribute name="editableTypes" required="true" type="java.util.List" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<dialog class="sample-picker" id="sample-picker" aria-labelledby="picker-title" aria-describedby="picker-description">
  <div class="picker-shell">
    <header class="picker-header">
      <div class="picker-title"><span class="picker-folder-icon" aria-hidden="true"></span><div><span>마음이음 예시 보관함</span><h2 id="picker-title">예시 파일 선택</h2></div></div>
      <button type="button" class="picker-close" data-picker-close aria-label="예시 파일 선택창 닫기">×</button>
    </header>
    <p id="picker-description" class="picker-description">포트폴리오용 가상 문서입니다. 파일을 고르면 오른쪽에서 내용을 확인할 수 있어요.</p>
    <div class="picker-address"><span aria-hidden="true">⌂</span><span>마음이음</span><span aria-hidden="true">›</span><span>신청 예시</span><span aria-hidden="true">›</span><strong data-picker-folder-label>사업자등록증</strong><span class="picker-file-format">PNG 이미지</span></div>
    <div class="picker-body">
      <nav class="picker-sidebar" aria-label="예시 문서 폴더">
        <p>신청 서류</p>
        <c:forEach var="type" items="${['BUSINESS_CERT','ESTIMATE']}">
          <button type="button" data-picker-folder="${type}" ${!editableTypes.contains(type)?'disabled':''}>
            <span class="picker-folder-icon" aria-hidden="true"></span>${type=='BUSINESS_CERT'?'사업자등록증':'견적서'}
          </button>
        </c:forEach>
        <small>이 사업에 맞는 예시만<br>준비되어 있어요.</small>
        <button type="button" class="picker-guide-toggle" data-portfolio-guide-toggle aria-pressed="true">안내 끄기</button>
      </nav>
      <section class="picker-files" aria-labelledby="picker-files-heading">
        <div class="picker-list-heading"><h3 id="picker-files-heading">파일 목록</h3><span>클릭해서 선택 · 더블클릭해서 첨부</span></div>
        <div class="picker-file-grid">
          <c:forEach var="option" items="${options}">
            <c:forEach var="type" items="${['BUSINESS_CERT','ESTIMATE']}">
              <c:if test="${editableTypes.contains(type)}">
                <button type="button" class="picker-file" data-picker-file data-kind="${type}"
                  data-url="${base}/samples/${option.code}/${type=='BUSINESS_CERT'?'business-cert':'estimate'}"
                  data-filename="${option.code}_${type=='BUSINESS_CERT'?'사업자등록증':'견적서'}.png" aria-pressed="false" hidden>
                  <span class="picker-paper" aria-hidden="true"><span>PNG</span><i></i><i></i><i></i><i></i></span>
                  <strong>${type=='BUSINESS_CERT'?'사업자등록증':'견적서'}.png</strong>
                  <small><c:out value="${option.label}" /></small>
                </button>
              </c:if>
            </c:forEach>
          </c:forEach>
        </div>
        <p class="picker-file-count" data-picker-count></p>
      </section>
      <aside class="picker-preview" aria-labelledby="picker-preview-heading">
        <h3 id="picker-preview-heading">미리보기</h3>
        <div class="picker-preview-empty" data-picker-empty><span class="portal-icon icon-file" aria-hidden="true"></span><p>파일을 선택하면<br>문서를 미리 볼 수 있어요.</p></div>
        <img data-picker-preview alt="선택한 가상 문서 미리보기" hidden>
        <p data-picker-preview-caption></p>
      </aside>
    </div>
    <footer class="picker-footer">
      <div class="picker-selected-name"><span>파일 이름</span><output data-picker-selected>선택한 파일 없음</output></div>
      <p class="picker-feedback" data-picker-feedback role="status" aria-live="polite"></p>
      <div class="picker-actions"><button type="button" class="krds-btn medium secondary" data-picker-close>취소</button><button type="button" class="krds-btn medium primary" data-picker-attach disabled>선택해서 첨부</button></div>
    </footer>
  </div>
</dialog>

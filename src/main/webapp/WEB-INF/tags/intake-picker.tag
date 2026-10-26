<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="options" required="true" type="java.util.List" %>
<%@ attribute name="action" required="true" type="java.lang.String" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<dialog class="sample-picker intake-file-picker" data-intake-samples data-program-upload-dialog aria-labelledby="intake-picker-title">
  <form method="post" action="${action}" enctype="multipart/form-data" data-intake-upload class="picker-shell">
    <t:csrf /><input type="hidden" name="layout" value="BUNDLE">
    <header class="picker-header">
      <div class="picker-title"><span class="picker-folder-icon" aria-hidden="true"></span><div><span>마음이음 · 신청 서류</span><h2 id="intake-picker-title">업로드할 파일 선택</h2></div></div>
      <button type="button" class="picker-close" data-intake-sample-close aria-label="파일 선택창 닫기">×</button>
    </header>
    <p class="picker-description">파일을 선택하면 오른쪽에서 미리 볼 수 있습니다. 업로드 후 다음 화면에서 서류를 확인하고 문서 검토를 요청해 주세요.</p>
    <div class="picker-address"><span aria-hidden="true">⌂</span><span>마음이음</span><span aria-hidden="true">›</span><span>신청 서류</span><span aria-hidden="true">›</span><strong>통합 신청서</strong><span class="picker-file-format">PDF 파일 · 최대 10MiB</span></div>
    <div class="picker-body">
      <nav class="picker-sidebar" aria-label="파일 위치">
        <p>파일 위치</p>
        <button type="button" data-entry-mode="sample" aria-pressed="true"><span class="picker-folder-icon" aria-hidden="true"></span>신청 서류함</button>
        <button type="button" data-entry-mode="file" aria-pressed="false"><span class="picker-folder-icon" aria-hidden="true"></span>내 컴퓨터</button>
        <small>신청서·계획서·증빙·사진을<br>묶은 PDF 한 개를 선택하세요.</small>
      </nav>
      <section class="picker-files" aria-label="파일 목록">
        <div class="picker-list-heading"><h3>파일 목록</h3><span>클릭해서 선택</span></div>
        <div data-entry-panel="sample"><div class="picker-file-grid">
          <c:forEach var="option" items="${options}">
            <button type="button" class="picker-file" data-intake-sample-url="${base}/samples/${option.code}/application-scan" data-preview="${base}/assets/samples/${option.code}-scan.jpg" data-filename="<c:out value='${option.label}'/>-스캔본.pdf" aria-pressed="false">
              <span class="picker-paper" aria-hidden="true"><span>PDF</span><i></i><i></i><i></i><i></i></span>
              <strong>신청서류_스캔본.pdf</strong><small><c:out value="${option.label}" /></small>
            </button>
          </c:forEach>
        </div><p class="picker-file-count">PDF 파일 ${options.size()}개</p></div>
        <div data-entry-panel="file" hidden>
          <div class="intake-dropzone"><label for="intake-files">컴퓨터에서 PDF 선택</label><input id="intake-files" type="file" name="files" accept="application/pdf,.pdf" required><p>PDF 한 개 · 10MiB 이하 · 최대 12쪽</p></div>
          <p class="picker-file-count">선택한 원본은 업로드 후 다음 화면에서 확인할 수 있습니다.</p>
        </div>
      </section>
      <aside class="picker-preview" aria-label="선택한 파일 미리보기">
        <h3>미리보기</h3>
        <div class="picker-preview-empty" data-intake-preview-empty><span class="portal-icon icon-file" aria-hidden="true"></span><p>파일을 선택하면<br>문서를 미리 볼 수 있어요.</p></div>
        <img data-intake-selection-preview alt="선택한 신청서 첫 페이지" hidden>
        <p>업로드한 서류는 다음 화면에서 확인한 뒤 검토를 요청할 수 있습니다.</p>
      </aside>
    </div>
    <footer class="picker-footer">
      <div class="picker-selected-name"><span>파일 이름</span><output data-intake-selection>선택한 파일이 없습니다.</output></div>
      <p class="picker-feedback" data-sample-feedback role="status" aria-live="polite"></p>
      <div class="picker-actions"><button type="button" class="krds-btn medium secondary" data-intake-sample-close>취소</button><button class="krds-btn medium primary" data-intake-submit disabled>업로드</button></div>
    </footer>
  </form>
</dialog>

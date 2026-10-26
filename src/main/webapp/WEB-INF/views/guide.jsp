<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="신청·서류 안내">
  <div class="page-heading"><p class="eyebrow">이용 안내</p><h1>신청부터 결과 확인까지</h1><p>마음이음은 지원사업 접수와 서류 검토를 관리하는 서비스입니다.</p></div>
  <nav class="page-toc" aria-label="신청 안내 목차"><a href="#application-process">신청 절차</a><a href="#documents">준비할 서류</a><a href="#examples">예시 서류</a><a href="#after-submit">제출 이후</a></nav>
  <section class="content-panel" id="application-process"><h2>신청자는 이렇게 진행합니다.</h2>
    <ol class="process-list"><li><strong>사업 확인</strong> — 지원 대상·항목·접수 기간을 읽고 신청할 사업을 선택합니다.</li><li><strong>신청서 작성</strong> — 직접 입력하거나 작성한 신청서와 증빙을 묶은 PDF를 업로드합니다.</li><li><strong>원본 확인</strong> — 업로드한 문서를 확인한 뒤 ‘문서 검토 요청’을 누릅니다. 확인 전에는 인식을 시작하지 않습니다.</li><li><strong>내용 대조</strong> — 문서 분할·문자 인식·사진 추출 결과를 원본과 대조하고 수정한 뒤 신청서에 반영합니다. 직접 작성한 경우에는 증빙 이미지를 첨부해 확인합니다.</li><li><strong>제출</strong> — 최종 확인 후 제출하면 접수번호가 발급됩니다.</li><li><strong>보완·결과 확인</strong> — 나의 신청에서 보완 사유·기한과 결과를 확인합니다.</li></ol>
  </section>
  <section class="content-panel" id="documents"><h2>준비할 서류</h2><div class="table-wrap"><table><caption>필수 서류와 확인 목적</caption><thead><tr><th>서류</th><th>확인 내용</th><th>형식</th></tr></thead><tbody><tr><th>사업자등록증</th><td>상호·등록번호·대표자·개업일 대조</td><td>PNG/JPEG, 10MiB 이하</td></tr><tr><th>견적서</th><td>개선 항목·공급가액·부가세·총액 검토</td><td>PNG/JPEG, 10MiB 이하</td></tr></tbody></table></div><p>작성한 신청서·사업계획·증빙·현장 사진을 묶은 PDF도 업로드할 수 있습니다. PDF는 파일당 10MiB·최대 12쪽이며, 검토 요청 후 문서를 종류별로 분할하고 신청 항목과 사진을 추출합니다. 위 PNG/JPEG 형식은 직접 작성 방식의 개별 증빙 첨부에 해당합니다.</p></section>
  <section class="content-panel" id="examples"><h2>서류가 없을 때 사용할 예시</h2><p>공고의 ‘작성 문서 업로드’에서 제공된 신청 PDF를 선택하거나 아래 파일을 내려받아 사용할 수 있습니다. PDF는 스캔된 문서 형태이며, 개별 증빙 이미지는 직접 작성 방식에서 사용할 수 있습니다. 모든 자료에는 가상 정보만 들어 있습니다.</p>
    <div class="table-wrap"><table><caption>사업별로 다른 신청 정보와 견적 내용의 가상 예시</caption><thead><tr><th>예시 사례</th><th>작성 신청서</th><th>사업자등록증</th><th>견적서</th></tr></thead><tbody><c:forEach var="sampleOption" items="${sampleOptions}"><tr><th><c:out value="${sampleOption.label}" /></th><td><a href="${base}/samples/${sampleOption.code}/application-scan">스캔 PDF 내려받기</a></td><td><a href="${base}/samples/${sampleOption.code}/business-cert">PNG 내려받기</a></td><td><a href="${base}/samples/${sampleOption.code}/estimate">PNG 내려받기</a></td></tr></c:forEach></tbody></table></div>
  </section>
  <section class="content-panel" id="after-submit"><h2>제출한 다음에는 어떻게 되나요?</h2><div class="after-submit-grid"><div><span class="guide-label">접수 확인</span><h3>접수번호를 확인하세요</h3><p>최종 제출 후 나의 신청에서 접수번호와 상태를 확인할 수 있습니다. 임시저장 상태는 접수가 완료된 상태가 아닙니다.</p></div><div><span class="guide-label">보완 요청</span><h3>수정할 항목과 기한을 확인하세요</h3><p>담당자가 요청한 항목, 사유와 보완 기한을 확인하고 내용을 수정해 다시 제출하세요.</p></div><div><span class="guide-label">결과 확인</span><h3>검토 결과와 이력을 확인하세요</h3><p>검토가 끝나면 나의 신청에서 결과를 확인할 수 있습니다. 문자·이메일 자동 알림은 현재 제공하지 않습니다.</p></div></div></section>
  <section class="content-panel"><h2>담당자는 이렇게 처리합니다.</h2><p>관리자가 접수된 신청에 담당자를 배정하면, 담당자는 배정 목록에서 신청 정보와 서류를 확인합니다. 보완할 항목·사유·기한을 지정하거나 검토 결과를 확정합니다. 신청자 계정으로 담당자 권한을 전환할 수 없습니다.</p></section>
  <div class="actions"><a class="krds-btn medium primary" href="${base}/programs">지원사업 확인하기</a><a class="krds-btn medium secondary" href="${base}/my">나의 신청 보기</a></div>
</t:layout>

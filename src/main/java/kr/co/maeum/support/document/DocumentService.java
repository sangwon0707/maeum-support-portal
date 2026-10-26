package kr.co.maeum.support.document;

import kr.co.maeum.support.application.ApplicationMapper;
import kr.co.maeum.support.application.ApplicationService;
import kr.co.maeum.support.audit.AuditService;
import kr.co.maeum.support.common.BusinessException;
import kr.co.maeum.support.identity.AccessScope;
import kr.co.maeum.support.identity.PortalPrincipal;
import kr.co.maeum.support.ocr.OcrMapper;
import kr.co.maeum.support.review.ReviewMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
public class DocumentService {
    private final DocumentMapper documents;
    private final ApplicationMapper applications;
    private final ApplicationService workflow;
    private final AccessScope access;
    private final FileStorage storage;
    private final ImageValidator validator;
    private final FixtureImages fixtures;
    private final ReviewMapper reviews;
    private final AuditService audit;
    private final Clock clock;
    private final OcrMapper ocr;

    public DocumentService(
            DocumentMapper documents,
            ApplicationMapper applications,
            ApplicationService workflow,
            AccessScope access,
            FileStorage storage,
            ImageValidator validator,
            FixtureImages fixtures,
            ReviewMapper reviews,
            AuditService audit,
            Clock clock,
            OcrMapper ocr) {
        this.documents = documents;
        this.applications = applications;
        this.workflow = workflow;
        this.access = access;
        this.storage = storage;
        this.validator = validator;
        this.fixtures = fixtures;
        this.reviews = reviews;
        this.audit = audit;
        this.clock = clock;
        this.ocr = ocr;
    }

    /** 화면의 파일 선택 범위를 좁히며, 업로드 시 잠금 안에서 권한·기한을 다시 검사한다. */
    public List<String> editableTypes(PortalPrincipal actor, long id) {
        var application = workflow.view(actor, id);
        if (!"APPLICANT".equals(actor.role()) || actor.id() != application.getOwnerId()) {
            return List.of();
        }
        if ("DRAFT".equals(application.getStatus())) {
            return List.of("BUSINESS_CERT", "ESTIMATE");
        }
        if (!"NEEDS_SUPPLEMENT".equals(application.getStatus())) {
            return List.of();
        }
        var request = reviews.openSupplement(id);
        if (request == null || !LocalDateTime.now(clock).isBefore(request.getDueAt())) {
            return List.of();
        }
        return reviews.targets(request.getId()).stream()
                .filter(type -> List.of("BUSINESS_CERT", "ESTIMATE").contains(type))
                .toList();
    }

    /** 보완 제출본과 현재 파일 버전의 차이를 기준으로 다음 교체 안내를 정한다. */
    public List<String> pendingReplacementTypes(PortalPrincipal actor, long id) {
        if (!"NEEDS_SUPPLEMENT".equals(workflow.view(actor, id).getStatus())) {
            return List.of();
        }
        return editableTypes(actor, id).stream()
                .filter(type -> applications.changedDocument(id, type) == 0)
                .toList();
    }

    @Transactional
    public void attachFixture(PortalPrincipal actor, long id, String type) {
        attachFixture(actor, id, type, "original");
    }

    @Transactional
    public void attachFixture(PortalPrincipal actor, long id, String type, String dataset) {
        if (!List.of("original", "comparison").contains(dataset)) {
            throw new BusinessException(422, "예시 서류 종류를 확인해 주세요.");
        }
        attach(
                actor,
                id,
                type,
                ("BUSINESS_CERT".equals(type) ? "가상_사업자등록증.png" : "가상_견적서.png"),
                dataset.equals("original") && "MISMATCH".equals(scenario(id))
                        ? fixtures.create(type, "가상문구점")
                        : fixtures.prepared(type, dataset));
    }

    private String scenario(long id) {
        return ocr.scenario(id);
    }

    @Transactional
    public void upload(PortalPrincipal actor, long id, String type, String filename, byte[] bytes) {
        if (actor.demo()) {
            throw new BusinessException(403, "공개 체험은 제공된 가상 서류만 선택할 수 있습니다.");
        }
        attach(actor, id, type, filename, bytes);
    }

    private void attach(
            PortalPrincipal actor, long id, String type, String filename, byte[] bytes) {
        var application = applications.lock(id);
        access.owner(actor, application);
        if (!List.of("BUSINESS_CERT", "ESTIMATE").contains(type)) {
            throw new BusinessException(422, "서류 종류를 확인해 주세요.");
        }
        if ("NEEDS_SUPPLEMENT".equals(application.getStatus())) {
            var request = reviews.openSupplement(id);
            if (request == null
                    || !LocalDateTime.now(clock).isBefore(request.getDueAt())
                    || !reviews.targets(request.getId()).contains(type)) {
                throw new BusinessException(422, "기한 내 요청받은 서류만 변경할 수 있습니다.");
            }
        } else if (!"DRAFT".equals(application.getStatus())) {
            throw new BusinessException(409, "제출 후에는 서류를 교체할 수 없습니다.");
        }
        byte[] normalized = validator.normalize(bytes);
        String key = storage.save(normalized);
        // DB가 롤백되면 이번에 만든 파일만 보상 삭제한다. 이전 제출본의 파일은 유지한다.
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            storage.delete(key);
                        }
                    }
                });
        var document = new Document();
        document.setApplicationId(id);
        document.setTypeCode(type);
        int version =
                documents.current(id).stream()
                                .filter(item -> type.equals(item.getTypeCode()))
                                .mapToInt(Document::getVersion)
                                .max()
                                .orElse(0)
                        + 1;
        document.setVersion(version);
        document.setStorageKey(key);
        document.setOriginalName(
                filename == null
                        ? "첨부.png"
                        : filename.substring(0, Math.min(filename.length(), 255)));
        document.setMimeType("image/png");
        document.setSizeBytes((long) normalized.length);
        document.setSha256(hash(normalized));
        document.setCreatedBy(actor.id());
        documents.insert(document);
        workflow.persist(application);
        audit.record(
                actor,
                id,
                "DOCUMENT_ATTACHED",
                "DOCUMENT",
                document.getId(),
                type + " v" + version);
    }

    @Transactional
    public Path authorizedFile(PortalPrincipal actor, long documentId) {
        var document = documents.find(documentId);
        if (document == null) {
            throw new BusinessException(404, "서류를 찾을 수 없습니다.");
        }
        var application = applications.find(document.getApplicationId());
        access.view(actor, application);
        audit.record(
                actor, application.getId(), "DOCUMENT_READ", "DOCUMENT", documentId, "비공개 서류 열람");
        return storage.resolve(document.getStorageKey());
    }

    private String hash(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}

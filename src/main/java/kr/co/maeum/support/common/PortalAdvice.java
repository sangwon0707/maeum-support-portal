package kr.co.maeum.support.common;

import jakarta.servlet.http.HttpServletResponse;

import kr.co.maeum.support.identity.PortalPrincipal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class PortalAdvice {
    private static final Logger log = LoggerFactory.getLogger(PortalAdvice.class);

    @ModelAttribute("actor")
    public PortalPrincipal actor(Authentication authentication) {
        return authentication != null
                        && authentication.getPrincipal() instanceof PortalPrincipal principal
                ? principal
                : null;
    }

    @ExceptionHandler(BusinessException.class)
    public String business(BusinessException exception, HttpServletResponse response, Model model) {
        response.setStatus(exception.getStatus());
        model.addAttribute("errorStatus", exception.getStatus());
        model.addAttribute("errorMessage", exception.getMessage());
        return exception.getStatus() == 404 ? "not-found" : "error";
    }

    @ExceptionHandler({
        MethodArgumentTypeMismatchException.class,
        MaxUploadSizeExceededException.class,
        org.springframework.web.bind.MissingServletRequestParameterException.class,
        org.springframework.web.multipart.support.MissingServletRequestPartException.class,
        org.springframework.validation.BindException.class
    })
    public String input(Exception exception, HttpServletResponse response, Model model) {
        response.setStatus(422);
        model.addAttribute("errorStatus", 422);
        model.addAttribute("errorMessage", "입력 형식 또는 파일 용량을 확인해 주세요.");
        return "error";
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public String unsupportedMethod(HttpServletResponse response, Model model) {
        response.setStatus(405);
        model.addAttribute("errorStatus", 405);
        model.addAttribute("errorMessage", "허용되지 않은 요청 방식입니다. 화면의 버튼을 통해 다시 진행해 주세요.");
        return "error";
    }

    @ExceptionHandler({
        NoResourceFoundException.class,
        org.springframework.web.servlet.NoHandlerFoundException.class
    })
    public String missingResource(HttpServletResponse response, Model model) {
        response.setStatus(404);
        model.addAttribute("errorStatus", 404);
        model.addAttribute("errorMessage", "주소가 변경되었거나 더 이상 제공하지 않는 페이지입니다.");
        return "not-found";
    }

    @ExceptionHandler(Exception.class)
    public String unexpected(Exception exception, HttpServletResponse response, Model model) {
        // MyBatis와 트랜잭션 예외가 감싸는 연결 장애도 입력 오류와 구분한다.
        Throwable cause = exception;
        for (int depth = 0; cause != null && depth < 16; depth++, cause = cause.getCause()) {
            if (cause instanceof org.springframework.dao.DataAccessResourceFailureException
                    || cause
                            instanceof
                            org.springframework.transaction.CannotCreateTransactionException
                    || cause instanceof java.sql.SQLTransientConnectionException
                    || cause instanceof java.sql.SQLNonTransientConnectionException) {
                log.warn("저장소 연결 불가 type={}", cause.getClass().getSimpleName());
                response.setStatus(503);
                model.addAttribute("errorStatus", 503);
                model.addAttribute(
                        "errorMessage", "저장소에 연결할 수 없어 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.");
                return "error";
            }
        }
        log.error("처리 실패 type={}", exception.getClass().getSimpleName(), exception);
        response.setStatus(500);
        model.addAttribute("errorStatus", 500);
        model.addAttribute("errorMessage", "처리 중 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        return "error";
    }
}

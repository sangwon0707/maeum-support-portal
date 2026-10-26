package kr.co.maeum.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import kr.co.maeum.support.common.PortalAdvice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;

class PortalAdviceTest {
    @Test
    @DisplayName("감싸진 DB 연결 장애는 503으로 안내하고 내부 연결 정보를 노출하지 않는다")
    void wrapped_connection_failure_returns_unavailable() {
        var response = new MockHttpServletResponse();
        var model = new ExtendedModelMap();
        var exception =
                new RuntimeException(
                        new java.sql.SQLNonTransientConnectionException(
                                "private-host-and-password"));
        assertEquals("error", new PortalAdvice().unexpected(exception, response, model));
        assertEquals(503, response.getStatus());
        assertEquals(503, model.get("errorStatus"));
        assertFalse(model.get("errorMessage").toString().contains("private-host"));
    }

    @Test
    @DisplayName("SQL 문법 오류를 연결 장애로 숨기지 않는다")
    void sql_bug_remains_server_error() {
        var response = new MockHttpServletResponse();
        var model = new ExtendedModelMap();
        new PortalAdvice()
                .unexpected(new java.sql.SQLSyntaxErrorException("invalid SQL"), response, model);
        assertEquals(500, response.getStatus());
    }

    @Test
    @DisplayName("필수 요청값 누락은 실제 HTTP 바인딩에서 422로 안내한다")
    void missing_request_parameter_returns_input_error() throws Exception {
        var mvc =
                org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                                new BoundaryController())
                        .setControllerAdvice(new PortalAdvice())
                        .build();
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                "/probe"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                                .isUnprocessableEntity());
    }

    @Test
    @DisplayName("잘못된 HTTP 메서드를 서버 오류로 처리하지 않는다")
    void unsupported_method_returns_method_not_allowed() throws Exception {
        var mvc =
                org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                                new BoundaryController())
                        .setControllerAdvice(new PortalAdvice())
                        .build();
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                "/probe"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                                .isMethodNotAllowed());
    }

    @Test
    @DisplayName("없는 페이지는 404 화면을 반환하고 서버 오류로 바꾸지 않는다")
    void missing_page_returns_not_found_view() throws Exception {
        var mvc =
                org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                                new BoundaryController())
                        .setControllerAdvice(new PortalAdvice())
                        .build();
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                "/removed-page"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                                .isNotFound())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.view()
                                .name("not-found"));
        var response = new MockHttpServletResponse();
        assertEquals(
                "not-found",
                new PortalAdvice()
                        .business(
                                new kr.co.maeum.support.common.BusinessException(404, "없는 공고"),
                                response,
                                new ExtendedModelMap()));
        assertEquals(404, response.getStatus());
    }

    @org.springframework.stereotype.Controller
    static class BoundaryController {
        @org.springframework.web.bind.annotation.PostMapping("/probe")
        String submit(
                @org.springframework.web.bind.annotation.RequestParam("submissionKey") String key) {
            return "ok";
        }
    }
}

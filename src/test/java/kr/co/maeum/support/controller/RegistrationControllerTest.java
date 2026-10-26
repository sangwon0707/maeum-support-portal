package kr.co.maeum.support.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RegistrationControllerTest {
    @Test
    @DisplayName("가입 화면은 보여주되 계정 생성 POST 경로는 제공하지 않는다")
    void registration_page_does_not_expose_account_creation() throws Exception {
        var mvc =
                MockMvcBuilders.standaloneSetup(new RegistrationController())
                        .setControllerAdvice(new PortalAdvice())
                        .setViewResolvers(
                                new org.springframework.web.servlet.view
                                        .InternalResourceViewResolver("/WEB-INF/views/", ".jsp"))
                        .build();
        mvc.perform(MockMvcRequestBuilders.get("/register"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("register"));
        mvc.perform(
                        MockMvcRequestBuilders.post("/register")
                                .param("loginId", "must-not-be-created")
                                .param("displayName", "생성 금지")
                                .param("password", "Only-test-password-2026!")
                                .param("passwordConfirmation", "Only-test-password-2026!"))
                .andExpect(MockMvcResultMatchers.status().isMethodNotAllowed());
    }
}

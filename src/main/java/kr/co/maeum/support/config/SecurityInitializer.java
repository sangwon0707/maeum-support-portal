package kr.co.maeum.support.config;

import jakarta.servlet.ServletContext;

import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.web.filter.CharacterEncodingFilter;

public class SecurityInitializer extends AbstractSecurityWebApplicationInitializer {
    @Override
    protected String getDispatcherWebApplicationContextSuffix() {
        return "dispatcher";
    }

    @Override
    protected void beforeSpringSecurityFilterChain(ServletContext servletContext) {
        // CSRF 검사도 폼 파라미터를 읽는다. 보안 필터보다 늦게 설정하면 한글이 이미 깨진다.
        insertFilters(servletContext, new CharacterEncodingFilter("UTF-8", true));
    }
}

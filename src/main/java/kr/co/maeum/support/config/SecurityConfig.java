package kr.co.maeum.support.config;

import kr.co.maeum.support.mapper.IdentityMapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(IdentityMapper identities) {
        return username -> {
            var account = identities.findLogin(username);
            if (account == null) {
                throw new UsernameNotFoundException("계정을 확인해 주세요.");
            }
            return account.principal();
        };
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, SecurityContextRepository repository) throws Exception {
        http.authorizeHttpRequests(
                        auth ->
                                auth.dispatcherTypeMatchers(
                                                jakarta.servlet.DispatcherType.FORWARD,
                                                jakarta.servlet.DispatcherType.ERROR)
                                        .permitAll()
                                        .requestMatchers(
                                                "/",
                                                "/programs",
                                                "/programs/*",
                                                "/login",
                                                "/register",
                                                "/guide",
                                                "/terms",
                                                "/privacy",
                                                "/samples/**",
                                                "/assets/**",
                                                "/health")
                                        .permitAll()
                                        .requestMatchers("/admin/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers("/staff/**")
                                        .hasRole("STAFF")
                                        .requestMatchers(
                                                "/applications/**",
                                                "/documents/**",
                                                "/intakes/**",
                                                "/my",
                                                "/mypage",
                                                "/programs/*/intake",
                                                "/programs/*/intake/active")
                                        .authenticated()
                                        // 존재하지 않는 공개 주소도 로그인 화면 대신 404 안내로 보낸다.
                                        .anyRequest()
                                        .permitAll())
                .securityContext(context -> context.securityContextRepository(repository))
                .formLogin(
                        form -> form.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .headers(
                        headers ->
                                headers.contentSecurityPolicy(
                                        policy ->
                                                policy.policyDirectives(
                                                        "default-src 'self'; img-src 'self' data:;"
                                                            + " style-src 'self'; script-src"
                                                            + " 'self'; font-src 'self';"
                                                            + " frame-ancestors 'none'; base-uri"
                                                            + " 'self'; form-action 'self'")));
        return http.build();
    }
}

package kr.co.maeum.support.config;

import org.egovframe.rte.fdl.cmmn.trace.LeaveaTrace;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.time.Clock;

@Configuration
@EnableWebMvc
@EnableScheduling
@ComponentScan("kr.co.maeum.support")
public class WebConfig implements WebMvcConfigurer {
    @Bean
    public MessageSource messageSource() {
        return new StaticMessageSource();
    }

    @Bean
    public LeaveaTrace leaveaTrace() {
        return new LeaveaTrace();
    }

    @Bean
    public org.springframework.web.multipart.support.StandardServletMultipartResolver
            multipartResolver() {
        return new org.springframework.web.multipart.support.StandardServletMultipartResolver();
    }

    @Bean
    public org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler taskScheduler() {
        var scheduler = new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("portal-job-");
        scheduler.setWaitForTasksToCompleteOnShutdown(false);
        return scheduler;
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public InternalResourceViewResolver viewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/WEB-INF/views/");
        resolver.setSuffix(".jsp");
        return resolver;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**").addResourceLocations("/assets/");
    }
}

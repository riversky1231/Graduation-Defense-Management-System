package com.example.defensemanagement.config;

import com.example.defensemanagement.interceptor.AdminAuditInterceptor;
import com.example.defensemanagement.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Autowired
    private AdminAuditInterceptor adminAuditInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/captcha", "/image.png", "/css/**", "/js/**", "/images/**", "/error", "/health");

        registry.addInterceptor(adminAuditInterceptor)
                .addPathPatterns("/admin/**");
    }
}

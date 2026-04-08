package com.example.defensemanagement.config;

import com.example.defensemanagement.security.SessionAuthenticationEntryPoint;
import com.example.defensemanagement.security.SessionAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SessionAuthenticationFilter sessionAuthenticationFilter;
    private final SessionAuthenticationEntryPoint sessionAuthenticationEntryPoint;
    private final Environment environment;

    public SecurityConfig(SessionAuthenticationFilter sessionAuthenticationFilter,
                          SessionAuthenticationEntryPoint sessionAuthenticationEntryPoint,
                          Environment environment) {
        this.sessionAuthenticationFilter = sessionAuthenticationFilter;
        this.sessionAuthenticationEntryPoint = sessionAuthenticationEntryPoint;
        this.environment = environment;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF：禁用 Spring Security 内置 CSRF，依赖 SameSite Cookie + X-Requested-With Header 防护
                .csrf().disable()
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation().migrateSession())
                .headers(headers -> {
                    headers.frameOptions().sameOrigin();
                    headers.contentTypeOptions();
                    headers.xssProtection().block(true);
                    // 防止 MIME 类型嗅探
                    headers.contentTypeOptions();
                    // Referrer 策略：只发送同源 Referrer
                    headers.referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN);
                    // 禁用不必要的浏览器特性
                    headers.permissionsPolicy(policy -> policy.policy(
                            "camera=(), microphone=(), geolocation=(), payment=()"));
                    // HSTS：生产环境启用（开发环境不影响，浏览器会忽略非 HTTPS 的 HSTS）
                    headers.httpStrictTransportSecurity()
                            .includeSubDomains(true)
                            .maxAgeInSeconds(31536000);
                })
                .authorizeRequests(authz -> authz
                        .antMatchers(publicPaths()).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(sessionAuthenticationEntryPoint))
                .formLogin().disable()
                .httpBasic().disable()
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll())
                .addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private String[] publicPaths() {
        List<String> paths = new ArrayList<>(List.of(
                "/login",
                "/captcha",
                "/image.png",
                "/css/**",
                "/js/**",
                "/images/**",
                "/error",
                "/health",
                "/actuator/health",
                "/actuator/info"
        ));
        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            paths.add("/swagger-ui/**");
            paths.add("/swagger-ui.html");
            paths.add("/v3/api-docs/**");
        }
        return paths.toArray(new String[0]);
    }
}

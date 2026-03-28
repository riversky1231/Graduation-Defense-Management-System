package com.example.defensemanagement.config;

import com.example.defensemanagement.security.SessionAuthenticationEntryPoint;
import com.example.defensemanagement.security.SessionAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SessionAuthenticationFilter sessionAuthenticationFilter;
    private final SessionAuthenticationEntryPoint sessionAuthenticationEntryPoint;

    public SecurityConfig(SessionAuthenticationFilter sessionAuthenticationFilter,
                          SessionAuthenticationEntryPoint sessionAuthenticationEntryPoint) {
        this.sessionAuthenticationFilter = sessionAuthenticationFilter;
        this.sessionAuthenticationEntryPoint = sessionAuthenticationEntryPoint;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation().migrateSession())
                .headers(headers -> {
                    headers.frameOptions().sameOrigin();
                    headers.contentTypeOptions();
                    headers.xssProtection().block(true);
                })
                .authorizeRequests(authz -> authz
                        .antMatchers("/login", "/captcha", "/image.png", "/css/**", "/js/**", "/images/**", "/error", "/health").permitAll()
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
}

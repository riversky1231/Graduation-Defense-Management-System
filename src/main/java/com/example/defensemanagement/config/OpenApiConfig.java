package com.example.defensemanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI defenseManagementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("毕业答辩管理系统 API")
                        .description("提供院系配置、师生互选、分组打分、成绩计算、文档导出等接口")
                        .version("1.0.0")
                        .contact(new Contact().name("系统管理员")))
                .addSecurityItem(new SecurityRequirement().addList("cookieAuth"))
                .components(new Components()
                        .addSecuritySchemes("cookieAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.COOKIE)
                                        .name("JSESSIONID")
                                        .description("基于 Session Cookie 的认证，登录后自动携带")));
    }
}


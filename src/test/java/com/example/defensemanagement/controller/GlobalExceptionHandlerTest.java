package com.example.defensemanagement.controller;

import com.example.defensemanagement.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler buildHandler(String profile) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(handler, "activeProfile", profile);
        return handler;
    }

    private ApiResponse<?> body(ResponseEntity<ApiResponse<Void>> resp) {
        return resp.getBody();
    }

    @Test
    void devProfile_exposesExceptionMessage() {
        GlobalExceptionHandler handler = buildHandler("dev");
        RuntimeException ex = new RuntimeException("数据库连接失败: jdbc:mysql://secret-host");
        ApiResponse<?> resp = body(handler.handleRuntimeException(ex));
        assertNotNull(resp);
        assertFalse(resp.isSuccess());
        assertEquals("数据库连接失败: jdbc:mysql://secret-host", resp.getMessage(),
                "开发环境应暴露原始异常信息");
    }

    @Test
    void prodProfile_hidesExceptionMessage() {
        GlobalExceptionHandler handler = buildHandler("prod");
        RuntimeException ex = new RuntimeException("数据库连接失败: jdbc:mysql://secret-host");
        ApiResponse<?> resp = body(handler.handleRuntimeException(ex));
        assertNotNull(resp);
        assertFalse(resp.isSuccess());
        assertEquals("服务器内部错误，请联系管理员", resp.getMessage(),
                "生产环境不应暴露内部异常信息");
        assertFalse(resp.getMessage().contains("secret-host"), "不应包含敏感信息");
    }

    @Test
    void prodProfile_nullMessage_stillReturnsGenericError() {
        GlobalExceptionHandler handler = buildHandler("prod");
        RuntimeException ex = new RuntimeException((String) null);
        ApiResponse<?> resp = body(handler.handleRuntimeException(ex));
        assertNotNull(resp);
        assertFalse(resp.isSuccess());
        assertEquals("服务器内部错误，请联系管理员", resp.getMessage());
    }

    @Test
    void devProfile_nullMessage_returnsGenericFallback() {
        GlobalExceptionHandler handler = buildHandler("dev");
        RuntimeException ex = new RuntimeException((String) null);
        ApiResponse<?> resp = body(handler.handleRuntimeException(ex));
        assertNotNull(resp);
        assertFalse(resp.isSuccess());
        // dev 环境 message 为 null 时走兜底 "服务器内部错误"
        assertEquals("服务器内部错误", resp.getMessage());
    }
}


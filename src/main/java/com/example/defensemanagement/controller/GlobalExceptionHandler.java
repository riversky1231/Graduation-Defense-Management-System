package com.example.defensemanagement.controller;

import com.example.defensemanagement.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 全局异常处理器
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理导出相关的异常
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
        log.error("Unhandled runtime exception", e);
        // 检查是否是导出相关的异常
        if (e.getMessage() != null && e.getMessage().contains("导出")) {
            // 如果是模板文件不存在的错误，返回400状态码
            if (e.getMessage().contains("模板文件不存在") || e.getMessage().contains("模板不存在")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
            }

            // 其他导出错误返回500
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(e.getMessage()));
        }

        // 其他RuntimeException，返回通用错误信息
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage() != null ? e.getMessage() : "服务器内部错误"));
    }

    /**
     * 处理IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("上传文件超过大小限制"));
    }
}


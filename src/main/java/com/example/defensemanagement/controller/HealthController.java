package com.example.defensemanagement.controller;

import com.example.defensemanagement.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success("ok", Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}

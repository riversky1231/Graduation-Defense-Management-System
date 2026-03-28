package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.common.RelevanceAnalysisResult;
import com.example.defensemanagement.service.AiCommentService;
import com.example.defensemanagement.service.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 调用通义千问 DashScope 文本生成接口。
 * API: https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
 * 使用兼容OpenAI格式的API端点
 */
@Service
public class AiCommentServiceImpl implements AiCommentService {

    private static final Logger log = LoggerFactory.getLogger(AiCommentServiceImpl.class);
    private static final String DASH_SCOPE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private static final String DEFAULT_MODEL = "qwen-turbo";

    @Autowired
    private ConfigService configService;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public RelevanceAnalysisResult analyzeRelevance(String studentContext, String teacherContext) {
        if (!StringUtils.hasText(studentContext) || !StringUtils.hasText(teacherContext)) {
            return new RelevanceAnalysisResult(0.0, RelevanceAnalysisResult.SOURCE_EMPTY_INPUT, "input_missing");
        }

        String apiKey = configService.getConfigValue(ConfigServiceImpl.KEY_QWEN_API_KEY);
        if (!StringUtils.hasText(apiKey)) {
            return fallbackAnalysis(studentContext, teacherContext, "api_key_missing", null);
        }

        String prompt = "你是毕业设计导师匹配评分器。请根据学生提交材料与教师研究方向/招生要求的匹配程度，"
                + "给出一个0到100之间的整数分数。只返回数字，不要解释。\n"
                + "学生材料：" + studentContext + "\n"
                + "教师信息：" + teacherContext;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("model", DEFAULT_MODEL);
            java.util.List<Map<String, String>> messages = new java.util.ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);
            body.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(
                    DASH_SCOPE_URL, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || !StringUtils.hasText(response.getBody())) {
                String reason = !response.getStatusCode().is2xxSuccessful()
                        ? "http_status_" + response.getStatusCodeValue()
                        : "response_empty";
                return fallbackAnalysis(studentContext, teacherContext, reason, null);
            }

            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = objectMapper.readValue(response.getBody(), Map.class);
            String content = extractContent(resp);
            if (!StringUtils.hasText(content)) {
                return fallbackAnalysis(studentContext, teacherContext, "content_missing", null);
            }
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(content);
            if (!matcher.find()) {
                return fallbackAnalysis(studentContext, teacherContext, "score_parse_failed", null);
            }
            double score = Double.parseDouble(matcher.group(1));
            double normalizedScore = Math.max(0.0, Math.min(100.0, score));
            log.info("Relevance analysis source={}, score={}, reason={}",
                    RelevanceAnalysisResult.SOURCE_MODEL, normalizedScore, "model_success");
            return new RelevanceAnalysisResult(normalizedScore,
                    RelevanceAnalysisResult.SOURCE_MODEL,
                    "model_success");
        } catch (Exception e) {
            return fallbackAnalysis(studentContext, teacherContext,
                    "exception_" + e.getClass().getSimpleName(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public String generateComment(String promptTemplateKey, String context) {
        String template = configService.getPromptTemplate(promptTemplateKey);
        String apiKey = configService.getConfigValue(ConfigServiceImpl.KEY_QWEN_API_KEY);

        if (!StringUtils.hasText(template)) {
            return "【提示】尚未配置评语提示词模板，无法调用大模型。";
        }
        if (!StringUtils.hasText(apiKey)) {
            return "【提示】尚未配置大模型 API Key，无法调用大模型。";
        }

        String prompt = template + "\n上下文：" + (context == null ? "" : context);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);

            // 构建请求体 - 通义千问DashScope API格式（兼容OpenAI格式）
            // 使用compatible-mode端点，直接使用messages格式
            Map<String, Object> body = new HashMap<>();
            body.put("model", DEFAULT_MODEL);

            // 构建messages数组
            java.util.List<Map<String, String>> messages = new java.util.ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            // 直接使用messages，不需要input包装
            body.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // 先获取String类型的响应，避免HttpMessageConverter错误
            // 这样可以处理text/plain格式的错误响应
            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(
                    DASH_SCOPE_URL, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                // 如果状态码不是2xx，返回错误信息
                String errorBody = response.getBody() != null ? response.getBody() : "未知错误";
                return "【提示】调用大模型失败：HTTP " + response.getStatusCode() + " - " + errorBody;
            }

            String responseBody = response.getBody();
            if (responseBody == null || responseBody.trim().isEmpty()) {
                return "【提示】调用大模型失败：无响应";
            }

            // 手动解析JSON响应
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> resp = objectMapper.readValue(responseBody, Map.class);

                // 检查是否有错误信息（OpenAI兼容格式或DashScope错误格式）
                if (resp.containsKey("error")) {
                    Object errorObj = resp.get("error");
                    if (errorObj instanceof Map) {
                        Object messageObj = ((Map<?, ?>) errorObj).get("message");
                        String errorMsg = messageObj != null ? messageObj.toString() : "未知错误";
                        return "【提示】调用大模型失败：" + errorMsg;
                    }
                }
                if (resp.containsKey("code") && resp.containsKey("message")) {
                    String errorCode = resp.get("code") != null ? resp.get("code").toString() : "";
                    String errorMsg = resp.get("message") != null ? resp.get("message").toString() : "";
                    return "【提示】调用大模型失败：[" + errorCode + "] " + errorMsg;
                }

                // OpenAI兼容格式：choices[0].message.content
                Object choicesObj = resp.get("choices");
                if (choicesObj instanceof List && !((List<?>) choicesObj).isEmpty()) {
                    Object first = ((List<?>) choicesObj).get(0);
                    if (first instanceof Map) {
                        Object messageObj = ((Map<?, ?>) first).get("message");
                        if (messageObj instanceof Map) {
                            Object contentObj = ((Map<?, ?>) messageObj).get("content");
                            if (contentObj != null) {
                                return contentObj.toString();
                            }
                        }
                    }
                }

                // 兼容旧格式：output.choices[0].message.content
                Object output = resp.get("output");
                if (output instanceof Map) {
                    Object outputChoicesObj = ((Map<?, ?>) output).get("choices");
                    if (outputChoicesObj instanceof List && !((List<?>) outputChoicesObj).isEmpty()) {
                        Object first = ((List<?>) outputChoicesObj).get(0);
                        if (first instanceof Map) {
                            Object messageObj = ((Map<?, ?>) first).get("message");
                            if (messageObj instanceof Map) {
                                Object contentObj = ((Map<?, ?>) messageObj).get("content");
                                if (contentObj != null) {
                                    return contentObj.toString();
                                }
                            }
                        }
                    }
                }
                return "【提示】调用大模型成功但未获取到内容，响应：" + resp.toString();
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                // 如果无法解析为JSON，可能是错误响应
                return "【提示】调用大模型失败：无法解析响应 - " + responseBody + " (解析错误: " + e.getMessage() + ")";
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 处理4xx错误
            String errorBody = e.getResponseBodyAsString();
            return "【提示】调用大模型失败：HTTP " + e.getStatusCode() + " - " +
                    (errorBody != null && !errorBody.isEmpty() ? errorBody : e.getMessage());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            // 处理5xx错误
            String errorBody = e.getResponseBodyAsString();
            return "【提示】调用大模型失败：HTTP " + e.getStatusCode() + " - " +
                    (errorBody != null && !errorBody.isEmpty() ? errorBody : e.getMessage());
        } catch (org.springframework.web.client.RestClientException e) {
            // 处理其他RestTemplate异常（包括HttpMessageConverter错误）
            return "【提示】调用大模型出错：" + e.getMessage() +
                    (e.getCause() != null ? " (原因: " + e.getCause().getMessage() + ")" : "");
        } catch (Exception e) {
            return "【提示】调用大模型出错：" + e.getMessage() +
                    (e.getCause() != null ? " (原因: " + e.getCause().getMessage() + ")" : "");
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> resp) {
        Object choicesObj = resp.get("choices");
        if (choicesObj instanceof List && !((List<?>) choicesObj).isEmpty()) {
            Object first = ((List<?>) choicesObj).get(0);
            if (first instanceof Map) {
                Object messageObj = ((Map<?, ?>) first).get("message");
                if (messageObj instanceof Map) {
                    Object contentObj = ((Map<?, ?>) messageObj).get("content");
                    if (contentObj != null) {
                        return contentObj.toString();
                    }
                }
            }
        }

        Object output = resp.get("output");
        if (output instanceof Map) {
            Object outputChoicesObj = ((Map<?, ?>) output).get("choices");
            if (outputChoicesObj instanceof List && !((List<?>) outputChoicesObj).isEmpty()) {
                Object first = ((List<?>) outputChoicesObj).get(0);
                if (first instanceof Map) {
                    Object messageObj = ((Map<?, ?>) first).get("message");
                    if (messageObj instanceof Map) {
                        Object contentObj = ((Map<?, ?>) messageObj).get("content");
                        if (contentObj != null) {
                            return contentObj.toString();
                        }
                    }
                }
            }
        }
        return null;
    }

    private double fallbackKeywordScore(String studentContext, String teacherContext) {
        String normalizedStudent = normalize(studentContext);
        String[] teacherTokens = teacherContext.split("[,，;；/、\\s]+");
        int total = 0;
        int matched = 0;
        for (String token : teacherTokens) {
            String normalizedToken = normalize(token);
            if (normalizedToken.length() < 2) {
                continue;
            }
            total++;
            if (normalizedStudent.contains(normalizedToken)) {
                matched++;
            }
        }
        if (total == 0) {
            return 0.0;
        }
        return matched * 100.0 / total;
    }

    private RelevanceAnalysisResult fallbackAnalysis(String studentContext,
                                                     String teacherContext,
                                                     String reason,
                                                     Exception exception) {
        double score = fallbackKeywordScore(studentContext, teacherContext);
        if (exception == null) {
            log.warn("Relevance analysis fallback used: reason={}, score={}", reason, score);
        } else {
            log.warn("Relevance analysis fallback used: reason={}, score={}", reason, score, exception);
        }
        return new RelevanceAnalysisResult(score, RelevanceAnalysisResult.SOURCE_FALLBACK, reason);
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase().replaceAll("[^\\p{IsHan}a-z0-9]+", "");
    }

    @Override
    public String generateCommentStream(String promptTemplateKey, String context, Consumer<String> onChunk) {
        String template = configService.getPromptTemplate(promptTemplateKey);
        String apiKey = configService.getConfigValue(ConfigServiceImpl.KEY_QWEN_API_KEY);

        if (!StringUtils.hasText(template)) {
            String error = "【提示】尚未配置评语提示词模板，无法调用大模型。";
            if (onChunk != null) onChunk.accept(error);
            return error;
        }
        if (!StringUtils.hasText(apiKey)) {
            String error = "【提示】尚未配置大模型 API Key，无法调用大模型。";
            if (onChunk != null) onChunk.accept(error);
            return error;
        }

        String prompt = template + "\n上下文：" + (context == null ? "" : context);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);

            // 构建请求体 - 通义千问DashScope API格式（兼容OpenAI格式）
            Map<String, Object> body = new HashMap<>();
            body.put("model", DEFAULT_MODEL);
            body.put("stream", true); // 启用流式响应

            // 构建messages数组
            java.util.List<Map<String, String>> messages = new java.util.ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);
            body.put("messages", messages);

            // 创建支持流式响应的RestTemplate
            ClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            RestTemplate streamRestTemplate = new RestTemplate(factory);

            StringBuilder fullContent = new StringBuilder();

            // 执行请求并处理流式响应
            org.springframework.web.client.ResponseExtractor<String> responseExtractor = response -> {
                InputStream inputStream = response.getBody();
                if (inputStream == null) {
                    String error = "【提示】调用大模型失败：无响应流";
                    if (onChunk != null) onChunk.accept(error);
                    return error;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty()) continue;
                        
                        // SSE格式：data: {...}
                        if (line.startsWith("data: ")) {
                            String jsonData = line.substring(6).trim();
                            
                            // 流结束标记
                            if ("[DONE]".equals(jsonData)) {
                                break;
                            }

                            try {
                                com.fasterxml.jackson.databind.ObjectMapper objectMapper = 
                                    new com.fasterxml.jackson.databind.ObjectMapper();
                                @SuppressWarnings("unchecked")
                                Map<String, Object> data = objectMapper.readValue(jsonData, Map.class);

                                // 检查错误
                                if (data.containsKey("error")) {
                                    Object errorObj = data.get("error");
                                    if (errorObj instanceof Map) {
                                        Object messageObj = ((Map<?, ?>) errorObj).get("message");
                                        String errorMsg = messageObj != null ? messageObj.toString() : "未知错误";
                                        String error = "【提示】调用大模型失败：" + errorMsg;
                                        if (onChunk != null) onChunk.accept(error);
                                        return error;
                                    }
                                }

                                // 解析choices[0].delta.content
                                Object choicesObj = data.get("choices");
                                if (choicesObj instanceof List && !((List<?>) choicesObj).isEmpty()) {
                                    Object first = ((List<?>) choicesObj).get(0);
                                    if (first instanceof Map) {
                                        Object deltaObj = ((Map<?, ?>) first).get("delta");
                                        if (deltaObj instanceof Map) {
                                            Object contentObj = ((Map<?, ?>) deltaObj).get("content");
                                            if (contentObj != null) {
                                                String chunk = contentObj.toString();
                                                fullContent.append(chunk);
                                                if (onChunk != null) {
                                                    onChunk.accept(chunk);
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                                // 忽略单个JSON解析错误，继续处理下一行
                                System.err.println("解析SSE数据失败: " + jsonData + ", 错误: " + e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    String error = "【提示】读取流式响应失败：" + e.getMessage();
                    if (onChunk != null) onChunk.accept(error);
                    return error;
                }

                return fullContent.toString();
            };

            java.net.URI uri = java.net.URI.create(DASH_SCOPE_URL);
            String result = streamRestTemplate.execute(uri, HttpMethod.POST, 
                    request -> {
                        request.getHeaders().addAll(headers);
                        com.fasterxml.jackson.databind.ObjectMapper mapper = 
                            new com.fasterxml.jackson.databind.ObjectMapper();
                        mapper.writeValue(request.getBody(), body);
                    }, 
                    responseExtractor);

            if (result == null || result.isEmpty()) {
                result = fullContent.toString();
                if (result.isEmpty()) {
                    result = "【提示】调用大模型成功但未获取到内容";
                    if (onChunk != null) onChunk.accept(result);
                }
            }
            return result;

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            String error = "【提示】调用大模型失败：HTTP " + e.getStatusCode() + " - " +
                    (errorBody != null && !errorBody.isEmpty() ? errorBody : e.getMessage());
            if (onChunk != null) onChunk.accept(error);
            return error;
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            String error = "【提示】调用大模型失败：HTTP " + e.getStatusCode() + " - " +
                    (errorBody != null && !errorBody.isEmpty() ? errorBody : e.getMessage());
            if (onChunk != null) onChunk.accept(error);
            return error;
        } catch (Exception e) {
            String error = "【提示】调用大模型出错：" + e.getMessage() +
                    (e.getCause() != null ? " (原因: " + e.getCause().getMessage() + ")" : "");
            if (onChunk != null) onChunk.accept(error);
            return error;
        }
    }
}

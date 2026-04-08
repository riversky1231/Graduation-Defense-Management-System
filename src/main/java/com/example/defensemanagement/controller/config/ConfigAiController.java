package com.example.defensemanagement.controller.config;

import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.impl.ConfigServiceImpl;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.Map;

@Controller
@RequestMapping("/admin/config")
public class ConfigAiController extends AbstractConfigController {

    public ConfigAiController(ConfigService configService) {
        super(configService);
    }

    @PostMapping("/ai/key/save")
    @ResponseBody
    public String setQwenApiKey(@RequestParam String apiKey, HttpSession session) {
        String permissionError = checkAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }

        try {
            configService.setQwenApiKey(apiKey);
            return "success";
        } catch (Exception e) {
            return "error:保存API Key失败, " + e.getMessage();
        }
    }

    @PostMapping("/ai/template/save")
    @ResponseBody
    public String setPromptTemplate(@RequestBody Map<String, String> request, HttpSession session) {
        String permissionError = checkAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }

        try {
            String templateKey = normalizeUpperCase(request.get("templateKey"));
            String templateContent = request.get("templateContent");

            if (templateKey == null || templateContent == null) {
                return "error:缺少模板参数";
            }
            if (!ALLOWED_TEMPLATE_KEYS.contains(templateKey)) {
                return "error:非法模板类型";
            }

            configService.setPromptTemplate(templateKey, templateContent);
            return "success";
        } catch (Exception e) {
            return "error:保存提示词模板失败, " + e.getMessage();
        }
    }

    @GetMapping("/ai/template/get")
    @ResponseBody
    public String getPromptTemplate(@RequestParam String templateKey, HttpSession session) {
        String permissionError = checkAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }

        try {
            String normalizedTemplateKey = normalizeUpperCase(templateKey);
            if (!ALLOWED_TEMPLATE_KEYS.contains(normalizedTemplateKey)) {
                return "error:非法模板类型";
            }
            String template = configService.getPromptTemplate(normalizedTemplateKey);
            return template != null ? template : "";
        } catch (Exception e) {
            return "error:获取提示词模板失败, " + e.getMessage();
        }
    }

    @GetMapping("/ai/key/get")
    @ResponseBody
    public String getQwenApiKey(HttpSession session) {
        String permissionError = checkAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }

        try {
            String apiKey = configService.getConfigValue(ConfigServiceImpl.KEY_QWEN_API_KEY);
            if (apiKey == null || apiKey.isEmpty()) {
                return "";
            }
            int len = apiKey.length();
            return len > 4 ? "****" + apiKey.substring(len - 4) : "****";
        } catch (Exception e) {
            return "error:获取API Key失败, " + e.getMessage();
        }
    }
}

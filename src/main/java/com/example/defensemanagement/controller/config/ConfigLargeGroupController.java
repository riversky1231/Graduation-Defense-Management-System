package com.example.defensemanagement.controller.config;

import com.example.defensemanagement.service.ConfigService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/config")
public class ConfigLargeGroupController extends AbstractConfigController {

    private static final String KEY_LARGE_GROUP_DEADLINE = "LARGE_GROUP_DEADLINE";
    private static final String KEY_LARGE_GROUP_ARCHIVED = "LARGE_GROUP_ARCHIVED";
    private static final String LARGE_GROUP_ARCHIVED_DESC = "大组成绩是否已归档(0未归档/1已归档)";

    public ConfigLargeGroupController(ConfigService configService) {
        super(configService);
    }

    @GetMapping("/largegroup/get")
    @ResponseBody
    public Map<String, Object> getLargeGroupConfig(HttpSession session) {
        requireAdmin(session);
        Map<String, Object> result = new HashMap<>();
        result.put("deadline", configService.getConfigValue(KEY_LARGE_GROUP_DEADLINE));
        result.put("archived", configService.getConfigValue(KEY_LARGE_GROUP_ARCHIVED));
        return result;
    }

    @PostMapping("/largegroup/save")
    @ResponseBody
    public String saveLargeGroupConfig(@RequestBody Map<String, Object> request, HttpSession session) {
        String permissionError = checkAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }
        try {
            Object deadline = request.get("deadline");
            if (deadline != null && !String.valueOf(deadline).trim().isEmpty()) {
                configService.saveConfig(
                        KEY_LARGE_GROUP_DEADLINE,
                        String.valueOf(deadline).trim(),
                        "大组打分截止时间");
            }
            return "success";
        } catch (Exception e) {
            return "error:保存大组打分配置失败, " + e.getMessage();
        }
    }

    @PostMapping("/largegroup/archive")
    @ResponseBody
    public String archiveLargeGroup(HttpSession session) {
        String permissionError = checkAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }
        try {
            configService.saveConfig(KEY_LARGE_GROUP_ARCHIVED, "1", LARGE_GROUP_ARCHIVED_DESC);
            return "success";
        } catch (Exception e) {
            return "error:归档失败, " + e.getMessage();
        }
    }

    @PostMapping("/largegroup/unarchive")
    @ResponseBody
    public String unarchiveLargeGroup(HttpSession session) {
        String permissionError = checkAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }
        try {
            configService.saveConfig(KEY_LARGE_GROUP_ARCHIVED, "0", LARGE_GROUP_ARCHIVED_DESC);
            return "success";
        } catch (Exception e) {
            return "error:取消归档失败, " + e.getMessage();
        }
    }
}

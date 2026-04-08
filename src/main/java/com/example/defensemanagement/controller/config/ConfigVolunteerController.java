package com.example.defensemanagement.controller.config;

import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.impl.ConfigServiceImpl;
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
public class ConfigVolunteerController extends AbstractConfigController {

    public ConfigVolunteerController(ConfigService configService) {
        super(configService);
    }

    @GetMapping("/volunteer/get")
    @ResponseBody
    public Map<String, Object> getVolunteerConfig(HttpSession session) {
        requireAdmin(session);
        Map<String, Object> result = new HashMap<>();
        result.put("maxStudents", configService.getConfigValue(ConfigServiceImpl.KEY_TEACHER_MAX_STUDENTS));
        result.put("deadline", configService.getConfigValue(ConfigServiceImpl.KEY_VOLUNTEER_DEADLINE));
        result.put("currentRound", configService.getConfigValue(ConfigServiceImpl.KEY_VOLUNTEER_CURRENT_ROUND));
        return result;
    }

    @PostMapping("/volunteer/save")
    @ResponseBody
    public String saveVolunteerConfig(@RequestBody Map<String, Object> request, HttpSession session) {
        String permissionError = checkAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }
        try {
            Object maxStudents = request.get("maxStudents");
            Object deadline = request.get("deadline");

            if (maxStudents != null && !String.valueOf(maxStudents).trim().isEmpty()) {
                configService.saveConfig(
                        ConfigServiceImpl.KEY_TEACHER_MAX_STUDENTS,
                        String.valueOf(maxStudents).trim(),
                        "教师最多可带学生数量");
            }
            if (deadline != null && !String.valueOf(deadline).trim().isEmpty()) {
                configService.saveConfig(
                        ConfigServiceImpl.KEY_VOLUNTEER_DEADLINE,
                        String.valueOf(deadline).trim(),
                        "志愿互选截止时间");
            }
            return "success";
        } catch (Exception e) {
            return "error:保存志愿配置失败, " + e.getMessage();
        }
    }
}

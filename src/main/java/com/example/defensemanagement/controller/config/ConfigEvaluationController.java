package com.example.defensemanagement.controller.config;

import com.example.defensemanagement.entity.EvaluationItem;
import com.example.defensemanagement.service.ConfigService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/admin/config")
public class ConfigEvaluationController extends AbstractConfigController {

    public ConfigEvaluationController(ConfigService configService) {
        super(configService);
    }

    @GetMapping("/evaluation/list")
    @ResponseBody
    public List<EvaluationItem> getEvaluationItems(@RequestParam String type, HttpSession session) {
        requireAdmin(session);
        return configService.getEvaluationItems(type.toUpperCase());
    }

    @PostMapping("/evaluation/save")
    @ResponseBody
    public String saveEvaluationItems(
            @RequestParam String type,
            @RequestBody List<EvaluationItem> items,
            HttpSession session) {
        String permissionError = checkAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }

        try {
            configService.saveEvaluationItems(type.toUpperCase(), items);
            return "success";
        } catch (Exception e) {
            return "error:保存评分指标失败, " + e.getMessage();
        }
    }
}

package com.example.defensemanagement.controller.config;

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
import java.util.Map;

@Controller
@RequestMapping("/admin/config")
public class ConfigTimelineController extends AbstractConfigController {

    public ConfigTimelineController(ConfigService configService) {
        super(configService);
    }

    @PostMapping("/year/set")
    @ResponseBody
    public String setCurrentDefenseYear(@RequestParam Integer year, HttpSession session) {
        String permissionError = checkAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }
        if (!isAllowedYear(year)) {
            return "error:年份必须在 " + MIN_YEAR + "-" + MAX_YEAR + " 之间";
        }

        try {
            configService.setCurrentDefenseYear(year);
            return "success";
        } catch (Exception e) {
            return "error:设置答辩年份失败, " + e.getMessage();
        }
    }

    @GetMapping("/year/current")
    @ResponseBody
    public Integer getCurrentDefenseYear() {
        return configService.getCurrentDefenseYear();
    }

    @GetMapping("/years/list")
    @ResponseBody
    public List<Integer> getAllYears(HttpSession session) {
        return configService.getAllYears();
    }

    @PostMapping("/date/set")
    @ResponseBody
    public String setDefenseDate(@RequestBody Map<String, Object> request, HttpSession session) {
        String permissionError = checkAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }

        try {
            String dateKeyPrefix = normalizeUpperCase((String) request.get("dateKeyPrefix"));
            Integer year = (Integer) request.get("year");
            Integer month = (Integer) request.get("month");
            Integer day = (Integer) request.get("day");

            if (dateKeyPrefix == null || year == null || month == null || day == null) {
                return "error:缺少日期参数";
            }
            if (!ALLOWED_DATE_KEY_PREFIXES.contains(dateKeyPrefix)) {
                return "error:非法日期配置项";
            }
            if (!isValidCalendarDate(year, month, day)) {
                return "error:日期参数不合法";
            }

            configService.setDefenseDate(dateKeyPrefix, year, month, day);
            return "success";
        } catch (Exception e) {
            return "error:设置日期失败, " + e.getMessage();
        }
    }

    @GetMapping("/date/get")
    @ResponseBody
    public String getDefenseDatePart(@RequestParam String key, HttpSession session) {
        String permissionError = checkAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }

        try {
            String value = configService.getDefenseDatePart(key);
            return value != null ? value : "";
        } catch (Exception e) {
            return "error:获取日期配置失败, " + e.getMessage();
        }
    }
}

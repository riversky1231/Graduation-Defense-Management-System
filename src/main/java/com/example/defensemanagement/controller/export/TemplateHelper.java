package com.example.defensemanagement.controller.export;

import com.example.defensemanagement.service.ConfigService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

@Component
public class TemplateHelper {

    private final ConfigService configService;

    @Value("${app.upload.base-dir:uploads}")
    private String uploadBaseDir;

    public TemplateHelper(ConfigService configService) {
        this.configService = configService;
    }

    Path getUploadBasePath() {
        Path basePath = Paths.get(uploadBaseDir);
        if (!basePath.isAbsolute()) {
            String userDir = System.getProperty("user.dir");
            basePath = Paths.get(userDir, uploadBaseDir);
        }
        return basePath;
    }

    String resolveTemplate(String key, String defaultClasspath, Long departmentId) {
        Path basePath = getUploadBasePath();
        if (departmentId != null) {
            Path deptPath = basePath.resolve("templates").resolve("dept_" + departmentId).resolve(key + ".docx");
            if (Files.exists(deptPath)) {
                return deptPath.toAbsolutePath().toString();
            }
        }

        Path globalPath = basePath.resolve("templates").resolve(key + ".docx");
        if (Files.exists(globalPath)) {
            return globalPath.toAbsolutePath().toString();
        }
        return defaultClasspath;
    }

    ExportHelper.DateParts getDateParts(String prefix) {
        String year = configService.getDefenseDatePart(prefix + "_YEAR");
        String month = configService.getDefenseDatePart(prefix + "_MONTH");
        String day = configService.getDefenseDatePart(prefix + "_DAY");
        LocalDate now = LocalDate.now();

        return new ExportHelper.DateParts(
                nvl(year, String.valueOf(now.getYear())),
                nvl(month, String.valueOf(now.getMonthValue())),
                nvl(day, String.valueOf(now.getDayOfMonth())));
    }

    String encode(String name) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String nvl(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}

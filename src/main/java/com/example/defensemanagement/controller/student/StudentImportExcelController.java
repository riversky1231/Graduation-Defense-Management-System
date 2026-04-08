package com.example.defensemanagement.controller.student;

import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.service.ConfigService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.time.Year;

/**
 * 学生 Excel 导入 Controller。
 */
@RestController
@RequestMapping("/department/student")
public class StudentImportExcelController extends AbstractStudentController {

    private final StudentExcelImportSupport studentExcelImportSupport;
    private final ConfigService configService;

    public StudentImportExcelController(
            StudentExcelImportSupport studentExcelImportSupport,
            ConfigService configService) {
        this.studentExcelImportSupport = studentExcelImportSupport;
        this.configService = configService;
    }

    @PostMapping("/import/excel")
    @ResponseBody
    public String importStudentsFromExcel(@RequestParam("file") MultipartFile file, HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }

        User currentUser = (User) session.getAttribute("currentUser");
        Long departmentId = currentUser != null ? currentUser.getDepartmentId() : null;
        Integer currentYear = configService.getCurrentDefenseYear();
        if (currentYear == null) {
            currentYear = Year.now().getValue();
        }
        return studentExcelImportSupport.importStudents(file, departmentId, currentYear);
    }
}

package com.example.defensemanagement.controller.defense;

import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.service.DefenseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;

/**
 * 答辩首页 Controller。
 */
@Controller
public class DefenseHomeController extends AbstractDefenseController {

    public DefenseHomeController(
            DefenseService defenseService,
            DefenseGroupTeacherMapper defenseGroupTeacherMapper,
            DefenseGroupMapper defenseGroupMapper,
            TeacherMapper teacherMapper,
            DepartmentMapper departmentMapper) {
        super(defenseService, defenseGroupTeacherMapper, defenseGroupMapper, teacherMapper, departmentMapper);
    }

    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        User currentUser = getCurrentUser(session);
        Teacher currentTeacher = resolveCurrentTeacher(session);
        if (currentUser == null && currentTeacher == null) {
            return "redirect:/login";
        }

        model.addAttribute("groups", resolveVisibleGroups(currentUser, currentTeacher));
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentTeacher", currentTeacher);
        model.addAttribute("isDefenseLeader", isDefenseLeader(currentTeacher != null ? currentTeacher.getId() : null));
        return "index";
    }
}

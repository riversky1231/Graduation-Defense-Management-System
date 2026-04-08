package com.example.defensemanagement.controller.defense;

import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.service.DefenseService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 答辩小组公共操作 Controller。
 */
@Controller
public class DefenseGroupOperationController extends AbstractDefenseController {

    public DefenseGroupOperationController(
            DefenseService defenseService,
            DefenseGroupTeacherMapper defenseGroupTeacherMapper,
            DefenseGroupMapper defenseGroupMapper,
            TeacherMapper teacherMapper,
            DepartmentMapper departmentMapper) {
        super(defenseService, defenseGroupTeacherMapper, defenseGroupMapper, teacherMapper, departmentMapper);
    }

    @GetMapping("/group/{id}/members")
    @ResponseBody
    public List<String> getMembers(@PathVariable Long id) {
        return defenseService.getGroupStudentInfo(id);
    }

    @PostMapping("/comment")
    public String addComment(@RequestParam Long groupId, @RequestParam String comment) {
        defenseService.addComment(groupId, comment);
        return "redirect:/";
    }

    @PostMapping("/updateOrder")
    @ResponseBody
    public void updateOrder(@RequestBody List<Long> groupIds) {
        defenseService.updateOrder(groupIds);
    }

    @PostMapping("/group/{id}/score")
    @ResponseBody
    public void updateScore(@PathVariable Long id, @RequestParam int score) {
        defenseService.updateScore(id, score);
    }

    @PostMapping("/member/add")
    @ResponseBody
    public void addMember(@RequestBody GroupMemberRequest request) {
        throw new UnsupportedOperationException("成员维护已迁移到学生管理：请在学生管理中分配/移除学生的小组");
    }

    @DeleteMapping("/member/{id}")
    @ResponseBody
    public void deleteMember(@PathVariable Long id) {
        throw new UnsupportedOperationException("成员维护已迁移到学生管理：请在学生管理中分配/移除学生的小组");
    }

    public static class GroupMemberRequest {
        private Long groupId;
        private String name;

        public Long getGroupId() {
            return groupId;
        }

        public void setGroupId(Long groupId) {
            this.groupId = groupId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}

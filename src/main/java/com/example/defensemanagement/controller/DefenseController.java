package com.example.defensemanagement.controller;

import com.example.defensemanagement.service.DefenseService;
import com.example.defensemanagement.entity.ArchiveSession;
import com.example.defensemanagement.entity.ArchiveDetail;
import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.DepartmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
public class DefenseController {

    @Autowired
    private DefenseService defenseService;
    
    @Autowired
    private DefenseGroupTeacherMapper defenseGroupTeacherMapper;
    
    @Autowired
    private DefenseGroupMapper defenseGroupMapper;
    
    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        // 检查是否已登录
        User currentUser = (User) session.getAttribute("currentUser");
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");

        if (currentUser == null && currentTeacher == null) {
            return "redirect:/login";
        }

        // 检查教师是否为答辩组长
        boolean isDefenseLeader = false;
        Long teacherId = null;
        
        // 如果 currentTeacher 不为空，直接使用
        if (currentTeacher != null) {
            teacherId = currentTeacher.getId();
        } 
        // 如果 currentUser 不为空且是教师角色，查找对应的 Teacher
        else if (currentUser != null && currentUser.getRole() != null && 
                 ("TEACHER".equals(currentUser.getRole().getName()) || 
                  "DEFENSE_LEADER".equals(currentUser.getRole().getName()))) {
            Teacher teacher = teacherMapper.findByUserId(currentUser.getId());
            if (teacher != null) {
                teacherId = teacher.getId();
                // 同时设置 currentTeacher 到 session，方便后续使用
                session.setAttribute("currentTeacher", teacher);
                currentTeacher = teacher;
            }
        }
        
        // 如果找到了 teacherId，检查是否为答辩组长
        if (teacherId != null) {
            List<DefenseGroupTeacher> allGroups = defenseGroupTeacherMapper.findAll();
            for (DefenseGroupTeacher gt : allGroups) {
                if (gt.getTeacherId() != null && gt.getTeacherId().equals(teacherId) && 
                    gt.getIsLeader() != null && gt.getIsLeader() == 1) {
                    isDefenseLeader = true;
                    break;
                }
            }
        }
        
        // 根据用户角色返回对应的小组数据（数据隔离）
        List<DefenseGroup> groups;
        if (currentUser != null && currentUser.getRole() != null) {
            String roleName = currentUser.getRole().getName();
            if ("SUPER_ADMIN".equals(roleName)) {
                // 超级管理员：返回所有小组
                groups = defenseService.getAllGroups();
            } else if ("DEPT_ADMIN".equals(roleName)) {
                // 院系管理员：返回本院系小组
                Long departmentId = currentUser.getDepartmentId();
                if (departmentId != null) {
                    groups = defenseGroupMapper.findByDepartmentId(departmentId);
                } else {
                    groups = java.util.Collections.emptyList();
                }
            } else {
                // 其他角色（教师/答辩组长）：返回本院系小组
                Long departmentId = currentTeacher != null ? currentTeacher.getDepartmentId() : null;
                if (departmentId != null) {
                    groups = defenseGroupMapper.findByDepartmentId(departmentId);
                } else {
                    groups = java.util.Collections.emptyList();
                }
            }
        } else if (currentTeacher != null) {
            // 教师登录：返回本院系小组
            Long departmentId = currentTeacher.getDepartmentId();
            if (departmentId != null) {
                groups = defenseGroupMapper.findByDepartmentId(departmentId);
            } else {
                groups = java.util.Collections.emptyList();
            }
        } else {
            groups = java.util.Collections.emptyList();
        }
        
        model.addAttribute("groups", groups);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentTeacher", currentTeacher);
        model.addAttribute("isDefenseLeader", isDefenseLeader);

        return "index";
    }

    @GetMapping("/group/{id}/members")
    @ResponseBody
    public List<String> getMembers(@PathVariable Long id) {
        // 返回学生姓名和项目名称
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
        // Historical endpoint kept for backward compatibility with old UI.
        // Member management is now based on Student records (t_student.defense_group_id).
        throw new UnsupportedOperationException("成员维护已迁移到学生管理：请在学生管理中分配/移除学生的小组");
    }

    @DeleteMapping("/member/{id}")
    @ResponseBody
    public void deleteMember(@PathVariable Long id) {
        // Historical endpoint kept for backward compatibility with old UI.
        // Member management is now based on Student records (t_student.defense_group_id).
        throw new UnsupportedOperationException("成员维护已迁移到学生管理：请在学生管理中分配/移除学生的小组");
    }

    @PostMapping("/archive/current")
    @ResponseBody
    public void archiveCurrentSession() {
        defenseService.archiveCurrentSession();
    }

    @GetMapping("/archive/list")
    @ResponseBody
    public List<ArchiveSession> getArchiveList() {
        return defenseService.getArchiveList();
    }

    @GetMapping("/archive/{id}/detail")
    @ResponseBody
    public ArchiveDetail getArchiveDetail(@PathVariable Long id) {
        return defenseService.getArchiveDetail(id);
    }

    @DeleteMapping("/archive/{id}")
    @ResponseBody
    public void deleteArchive(@PathVariable Long id) {
        defenseService.deleteArchive(id);
    }

    @PostMapping("/group/add")
    @ResponseBody
    public String addGroup(@RequestBody AddGroupRequest request, HttpSession session) {
        // 检查权限：超级管理员或院系管理员可以添加小组
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "error:请先登录";
        }
        
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
        Long departmentId = null;
        
        if ("SUPER_ADMIN".equals(roleName)) {
            // 超级管理员可以为任何院系创建小组，但需要指定departmentId
            departmentId = request.getDepartmentId();
            if (departmentId == null) {
                return "error:请指定小组所属院系";
            }
        } else if ("DEPT_ADMIN".equals(roleName)) {
            // 院系管理员只能为本院系创建小组
            departmentId = currentUser.getDepartmentId();
            if (departmentId == null) {
                return "error:院系信息未配置";
            }
        } else {
            return "error:权限不足：只有管理员可以添加小组";
        }
        
        try {
            DefenseGroup g = new DefenseGroup();
            
            // 如果名称为空或null，自动生成
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                String autoName = generateGroupName(departmentId);
                g.setName(autoName);
            } else {
                g.setName(request.getName());
            }
            
            g.setScore(request.getScore());
            g.setDepartmentId(departmentId);
            // Put it at the end by default; user can reorder via updateOrder.
            int order = defenseService.getAllGroups() != null ? defenseService.getAllGroups().size() : 0;
            g.setDisplayOrder(order);
            defenseService.addGroup(g);
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    /**
     * 批量创建小组
     * POST /group/addBatch
     */
    @PostMapping("/group/addBatch")
    @ResponseBody
    public String addGroupsBatch(@RequestBody BatchAddGroupRequest request, HttpSession session) {
        // 检查权限：超级管理员或院系管理员可以添加小组
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "error:请先登录";
        }
        
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
        Long departmentId = null;
        
        if ("SUPER_ADMIN".equals(roleName)) {
            // 超级管理员需要指定departmentId
            departmentId = request.getDepartmentId();
            if (departmentId == null) {
                return "error:请指定小组所属院系";
            }
        } else if ("DEPT_ADMIN".equals(roleName)) {
            // 院系管理员只能为本院系创建小组
            departmentId = currentUser.getDepartmentId();
            if (departmentId == null) {
                return "error:院系信息未配置";
            }
        } else {
            return "error:权限不足：只有管理员可以添加小组";
        }
        
        int count = request.getCount();
        if (count < 1 || count > 50) {
            return "error:小组个数必须在1-50之间";
        }
        
        try {
            // 获取该院系已有小组数量
            List<DefenseGroup> deptGroups = defenseGroupMapper.findByDepartmentId(departmentId);
            int currentCount = deptGroups != null ? deptGroups.size() : 0;
            // 获取院系简称
            Department dept = departmentMapper.findById(departmentId);
            String deptPrefix = getDeptShortName(dept);

            // 批量创建小组
            for (int i = 1; i <= count; i++) {
                DefenseGroup g = new DefenseGroup();
                g.setName(deptPrefix + "第" + toChineseNumber(currentCount + i) + "组");
                g.setScore(0);
                g.setDepartmentId(departmentId);
                g.setDisplayOrder(currentCount + i);
                defenseService.addGroup(g);
            }

            return "success:成功创建" + count + "个小组";
        } catch (Exception e) {
            e.printStackTrace();
            return "error:批量创建失败：" + e.getMessage();
        }
    }

    /**
     * 自动生成小组名称（按院系序号，如"计算机第三组"）
     */
    private String generateGroupName(Long departmentId) {
        List<DefenseGroup> deptGroups = defenseGroupMapper.findByDepartmentId(departmentId);
        int currentCount = deptGroups != null ? deptGroups.size() : 0;
        Department dept = departmentMapper.findById(departmentId);
        String deptPrefix = getDeptShortName(dept);
        return deptPrefix + "第" + toChineseNumber(currentCount + 1) + "组";
    }

    /**
     * 将数字转为中文序数（1-20），超出范围返回阿拉伯数字
     */
    private String toChineseNumber(int n) {
        String[] cn = {"一","二","三","四","五","六","七","八","九","十",
                        "十一","十二","十三","十四","十五","十六","十七","十八","十九","二十"};
        if (n >= 1 && n <= 20) return cn[n - 1];
        return String.valueOf(n);
    }

    /**
     * 获取院系简称，用于小组命名（如"计算机第一组"）。
     * 优先读取 department.description 字段（管理员填写的简称）；
     * 为空时兜底：去掉"学院"/"系"/"部"后缀，取前3字。
     */
    private String getDeptShortName(Department dept) {
        if (dept == null) return "";
        // 优先用 description 字段（管理员填写的简称，如"计算机"、"信通"）
        String desc = dept.getDescription();
        if (desc != null && !desc.isBlank()) return desc.trim();
        // 兜底：去掉常见后缀，取前3字
        String name = dept.getName().replace("学院", "").replace("系", "").replace("部", "");
        return name.length() > 3 ? name.substring(0, 3) : name;
    }

    @DeleteMapping("/group/{id}")
    @ResponseBody
    public String deleteGroup(@PathVariable Long id, HttpSession session) {
        // 检查权限：超级管理员或院系管理员可以删除小组
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "error:请先登录";
        }
        
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
        
        // 检查组内是否有成员
        DefenseGroup group = defenseService.getGroupById(id);
        if (group == null) {
            return "error:小组不存在";
        }
        
        // 院系管理员只能删除本院系的小组
        if ("DEPT_ADMIN".equals(roleName)) {
            Long userDeptId = currentUser.getDepartmentId();
            if (userDeptId == null || !userDeptId.equals(group.getDepartmentId())) {
                return "error:只能删除本院系的小组";
            }
        } else if (!"SUPER_ADMIN".equals(roleName)) {
            return "error:权限不足：只有管理员可以删除小组";
        }
        
        if (group.getMembers() != null && !group.getMembers().isEmpty()) {
            return "error:组内有成员，无法删除小组。请先移除所有成员后再删除。";
        }
        
        defenseService.deleteGroup(id);
        return "success";
    }

    // 内部类用于接收请求参数
    public static class GroupMemberRequest {
        private Long groupId;
        private String name;

        public Long getGroupId() { return groupId; }
        public void setGroupId(Long groupId) { this.groupId = groupId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class AddGroupRequest {
        private String name;
        private int score;
        private Long departmentId;
        private List<String> members;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        public List<String> getMembers() { return members; }
        public void setMembers(List<String> members) { this.members = members; }
    }

    public static class BatchAddGroupRequest {
        private int count;
        private Long departmentId;

        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    }
}
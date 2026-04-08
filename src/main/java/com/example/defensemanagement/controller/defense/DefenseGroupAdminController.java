package com.example.defensemanagement.controller.defense;

import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.service.DefenseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 答辩小组管理 Controller。
 */
@RestController
public class DefenseGroupAdminController extends AbstractDefenseController {

    private static final Logger log = LoggerFactory.getLogger(DefenseGroupAdminController.class);

    public DefenseGroupAdminController(
            DefenseService defenseService,
            DefenseGroupTeacherMapper defenseGroupTeacherMapper,
            DefenseGroupMapper defenseGroupMapper,
            TeacherMapper teacherMapper,
            DepartmentMapper departmentMapper) {
        super(defenseService, defenseGroupTeacherMapper, defenseGroupMapper, teacherMapper, departmentMapper);
    }

    @PostMapping("/group/add")
    @ResponseBody
    public String addGroup(@RequestBody AddGroupRequest request, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return "error:请先登录";
        }

        Long departmentId = resolveDepartmentIdForCreate(currentUser, request.getDepartmentId());
        if (departmentId == null) {
            return resolveDepartmentErrorForCreate(currentUser, request.getDepartmentId());
        }

        try {
            DefenseGroup group = new DefenseGroup();
            group.setName(isBlank(request.getName()) ? generateGroupName(departmentId) : request.getName());
            group.setScore(request.getScore());
            group.setDepartmentId(departmentId);
            List<DefenseGroup> allGroups = defenseService.getAllGroups();
            group.setDisplayOrder(allGroups != null ? allGroups.size() : 0);
            defenseService.addGroup(group);
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @PostMapping("/group/addBatch")
    @ResponseBody
    public String addGroupsBatch(@RequestBody BatchAddGroupRequest request, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return "error:请先登录";
        }

        Long departmentId = resolveDepartmentIdForCreate(currentUser, request.getDepartmentId());
        if (departmentId == null) {
            return resolveDepartmentErrorForCreate(currentUser, request.getDepartmentId());
        }
        if (request.getCount() < 1 || request.getCount() > 50) {
            return "error:小组个数必须在1-50之间";
        }

        try {
            List<DefenseGroup> departmentGroups = defenseGroupMapper.findByDepartmentId(departmentId);
            int currentCount = departmentGroups != null ? departmentGroups.size() : 0;
            Department department = departmentMapper.findById(departmentId);
            String departmentPrefix = getDeptShortName(department);

            for (int i = 1; i <= request.getCount(); i++) {
                DefenseGroup group = new DefenseGroup();
                group.setName(departmentPrefix + "第" + toChineseNumber(currentCount + i) + "组");
                group.setScore(0);
                group.setDepartmentId(departmentId);
                group.setDisplayOrder(currentCount + i);
                defenseService.addGroup(group);
            }
            return "success:成功创建" + request.getCount() + "个小组";
        } catch (Exception e) {
            log.warn("Failed to batch create defense groups", e);
            return "error:批量创建失败：" + e.getMessage();
        }
    }

    @DeleteMapping("/group/{id}")
    @ResponseBody
    public String deleteGroup(@PathVariable Long id, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return "error:请先登录";
        }

        DefenseGroup group = defenseService.getGroupById(id);
        if (group == null) {
            return "error:小组不存在";
        }

        String roleName = getRoleName(currentUser);
        if ("DEPT_ADMIN".equals(roleName)) {
            Long userDepartmentId = currentUser.getDepartmentId();
            if (userDepartmentId == null || !userDepartmentId.equals(group.getDepartmentId())) {
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

    private Long resolveDepartmentIdForCreate(User currentUser, Long requestedDepartmentId) {
        String roleName = getRoleName(currentUser);
        if ("SUPER_ADMIN".equals(roleName)) {
            return requestedDepartmentId;
        }
        if ("DEPT_ADMIN".equals(roleName)) {
            return currentUser.getDepartmentId();
        }
        return null;
    }

    private String resolveDepartmentErrorForCreate(User currentUser, Long requestedDepartmentId) {
        String roleName = getRoleName(currentUser);
        if ("SUPER_ADMIN".equals(roleName) && requestedDepartmentId == null) {
            return "error:请指定小组所属院系";
        }
        if ("DEPT_ADMIN".equals(roleName) && currentUser.getDepartmentId() == null) {
            return "error:院系信息未配置";
        }
        return "error:权限不足：只有管理员可以添加小组";
    }

    private String generateGroupName(Long departmentId) {
        List<DefenseGroup> departmentGroups = defenseGroupMapper.findByDepartmentId(departmentId);
        int currentCount = departmentGroups != null ? departmentGroups.size() : 0;
        Department department = departmentMapper.findById(departmentId);
        return getDeptShortName(department) + "第" + toChineseNumber(currentCount + 1) + "组";
    }

    private String toChineseNumber(int number) {
        String[] names = {"一", "二", "三", "四", "五", "六", "七", "八", "九", "十",
                "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十"};
        if (number >= 1 && number <= 20) {
            return names[number - 1];
        }
        return String.valueOf(number);
    }

    private String getDeptShortName(Department department) {
        if (department == null) {
            return "";
        }
        if (!isBlank(department.getDescription())) {
            return department.getDescription().trim();
        }
        String name = department.getName() == null ? "" : department.getName();
        name = name.replace("学院", "").replace("系", "").replace("部", "");
        return name.length() > 3 ? name.substring(0, 3) : name;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class AddGroupRequest {
        private String name;
        private int score;
        private Long departmentId;
        private List<String> members;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public Long getDepartmentId() {
            return departmentId;
        }

        public void setDepartmentId(Long departmentId) {
            this.departmentId = departmentId;
        }

        public List<String> getMembers() {
            return members;
        }

        public void setMembers(List<String> members) {
            this.members = members;
        }
    }

    public static class BatchAddGroupRequest {
        private int count;
        private Long departmentId;

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public Long getDepartmentId() {
            return departmentId;
        }

        public void setDepartmentId(Long departmentId) {
            this.departmentId = departmentId;
        }
    }
}

package com.example.defensemanagement.controller.defense;

import com.example.defensemanagement.entity.ArchiveDetail;
import com.example.defensemanagement.entity.ArchiveSession;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.service.DefenseService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 答辩归档 Controller。
 */
@RestController
public class DefenseArchiveController extends AbstractDefenseController {

    public DefenseArchiveController(
            DefenseService defenseService,
            DefenseGroupTeacherMapper defenseGroupTeacherMapper,
            DefenseGroupMapper defenseGroupMapper,
            TeacherMapper teacherMapper,
            DepartmentMapper departmentMapper) {
        super(defenseService, defenseGroupTeacherMapper, defenseGroupMapper, teacherMapper, departmentMapper);
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
}

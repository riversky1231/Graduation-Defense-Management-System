package com.example.defensemanagement.controller.score;

import com.example.defensemanagement.common.ApiResponse;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.ScoreService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 打分记录管理端点。
 */
@RestController
@RequestMapping("/defense/score")
public class ScoreRecordController extends AbstractScoreController {

    public ScoreRecordController(
            ScoreService scoreService,
            AuthService authService,
            TeacherScoreRecordMapper teacherScoreRecordMapper,
            DefenseGroupTeacherMapper defenseGroupTeacherMapper,
            TeacherMapper teacherMapper,
            StudentMapper studentMapper,
            ConfigService configService) {
        super(scoreService, authService, teacherScoreRecordMapper, defenseGroupTeacherMapper,
                teacherMapper, studentMapper, configService);
    }

    @GetMapping("/records/list")
    @ResponseBody
    public ApiResponse<List<TeacherScoreRecord>> getAllScoreRecords(
            @RequestParam(required = false) Integer year,
            HttpSession session) {
        if (!hasSuperAdminAccess(session)) {
            return errorResponse("权限不足");
        }
        List<TeacherScoreRecord> records = year != null
                ? teacherScoreRecordMapper.findByYear(year)
                : teacherScoreRecordMapper.findAll();
        return successResponse("查询成功", records);
    }

    @GetMapping("/records/{id}")
    @ResponseBody
    public ApiResponse<TeacherScoreRecord> getScoreRecordById(@PathVariable Long id, HttpSession session) {
        if (!hasSuperAdminAccess(session)) {
            return errorResponse("权限不足");
        }
        TeacherScoreRecord record = teacherScoreRecordMapper.findById(id);
        if (record == null) {
            return errorResponse("记录不存在");
        }
        return successResponse("查询成功", record);
    }

    @PutMapping("/records/update")
    @ResponseBody
    public ApiResponse<Void> updateScoreRecord(@RequestBody TeacherScoreRecord record, HttpSession session) {
        if (!hasSuperAdminAccess(session)) {
            return errorResponse("权限不足");
        }

        try {
            return teacherScoreRecordMapper.update(record) > 0
                    ? successResponse("更新成功")
                    : errorResponse("更新失败");
        } catch (Exception e) {
            return errorResponse(safeMessage(e, "更新评分记录失败"));
        }
    }

    @DeleteMapping("/records/{id}")
    @ResponseBody
    public ApiResponse<Void> deleteScoreRecord(@PathVariable Long id, HttpSession session) {
        if (!hasSuperAdminAccess(session)) {
            return errorResponse("权限不足");
        }

        try {
            return teacherScoreRecordMapper.deleteById(id) > 0
                    ? successResponse("删除成功")
                    : errorResponse("删除失败");
        } catch (Exception e) {
            return errorResponse(safeMessage(e, "删除评分记录失败"));
        }
    }
}

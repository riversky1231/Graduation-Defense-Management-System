package com.example.defensemanagement.mapper;

import com.example.defensemanagement.entity.StudentPreference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface StudentPreferenceMapper {
    StudentPreference findByStudentIdAndYear(@Param("studentId") Long studentId, @Param("year") Integer year);

    int insert(StudentPreference preference);

    int update(StudentPreference preference);

    int updateAdminAssignment(@Param("studentId") Long studentId,
                              @Param("year") Integer year,
                              @Param("adminAssignType") Integer adminAssignType,
                              @Param("adminAssignedTeacherId") Long adminAssignedTeacherId);

    List<Map<String, Object>> findByTeacherAndYearAndRound(@Param("teacherId") Long teacherId,
                                                           @Param("year") Integer year,
                                                           @Param("round") Integer round);

    List<Map<String, Object>> findAllByTeacherAndYear(@Param("teacherId") Long teacherId,
                                                      @Param("year") Integer year);

    List<Map<String, Object>> findByDepartmentAndYear(@Param("departmentId") Long departmentId,
                                                      @Param("year") Integer year,
                                                      @Param("unassignedOnly") Boolean unassignedOnly);
}

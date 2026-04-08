package com.example.defensemanagement.mapper;

import com.example.defensemanagement.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface StudentMapper {

    // 基础 CRUD
    Student findById(Long id);

    int insert(Student student);

    int update(Student student);

    int deleteById(@Param("id") Long id);

    /**
     * 仅更新 defense_group_id，允许置空
     */
    int updateDefenseGroupId(@Param("id") Long id, @Param("groupId") Long groupId);

    /**
     * 仅更新 advisor_teacher_id，允许置空
     */
    int updateAdvisorTeacherId(@Param("id") Long id, @Param("advisorTeacherId") Long advisorTeacherId);

    // 查询学生列表
    List<Student> findAll(); // 【补充】Controller中调用，需要获取所有学生列表

    // 根据学号和年份查找
    Student findByStudentNoAndYear(@Param("studentNo") String studentNo, @Param("year") Integer year);

    Student findLatestByStudentNo(@Param("studentNo") String studentNo);

    // 根据院系和年份查找 【补充】院系管理员需要此方法
    List<Student> findByDepartmentAndYear(@Param("departmentId") Long departmentId, @Param("year") Integer year);

    // 根据指导教师和年份查找
    List<Student> findByAdvisorIdAndYear(@Param("advisorId") Long advisorId, @Param("year") Integer year);

    // 根据评阅人和年份查找
    List<Student> findByReviewerIdAndYear(@Param("reviewerId") Long reviewerId, @Param("year") Integer year);

    // 根据答辩小组ID查找
    List<Student> findByDefenseGroupId(@Param("groupId") Long groupId);

    // 批量：根据多个答辩小组ID查找（按年份过滤）
    List<Student> findByDefenseGroupIdsAndYear(@Param("groupIds") List<Long> groupIds, @Param("year") Integer year);

    // 根据年份查找所有学生（超级管理员使用）
    List<Student> findByYear(@Param("year") Integer year);

    // 获取所有年份列表（去重，用于年份管理）
    List<Integer> findAllYears();
    
    // 搜索学生（支持分页）
    List<Student> searchStudents(@Param("keyword") String keyword,
                                  @Param("departmentId") Long departmentId,
                                  @Param("year") Integer year,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);
    
    // 统计学生数量（用于分页）
    int countStudents(@Param("keyword") String keyword,
                      @Param("departmentId") Long departmentId,
                      @Param("year") Integer year);

    int countByAdvisorAndYear(@Param("advisorTeacherId") Long advisorTeacherId,
                              @Param("year") Integer year);

    int updateAvatarPath(@Param("id") Long id, @Param("avatarPath") String avatarPath);
}

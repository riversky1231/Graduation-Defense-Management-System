package com.example.defensemanagement.mapper;

import com.example.defensemanagement.entity.LargeGroupScore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 大组答辩成绩 Mapper
 */
@Mapper
public interface LargeGroupScoreMapper {

    /**
     * 根据学生ID、教师ID和年份查找记录
     */
    LargeGroupScore findByStudentIdAndTeacherIdAndYear(
            @Param("studentId") Long studentId,
            @Param("teacherId") Long teacherId,
            @Param("year") Integer year);

    /**
     * 根据学生ID和年份查找所有教师的打分记录
     */
    List<LargeGroupScore> findByStudentIdAndYear(
            @Param("studentId") Long studentId,
            @Param("year") Integer year);

    /**
     * 批量：根据学生ID列表和年份查找所有打分记录
     */
    List<LargeGroupScore> findByStudentIdsAndYear(
            @Param("studentIds") List<Long> studentIds,
            @Param("year") Integer year);

    /**
     * 根据年份查找所有大组答辩记录
     */
    List<LargeGroupScore> findByYear(@Param("year") Integer year);

    /**
     * 根据主键查找记录
     */
    LargeGroupScore findById(@Param("id") Long id);

    /**
     * 插入新记录
     */
    int insert(LargeGroupScore record);

    /**
     * 更新记录
     */
    int update(LargeGroupScore record);

    /**
     * 删除记录
     */
    int deleteById(@Param("id") Long id);
}

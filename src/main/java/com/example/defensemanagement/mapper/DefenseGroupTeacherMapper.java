package com.example.defensemanagement.mapper;

import com.example.defensemanagement.entity.DefenseGroupTeacher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DefenseGroupTeacherMapper {

    List<DefenseGroupTeacher> findByGroupId(@Param("groupId") Long groupId);

    DefenseGroupTeacher findLeaderByGroupId(@Param("groupId") Long groupId);

    int insert(@Param("groupId") Long groupId, @Param("teacherId") Long teacherId, @Param("isLeader") Integer isLeader);

    int delete(@Param("groupId") Long groupId, @Param("teacherId") Long teacherId);

    int clearLeader(@Param("groupId") Long groupId);

    int setLeader(@Param("groupId") Long groupId, @Param("teacherId") Long teacherId);

    /**
     * 根据教师ID查找该教师所属的小组信息
     */
    DefenseGroupTeacher findByTeacherId(@Param("teacherId") Long teacherId);

    /**
     * 获取所有小组的教师关联（用于大组答辩时所有教师打分）
     */
    List<DefenseGroupTeacher> findAll();

    /**
     * 批量获取指定小组的教师关联。
     */
    List<DefenseGroupTeacher> findByGroupIds(@Param("groupIds") List<Long> groupIds);
}



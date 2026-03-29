package com.example.defensemanagement.mapper;

import com.example.defensemanagement.entity.Teacher;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TeacherMapper {
    
    Teacher findByTeacherNo(String teacherNo);
    
    @Select("SELECT * FROM teacher WHERE id = #{id}")
    @Results({
        @Result(property = "teacherNo", column = "teacher_no"),
        @Result(property = "departmentId", column = "department_id"),
        @Result(property = "userId", column = "user_id")
    })
    Teacher findById(Long id);
    
    @Select("<script>" +
            "SELECT * FROM teacher " +
            "<where>" +
            "  <if test='departmentId != null'> department_id = #{departmentId} </if>" +
            "</where>" +
            "</script>")
    @Results({
        @Result(property = "teacherNo", column = "teacher_no"),
        @Result(property = "departmentId", column = "department_id"),
        @Result(property = "userId", column = "user_id")
    })
    List<Teacher> findByDepartmentId(Long departmentId);
    
    @Insert("INSERT INTO teacher (teacher_no, name, department_id, title, email, phone, password, user_id) " +
            "VALUES (#{teacherNo}, #{name}, #{departmentId}, #{title}, #{email}, #{phone}, #{password}, #{userId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Teacher teacher);
    
    @Update("UPDATE teacher SET password = #{password}, updated_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);
    
    @Update("UPDATE teacher SET name = #{name}, department_id = #{departmentId}, title = #{title}, " +
            "email = #{email}, phone = #{phone}, user_id = #{userId}, updated_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int update(Teacher teacher);

    @Update("UPDATE teacher SET avatar_path = #{avatarPath}, updated_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateAvatar(@Param("id") Long id, @Param("avatarPath") String avatarPath);
    
    @Update("UPDATE teacher SET status = #{status}, updated_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    
    @Update("UPDATE teacher SET teacher_no = #{teacherNo}, updated_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateTeacherNo(@Param("id") Long id, @Param("teacherNo") String teacherNo);
    
    @Select("SELECT * FROM teacher WHERE user_id = #{userId}")
    @Results({
        @Result(property = "teacherNo", column = "teacher_no"),
        @Result(property = "departmentId", column = "department_id"),
        @Result(property = "userId", column = "user_id")
    })
    Teacher findByUserId(Long userId);
    
    @Delete("DELETE FROM teacher WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
    
    @Delete("DELETE FROM teacher WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
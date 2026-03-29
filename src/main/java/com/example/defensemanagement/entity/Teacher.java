package com.example.defensemanagement.entity;

import lombok.Data;

@Data
public class Teacher {
    private Long id;
    private String teacherNo;
    private String name;
    private Long departmentId;
    private String title;
    private String email;
    private String phone;
    private String password;
    private Integer status; // 1-启用，0-禁用
    private Long userId; // 关联的用户ID（user表）
    
   private String avatarPath; // 头像路径
   
    // 关联对象
    private Department department;
    
    // 临时属性：用于在小组中显示是否为组长（从 DefenseGroupTeacher 获取）
    private Integer isLeader;
}
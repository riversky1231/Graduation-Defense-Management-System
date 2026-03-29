package com.example.defensemanagement.entity;

import lombok.Data;

@Data
public class Student {
    private Long id;
    private String studentNo; // 学号
    private String name; // 姓名
    private String classInfo; // 班级
    private Long departmentId; // 所属院系ID
    private Long advisorTeacherId; // 指导教师ID
    private Long reviewerTeacherId; // 评阅人ID
    private String defenseType; // 毕业考核类型: PAPER(论文) 或 DESIGN(设计)
    private String title; // 毕业考核题目
    private String summary; // 毕业考核摘要
    private Long defenseGroupId; // 学生所在答辩小组ID
    private Integer defenseYear; // 答辩年份
    private String phone; // 学生联系电话
    private String email; // 学生邮箱
    private java.sql.Date defenseDate; // 答辩日期
   private String avatarPath; // 头像路径

    // 关联对象 (用于查询显示)
    private Teacher advisor;
    private Teacher reviewer;
    private Department department;
    private DefenseGroup defenseGroup;
}

package com.example.defensemanagement.service;

import com.example.defensemanagement.common.StudentMaterialContext;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentPreference;

public interface VolunteerMaterialService {

    StudentMaterialContext buildStudentContext(Student student, StudentPreference preference, Long teacherId);
}

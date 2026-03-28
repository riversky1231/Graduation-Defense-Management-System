package com.example.defensemanagement.service;

import com.example.defensemanagement.common.MatchScoreDetail;
import com.example.defensemanagement.common.RelevanceAnalysisResult;
import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentPreference;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.TeacherProfile;

import java.util.Map;

public interface VolunteerMatchService {

    default double calculateMatchScore(StudentPreference preference,
                                       Student student,
                                       Teacher teacher,
                                       TeacherProfile teacherProfile,
                                       int remainingCapacity,
                                       int assignedCount,
                                       int maxStudents,
                                       Map<String, RelevanceAnalysisResult> relevanceCache) {
        return calculateMatchDetail(preference, student, teacher, teacherProfile, remainingCapacity,
                assignedCount, maxStudents, relevanceCache).getTotalScore();
    }

    MatchScoreDetail calculateMatchDetail(StudentPreference preference,
                                          Student student,
                                          Teacher teacher,
                                          TeacherProfile teacherProfile,
                                          int remainingCapacity,
                                          int assignedCount,
                                          int maxStudents,
                                          Map<String, RelevanceAnalysisResult> relevanceCache);
}

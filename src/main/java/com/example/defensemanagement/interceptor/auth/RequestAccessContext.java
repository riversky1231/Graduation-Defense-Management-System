package com.example.defensemanagement.interceptor.auth;

import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;

public final class RequestAccessContext {

    private final String path;
    private final String method;
    private final User currentUser;
    private final Teacher currentTeacher;

    public RequestAccessContext(String path, String method, User currentUser, Teacher currentTeacher) {
        this.path = path;
        this.method = method;
        this.currentUser = currentUser;
        this.currentTeacher = currentTeacher;
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public Teacher getCurrentTeacher() {
        return currentTeacher;
    }

    public String getRoleName() {
        if (currentUser == null || currentUser.getRole() == null) {
            return null;
        }
        return currentUser.getRole().getName();
    }

    public boolean hasCurrentTeacher() {
        return currentTeacher != null;
    }

    public boolean hasAnyRole(String... roleNames) {
        String roleName = getRoleName();
        if (roleName == null) {
            return false;
        }
        for (String candidate : roleNames) {
            if (candidate.equals(roleName)) {
                return true;
            }
        }
        return false;
    }
}

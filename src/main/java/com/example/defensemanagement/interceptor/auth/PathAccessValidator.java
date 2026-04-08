package com.example.defensemanagement.interceptor.auth;

public interface PathAccessValidator {

    AccessDecision validate(RequestAccessContext context);
}

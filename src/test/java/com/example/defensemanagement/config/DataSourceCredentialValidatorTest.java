package com.example.defensemanagement.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataSourceCredentialValidatorTest {

    @Test
    void validate_rejectsBlankCredentials() {
        DataSourceCredentialValidator validator = new DataSourceCredentialValidator();
        ReflectionTestUtils.setField(validator, "username", "");
        ReflectionTestUtils.setField(validator, "password", " ");

        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    void validate_acceptsConfiguredCredentials() {
        DataSourceCredentialValidator validator = new DataSourceCredentialValidator();
        ReflectionTestUtils.setField(validator, "username", "db_user");
        ReflectionTestUtils.setField(validator, "password", "db_password");

        assertDoesNotThrow(validator::validate);
    }
}

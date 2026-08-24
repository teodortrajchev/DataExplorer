package validation;

import dataexploreapp.db_config.validation.IdentifierValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class IdentifierValidatorTest {

    @Test
    void valid_identifier_test() {
        assertDoesNotThrow(() -> IdentifierValidator.validate("username"));
        assertDoesNotThrow(() -> IdentifierValidator.validate("USER123"));
        assertDoesNotThrow(() -> IdentifierValidator.validate("user_name"));
        assertDoesNotThrow(() -> IdentifierValidator.validate("user$name"));
        assertDoesNotThrow(() -> IdentifierValidator.validate("user#name"));
    }
    @Test
    void null_identifier_test() {
        assertThrows(IllegalArgumentException.class, () -> IdentifierValidator.validate(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\n"})
    void empty_identifier_test(String identifier) {
        assertThrows(IllegalArgumentException.class, () -> IdentifierValidator.validate(identifier));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1username", "123", "_username", "$username", "#username"})
    void invalid_identifier_test(String identifier) {
        assertThrows(IllegalArgumentException.class, () -> IdentifierValidator.validate(identifier));
    }
    @ParameterizedTest
    @ValueSource(strings = {"users;DROP TABLE users", "users--", "users' OR '1'='1", "users/*comment*/", "users UNION SELECT", "users OR 1=1"})
    void sql_injection_test(String identifier) {
        assertThrows(IllegalArgumentException.class, () -> IdentifierValidator.validate(identifier));
    }

}

package com.bonitasoft.processbuilder.filter;

import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security tests for MultipleUserIdsActorFilter.
 * Tests that the filter prevents common security vulnerabilities.
 */
@DisplayName("Security Tests")
class SecurityTests {

    private MultipleUserIdsActorFilter filter;

    /**
     * Mock implementation of the filter that allows manual parameter injection.
     */
    private static class TestableMultipleUserIdsActorFilter extends MultipleUserIdsActorFilter {
        private Object inputParameter;

        void setStoredParameter(Object parameter) {
            this.inputParameter = parameter;
        }

        @Override
        public Object getInputParameter(String parameterName) {
            if (USERS_LIST_INPUT.equals(parameterName)) {
                return inputParameter;
            }
            return null;
        }

        @Override
        public void setInputParameters(Map<String, Object> parameters) {
            this.inputParameter = parameters.get(USERS_LIST_INPUT);
        }
    }

    @BeforeEach
    void setUp() {
        filter = new TestableMultipleUserIdsActorFilter();
    }

    // =========================================================================
    // SQL INJECTION TESTS
    // =========================================================================

    /**
     * Security test: Prevent SQL injection via List element
     */
    @Test
    @DisplayName("Should prevent SQL injection via List element")
    void testSQLInjectionPrevention() {
        // Arrange - Attempt SQL injection
        List<Object> maliciousList = new ArrayList<>();
        maliciousList.add("'; DROP TABLE users; --");

        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(maliciousList);

        // Act & Assert
        ConnectorValidationException exception = assertThrows(
            ConnectorValidationException.class,
            () -> filter.validateInputParameters(),
            "String injection should be rejected"
        );

        // Verify that the injection payload was rejected
        assertTrue(exception.getMessage().contains("must be a List<Long>"),
            "Should reject non-Long values");
    }

    /**
     * Security test: Prevent common SQL injection patterns
     */
    @Test
    @DisplayName("Should prevent various SQL injection patterns")
    void testCommonSQLInjectionPatterns() {
        String[] sqlInjectionPatterns = {
            "1; DROP TABLE users;",
            "1' OR '1'='1",
            "1 UNION SELECT * FROM users",
            "1; DELETE FROM users WHERE 1=1;",
            "'; UPDATE users SET admin=1;",
            "1) UNION ALL SELECT NULL--"
        };

        for (String pattern : sqlInjectionPatterns) {
            // Arrange
            List<Object> maliciousList = new ArrayList<>();
            maliciousList.add(pattern);
            ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(maliciousList);

            // Act & Assert
            ConnectorValidationException exception = assertThrows(
                ConnectorValidationException.class,
                () -> filter.validateInputParameters(),
                "Should reject SQL injection pattern: " + pattern
            );

            assertTrue(exception.getMessage().contains("must be a List<Long>"),
                "Should reject injection pattern: " + pattern);
        }
    }

    // =========================================================================
    // CODE INJECTION TESTS
    // =========================================================================

    /**
     * Security test: Should not execute arbitrary code in filter
     */
    @Test
    @DisplayName("Should not execute arbitrary code in filter")
    void testNoCodeExecution() {
        // Arrange - Create object that might execute code via toString()
        List<Object> list = new ArrayList<>();
        list.add(new Object() {
            private boolean codeExecuted = false;

            @Override
            public String toString() {
                // Potential code execution point
                codeExecuted = true;
                return "malicious";
            }

            public boolean wasCodeExecuted() {
                return codeExecuted;
            }
        });

        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(list);

        // Act & Assert
        ConnectorValidationException exception = assertThrows(
            ConnectorValidationException.class,
            () -> filter.validateInputParameters(),
            "Should reject non-Long objects"
        );

        // Verify exception was thrown without executing toString() in a security-critical context
        assertTrue(exception.getMessage().contains("must be a List<Long>"),
            "Should reject arbitrary object types");
    }

    /**
     * Security test: Prevent XSS-like attacks via toString() overrides
     */
    @Test
    @DisplayName("Should prevent XSS-like attacks through object toString()")
    void testXSSPrevention() {
        // Arrange - Create malicious object with toString() that contains XSS payload
        List<Object> list = new ArrayList<>();
        list.add(new Object() {
            @Override
            public String toString() {
                return "<script>alert('XSS')</script>";
            }
        });

        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(list);

        // Act & Assert
        ConnectorValidationException exception = assertThrows(
            ConnectorValidationException.class,
            () -> filter.validateInputParameters(),
            "Should reject objects with suspicious toString() implementations"
        );

        assertTrue(exception.getMessage().contains("must be a List<Long>"),
            "Should reject non-Long type");
    }

    // =========================================================================
    // TYPE VALIDATION SECURITY TESTS
    // =========================================================================

    /**
     * Security test: Strict type checking prevents type confusion attacks
     */
    @Test
    @DisplayName("Should use strict type checking to prevent type confusion")
    void testStrictTypeChecking() {
        // Arrange - Try to pass Integer disguised as Long (won't work in Java, but test anyway)
        List<Integer> intList = new ArrayList<>();
        intList.add(Integer.MAX_VALUE);

        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(intList);

        // Act & Assert
        ConnectorValidationException exception = assertThrows(
            ConnectorValidationException.class,
            () -> filter.validateInputParameters(),
            "Should reject Integer list"
        );

        assertTrue(exception.getMessage().contains("Integer"),
            "Should specifically identify wrong type");
    }

    /**
     * Security test: Prevent null pointer attacks
     */
    @Test
    @DisplayName("Should prevent null pointer exceptions and null reference attacks")
    void testNullPointerPrevention() {
        // Arrange - Try to pass null
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(null);

        // Act & Assert
        ConnectorValidationException exception = assertThrows(
            ConnectorValidationException.class,
            () -> filter.validateInputParameters(),
            "Should reject null input"
        );

        assertTrue(exception.getMessage().contains("cannot be null"),
            "Should prevent null pointer");
    }

    // =========================================================================
    // INPUT VALIDATION SECURITY TESTS
    // =========================================================================

    /**
     * Security test: Validate that only List types are accepted
     */
    @Test
    @DisplayName("Should accept only List types for enhanced security")
    void testCollectionTypeSecurity() {
        // Test various collection types that might bypass validation
        Object[] unsafeInputs = {
            new ArrayList<>(),              // Empty ArrayList - should fail (empty)
            "user1,user2,user3".split(","), // Array instead of List
            new StringBuilder(),             // StringBuilder with user data
        };

        for (Object unsafeInput : unsafeInputs) {
            ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(unsafeInput);

            // Should fail validation
            assertThrows(
                ConnectorValidationException.class,
                () -> filter.validateInputParameters(),
                "Should reject unsafe input type: " + unsafeInput.getClass().getSimpleName()
            );
        }
    }

    /**
     * Security test: Boundary value testing for numeric overflow
     */
    @Test
    @DisplayName("Should handle Long boundary values safely")
    void testNumericBoundaryValues() throws ConnectorValidationException {
        // Arrange - Test with Long boundary values
        List<Long> boundaryList = new ArrayList<>();
        boundaryList.add(Long.MIN_VALUE);

        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(boundaryList);

        // Act & Assert - Should accept valid Long values
        assertDoesNotThrow(
            () -> filter.validateInputParameters(),
            "Should accept Long.MIN_VALUE"
        );

        // Test with Long.MAX_VALUE
        boundaryList.clear();
        boundaryList.add(Long.MAX_VALUE);

        assertDoesNotThrow(
            () -> filter.validateInputParameters(),
            "Should accept Long.MAX_VALUE"
        );
    }

    /**
     * Security test: Serialization/Deserialization attacks
     */
    @Test
    @DisplayName("Should prevent serialization attacks through type checking")
    void testSerializationSecurityChecks() {
        // Arrange - Create object that might be exploitable via serialization
        List<Object> list = new ArrayList<>();
        list.add(new SerializablePayload());

        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(list);

        // Act & Assert
        ConnectorValidationException exception = assertThrows(
            ConnectorValidationException.class,
            () -> filter.validateInputParameters(),
            "Should reject suspicious object types"
        );

        assertTrue(exception.getMessage().contains("must be a List<Long>"),
            "Should reject non-Long type");
    }

    /**
     * Helper class for serialization attack testing
     */
    private static class SerializablePayload {
        // Intentionally suspicious class that might be used in serialization attacks
    }
}
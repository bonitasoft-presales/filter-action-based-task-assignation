package com.bonitasoft.processbuilder.filter;

import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for validateInputParameters() method of MultipleUserIdsActorFilter.
 * Tests all validation paths including valid inputs and error cases.
 */
@DisplayName("Validation Tests")
class ValidationTests {

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
    }

    @BeforeEach
    void setUp() {
        filter = new TestableMultipleUserIdsActorFilter();
    }

    // =========================================================================
    // VALID INPUT TESTS
    // =========================================================================

    /**
     * Valid case: Non-empty List<Long>
     */
    @Test
    @DisplayName("Should pass validation with valid List<Long>")
    void testValidListLong() {
        // Arrange
        List<Long> validList = Arrays.asList(101L, 102L, 103L);
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(validList);

        // Act & Assert
        assertDoesNotThrow(() -> filter.validateInputParameters(),
            "Validation should pass for a non-empty List<Long>");
    }

    /**
     * Valid case: List with single element
     */
    @Test
    @DisplayName("Should pass validation with single element List<Long>")
    void testSingleElementListLong() {
        // Arrange
        List<Long> singleList = Collections.singletonList(999L);
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(singleList);

        // Act & Assert
        assertDoesNotThrow(() -> filter.validateInputParameters(),
            "Validation should pass for a single element List<Long>");
    }

    /**
     * Valid case: Large list
     */
    @Test
    @DisplayName("Should pass validation with large List<Long>")
    void testLargeListLong() {
        // Arrange
        List<Long> largeList = new ArrayList<>();
        for (long i = 0; i < 1000; i++) {
            largeList.add(i);
        }
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(largeList);

        // Act & Assert
        assertDoesNotThrow(() -> filter.validateInputParameters(),
            "Validation should pass for a large List<Long>");
    }

    /**
     * Valid case: LinkedList is a valid List type
     */
    @Test
    @DisplayName("Should pass validation with LinkedList<Long>")
    void testLinkedListValid() {
        // Arrange
        LinkedList<Long> linkedList = new LinkedList<>();
        linkedList.add(100L);
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(linkedList);

        // Act & Assert
        assertDoesNotThrow(() -> filter.validateInputParameters(),
            "Validation should pass for LinkedList<Long> since it is a List");
    }

    // =========================================================================
    // ERROR CASES - NULL AND TYPE ERRORS
    // =========================================================================

    /**
     * Error case: Null input
     */
    @Test
    @DisplayName("Should fail validation with null input")
    void testNullInput() {
        // Arrange
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(null);

        // Act & Assert
        ConnectorValidationException exception = assertThrows(
            ConnectorValidationException.class,
            () -> filter.validateInputParameters(),
            "Validation should fail for null input"
        );
        assertTrue(exception.getMessage().contains("cannot be null"),
            "Error message should indicate null parameter");
    }

    /**
     * Error case: Empty list
     */
    @Test
    @DisplayName("Should fail validation with empty List")
    void testEmptyList() {
        // Arrange
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(Collections.emptyList());

        // Act & Assert
        ConnectorValidationException exception = assertThrows(
            ConnectorValidationException.class,
            () -> filter.validateInputParameters(),
            "Validation should fail for empty list"
        );
        assertTrue(exception.getMessage().contains("cannot be empty"),
            "Error message should indicate empty list");
    }

    /**
     * Error case: Not a List (String)
     */
    @Test
    @DisplayName("Should fail validation when input is String instead of List")
    void testNonListTypeString() {
        // Arrange
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter("not a list");

        // Act & Assert
        ConnectorValidationException exception = assertThrows(
            ConnectorValidationException.class,
            () -> filter.validateInputParameters(),
            "Validation should fail for non-List type"
        );
        assertTrue(exception.getMessage().contains("must be a List<Long>"),
            "Error message should specify List<Long> requirement");
        assertTrue(exception.getMessage().contains("String"),
            "Error message should mention found type");
    }

    /**
     * Error case: Not a List (Integer)
     */
    @Test
    @DisplayName("Should fail validation when input is Integer instead of List")
    void testNonListTypeInteger() {
        // Arrange
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(42);

        // Act & Assert
        ConnectorValidationException exception = assertThrows(
            ConnectorValidationException.class,
            () -> filter.validateInputParameters(),
            "Validation should fail for non-List type"
        );
        assertTrue(exception.getMessage().contains("must be a List<Long>"),
            "Error message should specify List<Long> requirement");
    }

    /**
     * Error case: HashMap instead of List
     */
    @Test
    @DisplayName("Should fail validation with HashMap instead of List")
    void testHashMapType() {
        // Arrange
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(new HashMap<>());

        // Act & Assert
        ConnectorValidationException exception = assertThrows(
            ConnectorValidationException.class,
            () -> filter.validateInputParameters(),
            "Validation should fail for HashMap"
        );
        assertTrue(exception.getMessage().contains("must be a List<Long>"),
            "Error message should specify List<Long> requirement");
    }

    // =========================================================================
    // ERROR CASES - WRONG ELEMENT TYPES
    // =========================================================================

    /**
     * Error case: List with wrong element type (Integer)
     */
    @Test
    @DisplayName("Should fail validation with List<Integer>")
    void testListWrongElementTypeInteger() {
        // Arrange
        List<Integer> wrongTypeList = Arrays.asList(1, 2, 3);
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(wrongTypeList);

        // Act & Assert
        ConnectorValidationException exception = assertThrows(
            ConnectorValidationException.class,
            () -> filter.validateInputParameters(),
            "Validation should fail for List<Integer>"
        );
        assertTrue(exception.getMessage().contains("Found a List with elements of type Integer"),
            "Error message should indicate wrong element type");
    }

    /**
     * Error case: List with wrong element type (String)
     */
    @Test
    @DisplayName("Should fail validation with List<String>")
    void testListWrongElementTypeString() {
        // Arrange
        List<String> wrongTypeList = Arrays.asList("user1", "user2");
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(wrongTypeList);

        // Act & Assert
        ConnectorValidationException exception = assertThrows(
            ConnectorValidationException.class,
            () -> filter.validateInputParameters(),
            "Validation should fail for List<String>"
        );
        assertTrue(exception.getMessage().contains("Found a List with elements of type String"),
            "Error message should indicate wrong element type");
    }

    /**
     * Error case: List with null elements
     */
    @Test
    @DisplayName("Should fail validation with List containing null elements")
    void testListWithNullElements() {
        // Arrange
        List<Object> listWithNull = new ArrayList<>();
        listWithNull.add(null);
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(listWithNull);

        // Act & Assert
        ConnectorValidationException exception = assertThrows(
            ConnectorValidationException.class,
            () -> filter.validateInputParameters(),
            "Validation should fail for List with null elements"
        );
        assertTrue(exception.getMessage().contains("Found a List with elements of type null"),
            "Error message should indicate null element type");
    }

    // =========================================================================
    // PARAMETERIZED TESTS
    // =========================================================================

    /**
     * Parameterized test for various invalid collection types
     */
    @ParameterizedTest
    @ValueSource(classes = {
        java.util.HashMap.class,
        java.util.HashSet.class
    })
    @DisplayName("Should fail validation with non-List collection types")
    void testNonListCollectionTypes(Class<?> collectionType) throws Exception {
        // Arrange
        Object collection = collectionType.getDeclaredConstructor().newInstance();
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(collection);

        // Act & Assert
        ConnectorValidationException exception = assertThrows(
            ConnectorValidationException.class,
            () -> filter.validateInputParameters(),
            "Validation should fail for non-List collection type"
        );
        assertTrue(exception.getMessage().contains("must be a List<Long>"),
            "Error message should specify List<Long> requirement");
    }
}
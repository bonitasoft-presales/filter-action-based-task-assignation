package com.bonitasoft.processbuilder.filter;

import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.bonitasoft.engine.filter.UserFilterException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for MultipleUserIdsActorFilter with maximum code coverage.
 * Uses nested test classes for logical organization and parameterized tests for edge cases.
 */
@DisplayName("MultipleUserIdsActorFilter Test Suite")
class MultipleUserIdsActorFilterTest {

    private MultipleUserIdsActorFilter filter;
    private Object storedInputParameter;

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
    // NESTED CLASS: VALIDATION TESTS
    // =========================================================================
    @Nested
    @DisplayName("validateInputParameters() Tests")
    class ValidateInputParametersTests {

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

        // --- Error Cases ---

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

        /**
         * Parameterized test for various invalid types
         * Note: LinkedList is a List, so it won't fail on instanceof check
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

        /**
         * Valid case: LinkedList is a valid List type
         */
        @Test
        @DisplayName("Should pass validation with LinkedList<Long>")
        void testLinkedListValid() {
            // Arrange
            java.util.LinkedList<Long> linkedList = new java.util.LinkedList<>();
            linkedList.add(100L);
            ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(linkedList);

            // Act & Assert
            assertDoesNotThrow(() -> filter.validateInputParameters(),
                "Validation should pass for LinkedList<Long> since it is a List");
        }
    }

    // =========================================================================
    // NESTED CLASS: FILTER TESTS
    // =========================================================================
    @Nested
    @DisplayName("filter() Tests")
    class FilterTests {

        /**
         * Success case: Return validated List<Long>
         */
        @Test
        @DisplayName("Should return input list successfully")
        void testFilterSuccess() throws UserFilterException {
            // Arrange
            List<Long> expectedUserIds = Arrays.asList(400L, 500L, 600L);
            ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(expectedUserIds);

            // Act
            List<Long> result = filter.filter("testActor");

            // Assert
            assertNotNull(result, "Result should not be null");
            assertEquals(expectedUserIds, result, "Result should match input list");
            assertEquals(3, result.size(), "Result should have correct size");
        }

        /**
         * Success case: Single element
         */
        @Test
        @DisplayName("Should return single element list")
        void testFilterSingleElement() throws UserFilterException {
            // Arrange
            List<Long> singleElement = Collections.singletonList(12345L);
            ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(singleElement);

            // Act
            List<Long> result = filter.filter("admin");

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(12345L, result.get(0));
        }

        /**
         * Success case: Large list
         */
        @Test
        @DisplayName("Should return large list successfully")
        void testFilterLargeList() throws UserFilterException {
            // Arrange
            List<Long> largeList = new ArrayList<>();
            for (long i = 1; i <= 10000; i++) {
                largeList.add(i);
            }
            ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(largeList);

            // Act
            List<Long> result = filter.filter("powerUser");

            // Assert
            assertNotNull(result);
            assertEquals(10000, result.size());
            assertEquals(largeList, result);
        }

        /**
         * Success case: Different actor names
         */
        @ParameterizedTest
        @ValueSource(strings = {"manager", "admin", "supervisor", "director", "custom-role"})
        @DisplayName("Should work with various actor names")
        void testFilterWithDifferentActorNames(String actorName) throws UserFilterException {
            // Arrange
            List<Long> userIds = Arrays.asList(100L, 200L);
            ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(userIds);

            // Act
            List<Long> result = filter.filter(actorName);

            // Assert
            assertNotNull(result);
            assertEquals(userIds, result);
        }

        // --- Error Cases ---

        /**
         * Error case: ClassCastException (corrupted data)
         */
        @Test
        @DisplayName("Should throw UserFilterException when data is corrupted (ClassCastException)")
        void testFilterCorruptedData() {
            // Arrange: Simulate corrupted data
            ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter("corrupted data");

            // Act & Assert
            UserFilterException exception = assertThrows(
                UserFilterException.class,
                () -> filter.filter("testActor"),
                "Should throw UserFilterException on corrupted data"
            );
            assertNotNull(exception.getCause(), "Exception should have a cause");
            assertTrue(exception.getCause() instanceof ClassCastException,
                "Cause should be ClassCastException");
            assertTrue(exception.getMessage().contains("Failed to process validated user list input"),
                "Error message should describe the failure");
        }

        /**
         * Error case: Null input - filter returns null without exception
         * This is expected behavior since validation should be called first
         */
        @Test
        @DisplayName("Should return null when parameter is null (validation should prevent this)")
        void testFilterNullParameter() throws UserFilterException {
            // Arrange
            ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(null);

            // Act
            List<Long> result = filter.filter("testActor");

            // Assert - Returns null because validation wasn't called
            assertNull(result, "Should return null when parameter is null");
        }
    }

    // =========================================================================
    // NESTED CLASS: INPUT PARAMETER CONSTANT TESTS
    // =========================================================================
    @Nested
    @DisplayName("Input Parameter Constant Tests")
    class InputParameterConstantTests {

        /**
         * Verify the constant value is correct
         */
        @Test
        @DisplayName("USERS_LIST_INPUT constant should be 'usersList'")
        void testConstantValue() {
            assertEquals("usersList", MultipleUserIdsActorFilter.USERS_LIST_INPUT,
                "Constant should match expected value");
        }

        /**
         * Verify constant is used correctly in validation and filter
         */
        @Test
        @DisplayName("Constant should be used in both validation and filter methods")
        void testConstantUsage() throws UserFilterException {
            // Arrange
            List<Long> userIds = Arrays.asList(111L);
            Map<String, Object> parameters = new HashMap<>();
            parameters.put(MultipleUserIdsActorFilter.USERS_LIST_INPUT, userIds);

            // Act
            ((TestableMultipleUserIdsActorFilter) filter).setInputParameters(parameters);

            // Assert - Both should work with the same constant
            assertDoesNotThrow(() -> filter.validateInputParameters());
            assertEquals(userIds, filter.filter("test"));
        }
    }

    // =========================================================================
    // NESTED CLASS: INTEGRATION TESTS
    // =========================================================================
    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        /**
         * Full workflow: validation followed by filtering
         */
        @Test
        @DisplayName("Should execute complete validation and filter workflow")
        void testCompleteWorkflow() throws UserFilterException, ConnectorValidationException {
            // Arrange
            List<Long> userIds = Arrays.asList(50L, 60L, 70L);
            Map<String, Object> parameters = new HashMap<>();
            parameters.put(MultipleUserIdsActorFilter.USERS_LIST_INPUT, userIds);

            // Act
            ((TestableMultipleUserIdsActorFilter) filter).setInputParameters(parameters);
            filter.validateInputParameters();
            List<Long> result = filter.filter("manager");

            // Assert
            assertEquals(userIds, result);
        }

        /**
         * Multiple validations on same filter instance
         */
        @Test
        @DisplayName("Should handle multiple validations on same filter instance")
        void testMultipleValidations() {
            // Arrange
            List<Long> userIds1 = Arrays.asList(1L, 2L);
            List<Long> userIds2 = Arrays.asList(3L, 4L, 5L);

            // Act & Assert
            ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(userIds1);
            assertDoesNotThrow(() -> filter.validateInputParameters());

            ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(userIds2);
            assertDoesNotThrow(() -> filter.validateInputParameters());
        }

        /**
         * Multiple filter operations
         */
        @Test
        @DisplayName("Should support multiple filter operations")
        void testMultipleFilters() throws UserFilterException {
            // Arrange
            List<Long> userIds = Arrays.asList(100L, 200L, 300L);
            ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(userIds);

            // Act
            List<Long> result1 = filter.filter("actor1");
            List<Long> result2 = filter.filter("actor2");

            // Assert
            assertEquals(result1, result2);
            assertEquals(userIds, result1);
        }
    }
}
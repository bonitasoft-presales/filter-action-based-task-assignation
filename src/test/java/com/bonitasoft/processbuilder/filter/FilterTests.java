package com.bonitasoft.processbuilder.filter;

import org.bonitasoft.engine.filter.UserFilterException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for filter() method of MultipleUserIdsActorFilter.
 * Tests successful filtering and error handling.
 */
@DisplayName("Filter Tests")
class FilterTests {

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
    // SUCCESS CASES
    // =========================================================================

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
    @ValueSource(strings = {"manager", "admin", "supervisor", "director", "custom-role", "user", "guest", ""})
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

    /**
     * Success case: Actor name with special characters
     */
    @Test
    @DisplayName("Should work with actor name containing special characters")
    void testFilterWithSpecialCharactersActorName() throws UserFilterException {
        // Arrange
        List<Long> userIds = Arrays.asList(111L, 222L);
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(userIds);

        // Act
        List<Long> result = filter.filter("role-with_special.chars");

        // Assert
        assertNotNull(result);
        assertEquals(userIds, result);
    }

    // =========================================================================
    // ERROR CASES
    // =========================================================================

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
     * Error case: Null input - should throw UserFilterException
     * Validation should be called first to prevent this, but if it's not,
     * the filter method will catch the NullPointerException and wrap it
     */
    @Test
    @DisplayName("Should throw UserFilterException when parameter is null")
    void testFilterNullParameter() {
        // Arrange
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(null);

        // Act & Assert
        UserFilterException exception = assertThrows(
            UserFilterException.class,
            () -> filter.filter("testActor"),
            "Should throw UserFilterException when parameter is null"
        );
        
        // Verify exception details
        assertNotNull(exception.getCause(), "Exception should have a cause");
        assertTrue(exception.getMessage().contains("Failed to process validated user list input"),
            "Error message should describe the failure");
    }

    /**
     * Error case: Multiple casts in sequence
     */
    @Test
    @DisplayName("Should handle multiple sequential filter calls")
    void testMultipleSequentialCalls() throws UserFilterException {
        // Arrange
        List<Long> userIds = Arrays.asList(50L, 60L);
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(userIds);

        // Act - Call filter multiple times
        List<Long> result1 = filter.filter("actor1");
        List<Long> result2 = filter.filter("actor2");
        List<Long> result3 = filter.filter("actor3");

        // Assert
        assertEquals(userIds, result1);
        assertEquals(userIds, result2);
        assertEquals(userIds, result3);
    }

    /**
     * Error case: Integer instead of Long
     */
    @Test
    @DisplayName("Should throw UserFilterException when data type is Integer instead of List")
    void testFilterWithIntegerData() {
        // Arrange
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(12345);

        // Act & Assert
        UserFilterException exception = assertThrows(
            UserFilterException.class,
            () -> filter.filter("testActor")
        );
        
        assertNotNull(exception.getCause());
        assertTrue(exception.getMessage().contains("Failed to process validated user list input"));
    }

    /**
     * Error case: HashMap instead of List
     */
    @Test
    @DisplayName("Should throw UserFilterException when data is HashMap instead of List")
    void testFilterWithHashMapData() {
        // Arrange
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(new java.util.HashMap<String, Long>());

        // Act & Assert
        UserFilterException exception = assertThrows(
            UserFilterException.class,
            () -> filter.filter("testActor")
        );
        
        assertNotNull(exception.getCause());
    }

    /**
     * Success case: Boundary values
     */
    @Test
    @DisplayName("Should handle Long boundary values")
    void testFilterWithBoundaryValues() throws UserFilterException {
        // Arrange
        List<Long> boundaryValues = Arrays.asList(Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE);
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(boundaryValues);

        // Act
        List<Long> result = filter.filter("boundaryActor");

        // Assert
        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals(boundaryValues, result);
    }

    /**
     * Success case: Test logging with various scenarios
     */
    @Test
    @DisplayName("Should handle logging in successful filter execution")
    void testFilterLoggingOnSuccess() throws UserFilterException {
        // Arrange - This test ensures the LOGGER.info and LOGGER.debug lines execute
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(userIds);

        // Act
        List<Long> result = filter.filter("loggingTestActor");

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        // The logging itself is verified by code coverage, not assertions
    }

    /**
     * Error case: Test exception handling and logging
     */
    @Test
    @DisplayName("Should handle exception logging when cast fails")
    void testFilterExceptionLogging() {
        // Arrange - This test ensures the LOGGER.error line in catch block executes
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter("invalid");

        // Act & Assert
        UserFilterException exception = assertThrows(
            UserFilterException.class,
            () -> filter.filter("exceptionTestActor")
        );
        
        assertNotNull(exception);
        // The exception logging in catch block is verified by code coverage
    }
}
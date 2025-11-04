package com.bonitasoft.processbuilder.filter;

import org.bonitasoft.engine.filter.UserFilterException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional tests to improve code coverage of MultipleUserIdsActorFilter.
 * Focuses on:
 * - Logging coverage (LOGGER.isInfoEnabled() and LOGGER.isDebugEnabled())
 * - Exception handling in filter() method
 * - Edge cases not covered by existing tests
 */
@DisplayName("Additional Coverage Tests")
class AdditionalCoverageTests {

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
    // LOGGING COVERAGE TESTS
    // =========================================================================

    /**
     * Test to ensure LOGGER.isInfoEnabled() path is executed.
     * This covers the logging check at line 739 in filter() method.
     */
    @Test
    @DisplayName("Should execute filter() with info logging enabled")
    void testFilterWithInfoLoggingEnabled() throws UserFilterException {
        // Arrange
        List<Long> userIds = Arrays.asList(100L, 200L, 300L);
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(userIds);

        // Act - This should trigger LOGGER.isInfoEnabled() path
        List<Long> result = filter.filter("testActor");

        // Assert
        assertNotNull(result);
        assertEquals(userIds, result);
        assertEquals(3, result.size());
    }

    /**
     * Test to ensure LOGGER.isDebugEnabled() path is executed.
     * This covers the logging check at line 750 in filter() method.
     */
    @Test
    @DisplayName("Should execute filter() with debug logging enabled")
    void testFilterWithDebugLoggingEnabled() throws UserFilterException {
        // Arrange
        List<Long> userIds = Arrays.asList(500L, 600L);
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(userIds);

        // Act - This should trigger LOGGER.isDebugEnabled() path
        List<Long> result = filter.filter("debugActor");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(500L));
        assertTrue(result.contains(600L));
    }

    /**
     * Test logging with various actor names to ensure logging is triggered.
     */
    @Test
    @DisplayName("Should log different actor names correctly in filter()")
    void testFilterLoggingWithDifferentActorNames() throws UserFilterException {
        // Arrange
        List<Long> userIds = Arrays.asList(111L, 222L, 333L);
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(userIds);

        // Act - Execute filter multiple times with different actor names
        List<Long> result1 = filter.filter("actor1");
        List<Long> result2 = filter.filter("manager");
        List<Long> result3 = filter.filter("admin");

        // Assert
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertEquals(userIds, result1);
        assertEquals(userIds, result2);
        assertEquals(userIds, result3);
    }

    /**
     * Test logging with special characters in actor name.
     */
    @Test
    @DisplayName("Should handle logging with special characters in actor name")
    void testFilterLoggingWithSpecialCharacters() throws UserFilterException {
        // Arrange
        List<Long> userIds = Arrays.asList(999L);
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(userIds);

        // Act
        List<Long> result = filter.filter("role-with_special.chars@domain");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    /**
     * Test logging with large list to trigger debug logging.
     */
    @Test
    @DisplayName("Should log large list processing in filter()")
    void testFilterLoggingWithLargeList() throws UserFilterException {
        // Arrange - Create a large list
        java.util.List<Long> largeList = new java.util.ArrayList<>();
        for (long i = 0; i < 1000; i++) {
            largeList.add(i);
        }
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(largeList);

        // Act
        List<Long> result = filter.filter("largeListActor");

        // Assert
        assertNotNull(result);
        assertEquals(1000, result.size());
    }

    // =========================================================================
    // EXCEPTION HANDLING COVERAGE TESTS
    // =========================================================================

    /**
     * Test that ClassCastException during cast is caught and wrapped.
     * This covers the catch block at line 756 in filter() method.
     * We simulate corrupted data (String) that will fail the cast to List<Long>.
     */
    @Test
    @DisplayName("Should catch ClassCastException when data is corrupted")
    void testFilterCatchesClassCastExceptionOnCorruptedData() {
        // Arrange - Set corrupted data that will fail cast
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter("corrupted string data");

        // Act & Assert
        UserFilterException exception = assertThrows(
            UserFilterException.class,
            () -> filter.filter("testActor"),
            "Should throw UserFilterException when ClassCastException occurs"
        );

        // Verify exception details
        assertNotNull(exception.getCause(), "Exception should have a cause");
        assertTrue(exception.getMessage().contains("Failed to process validated user list input"),
            "Error message should describe the failure");
    }

    /**
     * Test that casting a non-List to List causes exception handling.
     */
    @Test
    @DisplayName("Should handle casting error gracefully")
    void testFilterHandlesCastingError() {
        // Arrange - Integer instead of List
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(12345);

        // Act & Assert
        UserFilterException exception = assertThrows(
            UserFilterException.class,
            () -> filter.filter("testActor")
        );

        // Verify
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Failed to process validated user list input"));
    }

    /**
     * Test exception handling with HashMap.
     */
    @Test
    @DisplayName("Should handle HashMap casting error")
    void testFilterHandlesHashMapCastingError() {
        // Arrange
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(new java.util.HashMap<String, Long>());

        // Act & Assert
        UserFilterException exception = assertThrows(
            UserFilterException.class,
            () -> filter.filter("testActor")
        );

        assertNotNull(exception.getCause());
    }

    // =========================================================================
    // EDGE CASES FOR COMPLETE COVERAGE
    // =========================================================================

    /**
     * Test filter with empty actor name.
     */
    @Test
    @DisplayName("Should handle filter with empty actor name")
    void testFilterWithEmptyActorName() throws UserFilterException {
        // Arrange
        List<Long> userIds = Arrays.asList(42L);
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(userIds);

        // Act
        List<Long> result = filter.filter("");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(42L, result.get(0));
    }

    /**
     * Test filter with null actor name (edge case but valid).
     */
    @Test
    @DisplayName("Should handle filter with null actor name")
    void testFilterWithNullActorName() throws UserFilterException {
        // Arrange
        List<Long> userIds = Arrays.asList(123L);
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(userIds);

        // Act - Actor name can technically be null in some scenarios
        List<Long> result = filter.filter(null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    /**
     * Test filter with very long actor name.
     */
    @Test
    @DisplayName("Should handle filter with very long actor name")
    void testFilterWithLongActorName() throws UserFilterException {
        // Arrange
        List<Long> userIds = Arrays.asList(789L);
        String longActorName = "a".repeat(1000);
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(userIds);

        // Act
        List<Long> result = filter.filter(longActorName);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    /**
     * Test the complete flow: validation and then filter.
     */
    @Test
    @DisplayName("Should complete full validation and filter flow successfully")
    void testCompleteValidationAndFilterFlow() throws Exception {
        // Arrange
        List<Long> userIds = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(userIds);

        // Act - Validate first, then filter (normal usage pattern)
        filter.validateInputParameters();
        List<Long> result = filter.filter("completeFlowActor");

        // Assert
        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals(userIds, result);
    }

    /**
     * Test multiple filters in sequence with logging.
     */
    @Test
    @DisplayName("Should handle multiple sequential filter calls with logging")
    void testMultipleSequentialFilterCalls() throws UserFilterException {
        // Arrange
        List<Long> userIds = Arrays.asList(100L, 200L);
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(userIds);

        // Act - Call filter many times to trigger logging multiple times
        List<Long> result1 = filter.filter("call1");
        List<Long> result2 = filter.filter("call2");
        List<Long> result3 = filter.filter("call3");
        List<Long> result4 = filter.filter("call4");
        List<Long> result5 = filter.filter("call5");

        // Assert - All should return same list
        assertEquals(userIds, result1);
        assertEquals(userIds, result2);
        assertEquals(userIds, result3);
        assertEquals(userIds, result4);
        assertEquals(userIds, result5);
    }

    /**
     * Test filter return value consistency.
     */
    @Test
    @DisplayName("Should return consistent results across multiple calls")
    void testFilterReturnConsistency() throws UserFilterException {
        // Arrange
        List<Long> userIds = Arrays.asList(555L, 666L, 777L);
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(userIds);

        // Act
        List<Long> result1 = filter.filter("actor");
        List<Long> result2 = filter.filter("actor");

        // Assert - Same reference and same content
        assertSame(result1, result2);
        assertEquals(result1, result2);
    }

    /**
     * Test with boundary values in list.
     */
    @Test
    @DisplayName("Should handle filter with Long boundary values")
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
     * Test filter multiple times with different lists to ensure robustness.
     */
    @Test
    @DisplayName("Should handle sequential filters with different lists")
    void testSequentialFiltersWithDifferentLists() throws UserFilterException {
        // First filter
        List<Long> list1 = Arrays.asList(1L, 2L, 3L);
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(list1);
        List<Long> result1 = filter.filter("actor1");
        assertEquals(list1, result1);

        // Second filter with different list
        List<Long> list2 = Arrays.asList(100L, 200L);
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(list2);
        List<Long> result2 = filter.filter("actor2");
        assertEquals(list2, result2);

        // Verify lists are independent
        assertNotEquals(result1, result2);
    }
}
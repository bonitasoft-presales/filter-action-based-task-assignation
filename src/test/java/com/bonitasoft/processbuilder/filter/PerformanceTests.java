package com.bonitasoft.processbuilder.filter;

import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.bonitasoft.engine.filter.UserFilterException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for MultipleUserIdsActorFilter.
 * Tests that the filter handles large lists efficiently within time constraints.
 */
@DisplayName("Performance Tests")
class PerformanceTests {

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
    // PERFORMANCE TESTS
    // =========================================================================

    /**
     * Performance test: Handle large lists efficiently within 1 second
     */
    @Test
    @DisplayName("Should handle large lists efficiently")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testLargeListPerformance() throws UserFilterException {
        // Arrange
        List<Long> largeList = new ArrayList<>();
        for (long i = 0; i < 100000; i++) {
            largeList.add(i);
        }
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(largeList);

        // Act - Should complete in under 1 second
        List<Long> result = filter.filter("testActor");

        // Assert
        assertNotNull(result);
        assertEquals(100000, result.size());
    }

    /**
     * Performance test: Validation should be O(1) complexity
     * Should be fast regardless of list size (only checks first element)
     */
    @Test
    @DisplayName("Validation should be O(1) - fast regardless of list size")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testValidationIsConstantTime() throws ConnectorValidationException {
        // Arrange - Create a very large list
        List<Long> list = new ArrayList<>();
        for (long i = 0; i < 1000000; i++) {
            list.add(i);
        }
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(list);

        // Act - Should be fast regardless of list size
        long startTime = System.currentTimeMillis();
        filter.validateInputParameters();
        long endTime = System.currentTimeMillis();

        // Assert - Should complete very quickly (under 100ms)
        long elapsedTime = endTime - startTime;
        assertTrue(elapsedTime < 100, 
            String.format("Validation took %d ms, should be under 100ms for O(1) complexity", elapsedTime));
    }

    /**
     * Performance test: Filter method should be O(1) - just returns the list
     */
    @Test
    @DisplayName("Filter method should be O(1) - constant time return")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testFilterMethodIsConstantTime() throws UserFilterException {
        // Arrange - Create a very large list
        List<Long> list = new ArrayList<>();
        for (long i = 0; i < 1000000; i++) {
            list.add(i);
        }
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(list);

        // Act
        long startTime = System.currentTimeMillis();
        List<Long> result = filter.filter("testActor");
        long endTime = System.currentTimeMillis();

        // Assert
        long elapsedTime = endTime - startTime;
        assertNotNull(result);
        assertEquals(1000000, result.size());
        assertTrue(elapsedTime < 50,
            String.format("Filter took %d ms, should be under 50ms for O(1) complexity", elapsedTime));
    }

    /**
     * Stress test: Multiple sequential large list operations
     */
    @Test
    @DisplayName("Should handle multiple large list operations sequentially")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testMultipleLargeListOperations() throws UserFilterException, ConnectorValidationException {
        // Act - Process 5 large lists sequentially
        for (int iteration = 0; iteration < 5; iteration++) {
            // Create large list
            List<Long> largeList = new ArrayList<>();
            for (long i = 0; i < 100000; i++) {
                largeList.add(i);
            }
            ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(largeList);

            // Validate and filter
            filter.validateInputParameters();
            List<Long> result = filter.filter("actor" + iteration);

            // Assert
            assertNotNull(result);
            assertEquals(100000, result.size());
        }
    }

    /**
     * Memory efficiency test: Verify list is returned by reference, not copied
     */
    @Test
    @DisplayName("Should return list by reference without copying")
    void testListReturnedByReference() throws UserFilterException {
        // Arrange
        List<Long> originalList = new ArrayList<>();
        for (long i = 0; i < 100000; i++) {
            originalList.add(i);
        }
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(originalList);

        // Act
        List<Long> result = filter.filter("testActor");

        // Assert - Should be the same object (by reference)
        assertSame(originalList, result, 
            "Filter should return the same list object, not a copy (memory efficient)");
    }

    /**
     * Edge case: Empty list should still be fast
     */
    @Test
    @DisplayName("Should handle edge case: single element list quickly")
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    void testEdgeCaseSingleElement() throws UserFilterException, ConnectorValidationException {
        // Arrange
        List<Long> singleElement = new ArrayList<>();
        singleElement.add(12345L);
        ((TestableMultipleUserIdsActorFilter) filter).setStoredParameter(singleElement);

        // Act
        filter.validateInputParameters();
        List<Long> result = filter.filter("testActor");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(12345L, result.get(0));
    }
}
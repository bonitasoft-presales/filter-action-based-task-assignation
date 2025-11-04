package com.bonitasoft.processbuilder.filter.testutil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class providing test data generators and helper methods for filter tests.
 * Reduces code duplication and makes tests more maintainable.
 */
public final class ActorFilterTestDataBuilder {

    private ActorFilterTestDataBuilder() {
        throw new UnsupportedOperationException("This is a test utility class and cannot be instantiated.");
    }

    // =========================================================================
    // VALID TEST DATA GENERATORS
    // =========================================================================

    /**
     * Creates a valid List<Long> with a single element.
     */
    public static List<Long> createSingleElementList(long userId) {
        List<Long> list = new ArrayList<>();
        list.add(userId);
        return list;
    }

    /**
     * Creates a valid List<Long> with multiple elements.
     */
    public static List<Long> createMultipleElementList(long... userIds) {
        List<Long> list = new ArrayList<>();
        for (long userId : userIds) {
            list.add(userId);
        }
        return list;
    }

    /**
     * Creates a valid List<Long> with a specific size, populated with sequential IDs.
     */
    public static List<Long> createListWithSize(int size) {
        List<Long> list = new ArrayList<>();
        for (long i = 0; i < size; i++) {
            list.add(i);
        }
        return list;
    }

    /**
     * Creates a parameter map with valid user list.
     */
    public static Map<String, Object> createValidParameterMap(List<Long> userIds) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("usersList", userIds);
        return parameters;
    }

    /**
     * Creates a large list for stress testing (10000 elements).
     */
    public static List<Long> createLargeUserList() {
        return createListWithSize(10000);
    }

    /**
     * Creates a list with boundary values.
     */
    public static List<Long> createBoundaryValuesList() {
        return createMultipleElementList(
            Long.MIN_VALUE,
            -1L,
            0L,
            1L,
            Long.MAX_VALUE
        );
    }

    // =========================================================================
    // INVALID TEST DATA GENERATORS
    // =========================================================================

    /**
     * Creates a parameter map with null value.
     */
    public static Map<String, Object> createNullParameterMap() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("usersList", null);
        return parameters;
    }

    /**
     * Creates a parameter map with empty list.
     */
    public static Map<String, Object> createEmptyListParameterMap() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("usersList", new ArrayList<Long>());
        return parameters;
    }

    /**
     * Creates a parameter map with wrong type (String).
     */
    public static Map<String, Object> createStringParameterMap(String value) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("usersList", value);
        return parameters;
    }

    /**
     * Creates a parameter map with wrong type (Integer).
     */
    public static Map<String, Object> createIntegerParameterMap(Integer value) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("usersList", value);
        return parameters;
    }

    /**
     * Creates a parameter map with List<Integer> instead of List<Long>.
     */
    public static Map<String, Object> createIntegerListParameterMap(int... values) {
        Map<String, Object> parameters = new HashMap<>();
        List<Integer> intList = new ArrayList<>();
        for (int value : values) {
            intList.add(value);
        }
        parameters.put("usersList", intList);
        return parameters;
    }

    /**
     * Creates a parameter map with List<String> instead of List<Long>.
     */
    public static Map<String, Object> createStringListParameterMap(String... values) {
        Map<String, Object> parameters = new HashMap<>();
        List<String> stringList = new ArrayList<>();
        for (String value : values) {
            stringList.add(value);
        }
        parameters.put("usersList", stringList);
        return parameters;
    }

    /**
     * Creates a parameter map with List containing null elements.
     */
    public static Map<String, Object> createListWithNullElementsParameterMap() {
        Map<String, Object> parameters = new HashMap<>();
        List<Object> list = new ArrayList<>();
        list.add(null);
        parameters.put("usersList", list);
        return parameters;
    }

    /**
     * Creates a parameter map with HashMap instead of List.
     */
    public static Map<String, Object> createHashMapParameterMap() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("usersList", new HashMap<String, Long>());
        return parameters;
    }

    // =========================================================================
    // ACTOR NAME TEST DATA
    // =========================================================================

    /**
     * Returns an array of test actor names for parameterized tests.
     */
    public static String[] getTestActorNames() {
        return new String[]{
            "manager",
            "admin",
            "supervisor",
            "director",
            "custom-role",
            "user",
            "guest",
            ""  // Edge case: empty string
        };
    }

    /**
     * Returns valid actor name for basic tests.
     */
    public static String getDefaultActorName() {
        return "testActor";
    }

    // =========================================================================
    // ASSERTION HELPERS
    // =========================================================================

    /**
     * Validates that an error message contains expected keywords.
     */
    public static boolean validateErrorMessage(String errorMessage, String... keywords) {
        for (String keyword : keywords) {
            if (!errorMessage.contains(keyword)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a descriptive error message for test failures.
     */
    public static String createFailureMessage(String testName, String reason) {
        return String.format("[%s] %s", testName, reason);
    }
}
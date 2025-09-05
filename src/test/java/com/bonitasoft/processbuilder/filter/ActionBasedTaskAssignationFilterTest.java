package com.bonitasoft.processbuilder.filter;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessInstanceNotFoundException;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.bonitasoft.engine.connector.EngineExecutionContext;
import org.bonitasoft.engine.filter.UserFilterException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.search.SearchOptions;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.search.impl.SearchResultImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.logging.Logger;

import static com.bonitasoft.processbuilder.filter.ActionBasedTaskAssignationFilter.USERS_INPUT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link ActionBasedTaskAssignationFilter} class.
 * These tests use Mockito to simulate the Bonita APIs and execution context,
 * ensuring that the filter logic works as expected under various scenarios.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActionBasedTaskAssignationFilterTest {

    @Spy
    private ActionBasedTaskAssignationFilter filter;

    @Mock
    private APIAccessor apiAccessor;
    @Mock
    private ProcessAPI processApi;
    @Mock
    private IdentityAPI identityAPI;
    @Mock
    private EngineExecutionContext executionContext;

    private static final Logger LOGGER = Logger.getLogger(ActionBasedTaskAssignationFilterTest.class.getName());

    /**
     * Sets up the Mockito mocks before each test method.
     * This ensures that the API accessor always returns the mocked API clients.
     */
    @BeforeEach
    void setUp() {
        when(apiAccessor.getProcessAPI()).thenReturn(processApi);
        when(apiAccessor.getIdentityAPI()).thenReturn(identityAPI);
    }

    // --- Validation Tests ---

    /**
     * Tests that the filter throws a {@link ConnectorValidationException}
     * when a membership object has both groupId and roleId as null or non-numeric values.
     */
    @Test
    void validateInputParameters_should_throw_exception_if_both_groupId_and_roleId_are_null() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(USERS_INPUT, "{\"initiator\": false, \"users\": [], \"memberShips\": [{\"groupId\": null, \"roleId\": null}]}");
        filter.setInputParameters(parameters);
        
        ConnectorValidationException thrown = assertThrows(ConnectorValidationException.class, () -> filter.validateInputParameters());
        
        // Assert the full error message, as it's wrapped by a generic catch block
        assertTrue(thrown.getMessage().contains("Invalid JSON structure for parameter 'users' - ERROR: 'Each membership must have at least a groupId or a roleId.'"));
    }

    /**
     * Tests that the validation succeeds with a correctly structured JSON input.
     */
    @Test
    void validateInputParameters_should_succeed_with_valid_json() throws ConnectorValidationException {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(USERS_INPUT, "{\"initiator\": true, \"users\": [1, 2], \"memberShips\": [{\"groupId\": 10, \"roleId\": 20}]}");
        filter.setInputParameters(parameters);
        assertDoesNotThrow(() -> filter.validateInputParameters());
    }

    /**
     * Tests that a {@link ConnectorValidationException} is thrown if the input parameter is missing.
     */
    @Test
    void validateInputParameters_should_throw_exception_if_input_is_not_exist() {
        Map<String, Object> parameters = new HashMap<>();
        filter.setInputParameters(parameters);
        assertThrows(ConnectorValidationException.class, () -> filter.validateInputParameters());
    }

    /**
     * Tests that a {@link ConnectorValidationException} is thrown if the input is explicitly null.
     */
    @Test
    void validateInputParameters_should_throw_exception_if_input_is_null() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(USERS_INPUT, null);
        filter.setInputParameters(parameters);
        assertThrows(ConnectorValidationException.class, () -> filter.validateInputParameters());
    }

    /**
     * Tests that a {@link ConnectorValidationException} is thrown if the input is an empty string.
     */
    @Test
    void validateInputParameters_should_throw_exception_if_input_is_empty() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(USERS_INPUT, "");
        filter.setInputParameters(parameters);
        assertThrows(ConnectorValidationException.class, () -> filter.validateInputParameters());
    }

    /**
     * Tests that a {@link ConnectorValidationException} is thrown if the input is not a String.
     */
    @Test
    void validateInputParameters_should_throw_exception_if_input_is_not_a_string() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(USERS_INPUT, 12345L);
        filter.setInputParameters(parameters);
        assertThrows(ConnectorValidationException.class, () -> filter.validateInputParameters());
    }
 
    /**
     * Tests that a {@link ConnectorValidationException} is thrown if the JSON is malformed.
     */
    @Test
    void validateInputParameters_should_throw_exception_if_input_is_malformed_json() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(USERS_INPUT, "invalid json");
        filter.setInputParameters(parameters);
        assertThrows(ConnectorValidationException.class, () -> filter.validateInputParameters());
    }
 
    /**
     * Tests that a {@link ConnectorValidationException} is thrown if the JSON is not an object.
     */
    @Test
    void validateInputParameters_should_throw_exception_if_json_is_not_an_object() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(USERS_INPUT, "[]");
        filter.setInputParameters(parameters);
        assertThrows(ConnectorValidationException.class, () -> filter.validateInputParameters());
    }
 
    /**
     * Tests that a {@link ConnectorValidationException} is thrown if the 'initiator' field is missing.
     */
    @Test
    void validateInputParameters_should_throw_exception_if_initiator_field_is_missing() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(USERS_INPUT, "{\"users\": [], \"memberShips\": []}");
        filter.setInputParameters(parameters);
        ConnectorValidationException thrown = assertThrows(ConnectorValidationException.class, () -> filter.validateInputParameters());
        assertNotNull(thrown.getMessage());
        assertTrue(thrown.getMessage().contains("initiator"));
    }
 
    /**
     * Tests that a {@link ConnectorValidationException} is thrown if the 'users' field is not an array.
     */
    @Test
    void validateInputParameters_should_throw_exception_if_users_field_is_not_an_array() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(USERS_INPUT, "{\"initiator\": true, \"users\": \"not an array\", \"memberShips\": []}");
        filter.setInputParameters(parameters);
        ConnectorValidationException thrown = assertThrows(ConnectorValidationException.class, () -> filter.validateInputParameters());
        assertNotNull(thrown.getMessage());
        assertTrue(thrown.getMessage().contains("users"));
    }

    /**
     * Tests the private validateMemberships method directly using reflection.
     * This test ensures that the method throws an exception when both groupId and roleId
     * are not numbers, covering a critical validation path.
     * @throws Exception if a method invocation error occurs.
     */
    @Test
    void validateMemberships_should_throw_exception_if_ids_are_not_numbers_in_isolated_test() throws Exception {
        String jsonString = "[{\"groupId\": \"invalid\", \"roleId\": \"invalid\"}]";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode memberShipsNode = mapper.readTree(jsonString);

        InvocationTargetException thrown = assertThrows(InvocationTargetException.class, () -> {
            java.lang.reflect.Method method = ActionBasedTaskAssignationFilter.class.getDeclaredMethod("validateMemberships", JsonNode.class);
            method.setAccessible(true);
            method.invoke(filter, memberShipsNode);
        });

        Throwable cause = thrown.getCause();
        assertNotNull(cause);
        assertTrue(cause instanceof ConnectorValidationException);
        assertTrue(cause.getMessage().contains("Each membership must have at least a groupId or a roleId."));
    }

    /**
     * Tests that the {@code validateMemberships} method throws a {@link ConnectorValidationException}
     * when a membership object contains both 'groupId' and 'roleId' with non-numeric values.
     * This ensures that the validation logic correctly identifies invalid data types and
     * prevents the process from continuing with malformed membership information.
     * The test uses the Jackson {@link JsonNodeFactory} to programmatically create the JSON
     * structure, ensuring a controlled and reliable test environment.
     *
     * @throws Exception if a method invocation error occurs, though the test asserts for the expected exception.
     */
    @Test
    void validateMemberships_should_throw_exception_if_ids_are_not_numbers() throws Exception {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ArrayNode memberShipsNode = factory.arrayNode();
        ObjectNode firstMembership = factory.objectNode();
        firstMembership.put("groupId", "invalid");
        firstMembership.put("roleId", "invalid");
        memberShipsNode.add(firstMembership);
        ConnectorValidationException thrown = assertThrows(ConnectorValidationException.class, () -> filter.validateMemberships(memberShipsNode));

        assertTrue(thrown.getMessage().contains("Each membership must have at least a groupId or a roleId."));
    }


    /**
     * Tests that parsing throws an exception when the JSON string is null.
     */
    @Test
    void parseInvolvedUsersJson_should_throw_exception_when_jsonString_is_null() {
        String usersJson =  null;
        assertThrows(IllegalArgumentException.class, () -> filter.parseInvolvedUsersJson(usersJson));
    }

    /**
     * Tests that parsing throws an exception when the JSON string is empty.
     */
    @Test
    void parseInvolvedUsersJson_should_throw_exception_when_jsonString_is_empty() {
        String usersJson =  "";
        assertThrows(IllegalArgumentException.class, () -> filter.parseInvolvedUsersJson(usersJson));
    }

    /**
     * Tests that the filter's main method throws a {@link UserFilterException}
     * when there is an error parsing the input JSON.
     * @throws Exception if an API accessor error occurs.
     */
    @Test
    void filter_should_throw_UserFilterException_on_parsing_error() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(USERS_INPUT, "invalid json");
        filter.setInputParameters(parameters);
        
        when(filter.getAPIAccessor()).thenReturn(apiAccessor);
        
        doThrow(new IllegalArgumentException("Parsing failed")).when(filter).parseInvolvedUsersJson(anyString());
        
        assertThrows(UserFilterException.class, () -> filter.filter("myActor"));
    }

    /**
     * Tests that parsing throws an exception for malformed JSON strings.
     */
    @Test
    void parseInvolvedUsersJson_should_throw_exception_on_invalid_json_format() {
        String malformedJson = "{ \"initiator\": true, \"users\": [1, 2, ], }";
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> filter.parseInvolvedUsersJson(malformedJson));

        assertTrue(thrown.getMessage().contains("Failed to parse JSON string. Invalid format."));
    }

    /**
     * Tests that a missing 'groupId' in a membership object is handled correctly (assigned as null).
     */
    @Test
    void parseInvolvedUsersJson_should_handle_missing_groupId() {
        String json = "{\"initiator\": false, \"users\": [], \"memberShips\": [{\"roleId\": 20, \"memberShipsRef\": \"ref1\"}]}";
        
        ActionBasedTaskAssignationFilter.InvolvedUsersData data = filter.parseInvolvedUsersJson(json);
        
        assertNotNull(data.memberships());
        assertEquals(1, data.memberships().size());
        assertNull(data.memberships().get(0).groupId());
        assertEquals(20L, data.memberships().get(0).roleId());
        assertEquals("ref1", data.memberships().get(0).memberShipsRef());
    }

    /**
     * Tests that a missing 'roleId' in a membership object is handled correctly (assigned as null).
     */
    @Test
    void parseInvolvedUsersJson_should_handle_missing_roleId() {
        String json = "{\"initiator\": false, \"users\": [], \"memberShips\": [{\"groupId\": 10, \"memberShipsRef\": \"ref1\"}]}";
        
        ActionBasedTaskAssignationFilter.InvolvedUsersData data = filter.parseInvolvedUsersJson(json);
        
        assertNotNull(data.memberships());
        assertEquals(1, data.memberships().size());
        assertEquals(10L, data.memberships().get(0).groupId());
        assertNull(data.memberships().get(0).roleId());
        assertEquals("ref1", data.memberships().get(0).memberShipsRef());
    }

    /**
     * Tests that a missing 'memberShipsRef' field is handled correctly.
     */
    @Test
    void parseInvolvedUsersJson_should_handle_missing_memberShipsRef() {
        String json = "{\"initiator\": false, \"users\": [], \"memberShips\": [{\"groupId\": 10, \"roleId\": 20}]}";
        
        ActionBasedTaskAssignationFilter.InvolvedUsersData data = filter.parseInvolvedUsersJson(json);
        
        assertNotNull(data.memberships());
        assertEquals(1, data.memberships().size());
        assertEquals(10L, data.memberships().get(0).groupId());
        assertEquals(20L, data.memberships().get(0).roleId());
        assertNull(data.memberships().get(0).memberShipsRef());
    }

    /**
     * Tests that parsing throws an exception if the 'groupId' value is not a long.
     */
    @Test
    void parseInvolvedUsersJson_should_throw_exception_if_groupId_is_not_a_long() {
        String json = "{\"initiator\": false, \"users\": [], \"memberShips\": [{\"groupId\": \"not a long\", \"roleId\": 20}]}";
        
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> filter.parseInvolvedUsersJson(json));
        
        assertTrue(thrown.getMessage().contains("Invalid format."));
    }

    /**
     * Tests that parsing throws an exception if the 'roleId' value is not a long.
     */
    @Test
    void parseInvolvedUsersJson_should_throw_exception_if_roleId_is_not_a_long() {
        String json = "{\"initiator\": false, \"users\": [], \"memberShips\": [{\"groupId\": 10, \"roleId\": \"not a long\"}]}";
        
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> filter.parseInvolvedUsersJson(json));
        
        assertTrue(thrown.getMessage().contains("Invalid format."));
    }

    /**
     * Tests that parsing throws an exception if 'memberShipsRef' is not a text value.
     */
    @Test
    void parseInvolvedUsersJson_should_throw_exception_if_memberShipsRef_is_not_a_text() {
        String json = "{\"initiator\": false, \"users\": [], \"memberShips\": [{\"groupId\": 10, \"roleId\": 20, \"memberShipsRef\": 123}]}";
        
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> filter.parseInvolvedUsersJson(json));
        
        assertTrue(thrown.getMessage().contains("Invalid format."));
    }

    /**
     * Tests that parsing throws an exception if a user in the list is not a number.
     */
    @Test
    void parseInvolvedUsersJson_should_throw_exception_if_user_in_list_is_not_a_number() {
        String json = "{\"initiator\": false, \"users\": [1, \"invalid_user_id\"], \"memberShips\": []}";
        
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> filter.parseInvolvedUsersJson(json));
        
        assertTrue(thrown.getMessage().contains("Failed to parse JSON string. Invalid format."));
    }

    /**
     * Tests that a 'memberShipsRef' with a null value is handled correctly.
     */
    @Test
    void parseInvolvedUsersJson_should_handle_null_memberShipsRef_value() {
        String json = "{\"initiator\": false, \"users\": [], \"memberShips\": [{\"groupId\": 10, \"roleId\": 20, \"memberShipsRef\": null}]}";
        ActionBasedTaskAssignationFilter.InvolvedUsersData data = filter.parseInvolvedUsersJson(json);
        assertNotNull(data.memberships());
        assertEquals(1, data.memberships().size());
        assertNull(data.memberships().get(0).memberShipsRef());
    }

    /**
     * Tests that a membership with a missing 'memberShipsRef' field is handled correctly.
     */
    @Test
    void parseInvolvedUsersJson_should_handle_missing_memberShipsRef_field() {
        String json = "{\"initiator\": false, \"users\": [], \"memberShips\": [{\"groupId\": 10, \"roleId\": 20}]}";
        ActionBasedTaskAssignationFilter.InvolvedUsersData data = filter.parseInvolvedUsersJson(json);
        assertNotNull(data.memberships());
        assertEquals(1, data.memberships().size());
        assertNull(data.memberships().get(0).memberShipsRef());
    }

    /**
     * Tests that a {@link ConnectorValidationException} is thrown if an element in the 'memberShips' array is not an object.
     */
    @Test
    void validateInputParameters_should_throw_exception_if_membership_element_is_not_an_object() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(USERS_INPUT, "{\"initiator\": false, \"users\": [], \"memberShips\": [\"invalid_element\"]}");
        filter.setInputParameters(parameters);
        ConnectorValidationException thrown = assertThrows(ConnectorValidationException.class, () -> filter.validateInputParameters());
        assertTrue(thrown.getMessage().contains("Each element in 'memberShips' array must be a JSON object."));
    }

    /**
     * Tests that a {@link ConnectorValidationException} is thrown if a membership has neither a valid groupId nor a valid roleId.
     */
    @Test
    void validateInputParameters_should_throw_exception_if_membership_has_neither_groupId_nor_roleId() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(USERS_INPUT, "{\"initiator\": false, \"users\": [], \"memberShips\": [{\"groupId\": \"invalid\", \"roleId\": \"invalid\"}]}");
        filter.setInputParameters(parameters);
        ConnectorValidationException thrown = assertThrows(ConnectorValidationException.class, () -> filter.validateInputParameters());
        assertTrue(thrown.getMessage().contains("Each membership must have at least a groupId or a roleId."));
    }

    /**
     * Tests that a {@link ConnectorValidationException} is thrown if a membership object is empty.
     */
    @Test
    void validateInputParameters_should_throw_exception_if_membership_has_empty_object() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(USERS_INPUT, "{\"initiator\": false, \"users\": [], \"memberShips\": [{}]}");
        filter.setInputParameters(parameters);
        ConnectorValidationException thrown = assertThrows(ConnectorValidationException.class, () -> filter.validateInputParameters());
        assertTrue(thrown.getMessage().contains("Each membership must have at least a groupId or a roleId."));
    }

    /**
     * Tests that a {@link ConnectorValidationException} is thrown when a membership has both groupId and roleId as false.
     */
    @Test
    void validateMemberships_should_throw_exception_if_both_ids_are_false() {
        String json = "{\"initiator\": false, \"users\": [], \"memberShips\": [{}]}";
        
        ConnectorValidationException thrown = assertThrows(ConnectorValidationException.class, () -> {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put(USERS_INPUT, json);
            filter.setInputParameters(parameters);
            
            filter.validateInputParameters();
        });
        
        assertTrue(thrown.getMessage().contains("Each membership must have at least a groupId or a roleId."));
    }

    /**
     * Tests that a {@link ConnectorValidationException} is thrown when both groupId and roleId are not numbers.
     */
    @Test
    void validateInputParameters_should_throw_exception_if_both_groupId_and_roleId_are_not_numbers() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(USERS_INPUT, "{\"initiator\": false, \"users\": [], \"memberShips\": [{\"groupId\": \"invalid\", \"roleId\": \"invalid\"}]}");
        filter.setInputParameters(parameters);

        ConnectorValidationException thrown = assertThrows(ConnectorValidationException.class, () -> filter.validateInputParameters());

        assertTrue(thrown.getMessage().contains("Each membership must have at least a groupId or a roleId."));
    }
 
    // --- Business Logic Tests ---
 
    /**
     * Tests that the filter gracefully handles a {@link ProcessInstanceNotFoundException} when retrieving the initiator.
     * @throws Exception if an API accessor error occurs.
     */
    @Test
    void filter_should_log_error_when_process_instance_not_found() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(USERS_INPUT, "{\"initiator\": true, \"users\": [], \"memberShips\": []}");
        filter.setInputParameters(parameters);
        
        when(filter.getExecutionContext()).thenReturn(executionContext);
        when(filter.getAPIAccessor()).thenReturn(apiAccessor);
        when(executionContext.getRootProcessInstanceId()).thenReturn(100L);
        
        when(processApi.getProcessInstance(anyLong())).thenThrow(new ProcessInstanceNotFoundException(""));
        
        List<Long> result = filter.filter("myActor");
        
        assertTrue(result.isEmpty());
        verify(processApi).getProcessInstance(100L);
    }

    /**
     * Tests that the filter logs an error and continues when a membership search fails with an unexpected exception.
     * @throws Exception if an API accessor error occurs.
     */
    @Test
    void filter_should_log_error_on_membership_search_exception() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(USERS_INPUT, "{\"initiator\": false, \"users\": [101], \"memberShips\": [{\"groupId\": 10, \"roleId\": 30}]}");
        filter.setInputParameters(parameters);

        when(filter.getAPIAccessor()).thenReturn(apiAccessor);

        when(identityAPI.searchUsers(any(SearchOptions.class))).thenThrow(new RuntimeException("Search failed"));
        
        List<Long> result = filter.filter("myActor");
        
        assertEquals(1, result.size());
        assertTrue(result.contains(101L));
        
        verify(identityAPI).searchUsers(any(SearchOptions.class));
    }

     /**
      * Tests that the filter returns the initiator's ID when the `initiator` flag is true.
      * @throws Exception if an API accessor error occurs.
      */
     @Test
     void filter_should_return_initiator_id_when_initiator_is_true() throws Exception {
         final long initiatorId = 99L;
         Map<String, Object> parameters = new HashMap<>();
         parameters.put(USERS_INPUT, "{\"initiator\": true, \"users\": [], \"memberShips\": []}");
         filter.setInputParameters(parameters);
 
         when(filter.getExecutionContext()).thenReturn(executionContext);
         when(filter.getAPIAccessor()).thenReturn(apiAccessor);
         
         ProcessInstance processInstance = mock(ProcessInstance.class);
         when(executionContext.getRootProcessInstanceId()).thenReturn(100L);
         when(processApi.getProcessInstance(100L)).thenReturn(processInstance);
         when(processInstance.getStartedBy()).thenReturn(initiatorId);
 
         List<Long> result = filter.filter("myActor");
         assertEquals(1, result.size());
         assertTrue(result.contains(initiatorId));
         verify(processApi).getProcessInstance(100L);
         verify(identityAPI, never()).searchUsers(any());
     }
 
     /**
      * Tests that the filter returns the direct user IDs when provided in the JSON.
      * @throws Exception if an API accessor error occurs.
      */
     @Test
     void filter_should_return_direct_user_ids() throws Exception {
         Map<String, Object> parameters = new HashMap<>();
         parameters.put(USERS_INPUT, "{\"initiator\": false, \"users\": [101, 102], \"memberShips\": []}");
         filter.setInputParameters(parameters);
         when(filter.getExecutionContext()).thenReturn(executionContext);
         when(filter.getAPIAccessor()).thenReturn(apiAccessor);
         
         List<Long> result = filter.filter("myActor");
         assertEquals(2, result.size());
         assertTrue(result.containsAll(Arrays.asList(101L, 102L)));
         verify(processApi, never()).getProcessInstance(anyLong());
         verify(identityAPI, never()).searchUsers(any());
     }
 
     /**
      * Tests that the filter returns users found by a single groupId filter.
      * @throws Exception if an API accessor error occurs.
      */
     @Test
     void filter_should_return_user_ids_from_groupId() throws Exception {
         Map<String, Object> parameters = new HashMap<>();
         parameters.put(USERS_INPUT, "{\"initiator\": false, \"users\": [], \"memberShips\": [{\"groupId\": 10, \"roleId\": null}]}");
         filter.setInputParameters(parameters);
         when(filter.getAPIAccessor()).thenReturn(apiAccessor);
         User user1 = mock(User.class);
         User user2 = mock(User.class);
         when(user1.getId()).thenReturn(201L);
         when(user2.getId()).thenReturn(202L);
         SearchResult<User> searchResult = new SearchResultImpl<>(2L, Arrays.asList(user1, user2));
         when(identityAPI.searchUsers(any(SearchOptions.class))).thenReturn(searchResult);
 
         List<Long> result = filter.filter("myActor");
         assertEquals(2, result.size());
         assertTrue(result.containsAll(Arrays.asList(201L, 202L)));
         ArgumentCaptor<SearchOptions> searchOptionsCaptor = ArgumentCaptor.forClass(SearchOptions.class);
         verify(identityAPI).searchUsers(searchOptionsCaptor.capture());
         assertFalse(searchOptionsCaptor.getValue().getFilters().isEmpty());
     }
 
     /**
      * Tests that the filter returns users found by a single roleId filter.
      * @throws Exception if an API accessor error occurs.
      */
     @Test
     void filter_should_return_user_ids_from_roleId() throws Exception {
         Map<String, Object> parameters = new HashMap<>();
         parameters.put(USERS_INPUT, "{\"initiator\": false, \"users\": [], \"memberShips\": [{\"groupId\": null, \"roleId\": 30}]}");
         filter.setInputParameters(parameters);
         when(filter.getAPIAccessor()).thenReturn(apiAccessor);
         User user1 = mock(User.class);
         when(user1.getId()).thenReturn(301L);
         SearchResult<User> searchResult = new SearchResultImpl<>(1L, Collections.singletonList(user1));
         when(identityAPI.searchUsers(any(SearchOptions.class))).thenReturn(searchResult);
 
         List<Long> result = filter.filter("myActor");
         assertEquals(1, result.size());
         assertTrue(result.contains(301L));
         ArgumentCaptor<SearchOptions> searchOptionsCaptor = ArgumentCaptor.forClass(SearchOptions.class);
         verify(identityAPI).searchUsers(searchOptionsCaptor.capture());
         assertFalse(searchOptionsCaptor.getValue().getFilters().isEmpty());
     }
 
     /**
      * Tests that the filter returns users found by a combined groupId and roleId filter.
      * @throws Exception if an API accessor error occurs.
      */
     @Test
     void filter_should_return_user_ids_from_groupId_and_roleId() throws Exception {
         Map<String, Object> parameters = new HashMap<>();
         parameters.put(USERS_INPUT, "{\"initiator\": false, \"users\": [], \"memberShips\": [{\"groupId\": 10, \"roleId\": 30}]}");
         filter.setInputParameters(parameters);
         when(filter.getAPIAccessor()).thenReturn(apiAccessor);
         User user1 = mock(User.class);
         when(user1.getId()).thenReturn(401L);
         SearchResult<User> searchResult = new SearchResultImpl<>(1L, Collections.singletonList(user1));
         when(identityAPI.searchUsers(any(SearchOptions.class))).thenReturn(searchResult);
 
         List<Long> result = filter.filter("myActor");
         assertEquals(1, result.size());
         assertTrue(result.contains(401L));
         ArgumentCaptor<SearchOptions> searchOptionsCaptor = ArgumentCaptor.forClass(SearchOptions.class);
         verify(identityAPI).searchUsers(searchOptionsCaptor.capture());
         assertTrue(searchOptionsCaptor.getValue().getFilters().size() > 1);
     }
 
     /**
      * Tests that the filter combines users from all sources and removes duplicates.
      * @throws Exception if an API accessor error occurs.
      */
     @Test
     void filter_should_combine_all_sources_and_remove_duplicates() throws Exception {
         Map<String, Object> parameters = new HashMap<>();
         parameters.put(USERS_INPUT, "{\"initiator\": true, \"users\": [501, 502], \"memberShips\": [{\"groupId\": 100, \"roleId\": 200}]}");
         filter.setInputParameters(parameters);
         when(filter.getExecutionContext()).thenReturn(executionContext);
         when(filter.getAPIAccessor()).thenReturn(apiAccessor);
         final long initiatorId = 502L;
         when(executionContext.getRootProcessInstanceId()).thenReturn(500L);
         ProcessInstance processInstance = mock(ProcessInstance.class);
         when(processApi.getProcessInstance(500L)).thenReturn(processInstance);
         when(processInstance.getStartedBy()).thenReturn(initiatorId);
         User userFromMembership = mock(User.class);
         when(userFromMembership.getId()).thenReturn(503L);
         SearchResult<User> searchResult = new SearchResultImpl<>(1L, Collections.singletonList(userFromMembership));
         when(identityAPI.searchUsers(any(SearchOptions.class))).thenReturn(searchResult);
 
         List<Long> result = filter.filter("myActor");
         assertEquals(3, result.size());
         assertTrue(result.containsAll(Arrays.asList(501L, 502L, 503L)));
     }
 
     /**
      * Tests that the filter returns an empty list when no users are found from any source.
      * @throws Exception if an API accessor error occurs.
      */
     @Test
     void filter_should_return_empty_list_when_no_users_found() throws Exception {
         Map<String, Object> parameters = new HashMap<>();
         parameters.put(USERS_INPUT, "{\"initiator\": false, \"users\": [], \"memberShips\": []}");
         filter.setInputParameters(parameters);
         when(filter.getAPIAccessor()).thenReturn(apiAccessor);
 
         List<Long> result = filter.filter("myActor");
         assertTrue(result.isEmpty());
         verify(processApi, never()).getProcessInstance(anyLong());
         verify(identityAPI, never()).searchUsers(any());
     }
 
     /**
      * Tests that parsing a valid JSON string returns the expected data object.
      */
     @Test
     void parseInvolvedUsersJson_should_handle_valid_json() {
         String json = "{\"initiator\": true, \"users\": [1, 2], \"memberShips\": [{\"groupId\": 10, \"roleId\": 20}]}";
         ActionBasedTaskAssignationFilter.InvolvedUsersData data = filter.parseInvolvedUsersJson(json);
         assertTrue(data.initiator());
         assertEquals(2, data.users().size());
         assertEquals(1, data.memberships().size());
         assertEquals(10L, data.memberships().get(0).groupId());
     }

    /**
     * Tests that {@code checkPositiveIntegerInput} does not throw an exception with a valid input.
     */
    @Test
    void checkPositiveIntegerInput_should_not_throw_exception_with_valid_input() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("my_input", 100);
        filter.setInputParameters(parameters);
        assertDoesNotThrow(() -> filter.checkPositiveIntegerInput("my_input"));
    }

    /**
     * Tests that {@code checkPositiveIntegerInput} throws an exception with an invalid value (negative or zero).
     */
    @Test
    void checkPositiveIntegerInput_should_throw_exception_with_invalid_value() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("my_input", -5);
        filter.setInputParameters(parameters);
        assertThrows(ConnectorValidationException.class, () -> filter.checkPositiveIntegerInput("my_input"));

        parameters = new HashMap<>();
        parameters.put("my_input", null);
        filter.setInputParameters(parameters);
        assertThrows(ConnectorValidationException.class, () -> filter.checkPositiveIntegerInput("my_input"));
    }

    /**
     * Tests that {@code checkPositiveIntegerInput} throws an exception with an incorrect data type.
     */
    @Test
    void checkPositiveIntegerInput_should_throw_exception_with_wrong_type() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("my_input", "not an integer");
        filter.setInputParameters(parameters);
        assertThrows(ConnectorValidationException.class, () -> filter.checkPositiveIntegerInput("my_input"));
    }

    /**
     * Tests that {@code checkPositiveLongInput} does not throw an exception with a valid input.
     */
    @Test
    void checkPositiveLongInput_should_not_throw_exception_with_valid_input() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("my_input", 100);
        filter.setInputParameters(parameters);
        assertDoesNotThrow(() -> filter.checkPositiveIntegerInput("my_input"));

        parameters = new HashMap<>();
        parameters.put("my_input", 100L);
        filter.setInputParameters(parameters);
        assertDoesNotThrow(() -> filter.checkPositiveLongInput("my_input"));

    }

    /**
     * Tests that {@code checkPositiveLongInput} throws an exception with an invalid value.
     */
    @Test
    void checkPositiveLongInput_should_throw_exception_with_invalid_value() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("my_input", -5);
        filter.setInputParameters(parameters);
        assertThrows(ConnectorValidationException.class, () -> filter.checkPositiveLongInput("my_input"));

    }

    /**
     * Tests that {@code checkPositiveLongInput} throws an exception when the input is null.
     */
    @Test
    void checkPositiveLongInput_should_throw_exception_with_null_value() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("my_input", null);
        filter.setInputParameters(parameters);
        assertThrows(ConnectorValidationException.class, () -> filter.checkPositiveLongInput("my_input"));
    }

    /**
     * Tests that {@code checkPositiveLongInput} throws an exception with an incorrect data type.
     */
    @Test
    void checkPositiveLongInput_should_throw_exception_with_wrong_type() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("my_input", "not an integer");
        filter.setInputParameters(parameters);
        assertThrows(ConnectorValidationException.class, () -> filter.checkPositiveLongInput("my_input"));
    }


    /**
     * Tests that {@code checkPositiveLongInput} throws an exception with a zero or negative value.
     */
    @Test
    void checkPositiveLongInput_should_throw_exception_with_zero_or_negative_value() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("my_input", -5L);
        filter.setInputParameters(parameters);
        assertThrows(ConnectorValidationException.class, () -> filter.checkPositiveLongInput("my_input"));
        parameters = new HashMap<>();
        parameters.put("my_input", 0L);
        filter.setInputParameters(parameters);
        assertThrows(ConnectorValidationException.class, () -> filter.checkPositiveLongInput("my_input"));
    }

    /**
     * Tests that the filter's search query correctly uses an OR clause when multiple memberships are provided.
     * @throws Exception if an API accessor error occurs.
     */
    @Test
    void filter_should_combine_multiple_memberships_with_or_clause() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(USERS_INPUT, "{\"initiator\": false, \"users\": [], \"memberShips\": [{\"groupId\": 10, \"roleId\": 20}, {\"groupId\": 30, \"roleId\": 40}]}");
        filter.setInputParameters(parameters);
        when(filter.getAPIAccessor()).thenReturn(apiAccessor);

        User user1 = mock(User.class);
        when(user1.getId()).thenReturn(1L);
        SearchResult<User> searchResult = new SearchResultImpl<>(1L, Collections.singletonList(user1));
        when(identityAPI.searchUsers(any(SearchOptions.class))).thenReturn(searchResult);

        List<Long> result = filter.filter("myActor");
        assertEquals(1, result.size());
        verify(identityAPI).searchUsers(any(SearchOptions.class));
    }

    /**
     * Tests that a membership with both groupId and roleId as null is handled correctly during the search.
     * @throws Exception if an API accessor error occurs.
     */
    @Test
    void filter_should_handle_membership_with_both_groupId_and_roleId_null() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(USERS_INPUT, "{\"initiator\": false, \"users\": [], \"memberShips\": [{\"groupId\": null, \"roleId\": null}]}");
        filter.setInputParameters(parameters);
        when(filter.getAPIAccessor()).thenReturn(apiAccessor);

        SearchResult<User> searchResult = new SearchResultImpl<>(0L, Collections.emptyList());
        when(identityAPI.searchUsers(any(SearchOptions.class))).thenReturn(searchResult);

        List<Long> result = filter.filter("myActor");
        assertTrue(result.isEmpty());
        verify(identityAPI).searchUsers(any(SearchOptions.class));
    }

    /**
     * Tests that the filter logs an error and returns an empty list when an unexpected exception occurs while retrieving the process initiator.
     * @throws Exception if an API accessor error occurs.
     */
    @Test
    void filter_should_log_error_when_getting_process_initiator_fails() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(USERS_INPUT, "{\"initiator\": true, \"users\": [], \"memberShips\": []}");
        filter.setInputParameters(parameters);
        
        when(filter.getExecutionContext()).thenReturn(executionContext);
        when(filter.getAPIAccessor()).thenReturn(apiAccessor);
        when(executionContext.getRootProcessInstanceId()).thenReturn(100L);
        
        when(processApi.getProcessInstance(anyLong())).thenThrow(new RuntimeException("An unexpected API error occurred."));
        
        List<Long> result = filter.filter("myActor");
        
        assertTrue(result.isEmpty());
        verify(processApi).getProcessInstance(100L);
    }
}
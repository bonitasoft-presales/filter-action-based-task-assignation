package com.bonitasoft.processbuilder.filter;
 
 import org.bonitasoft.engine.api.APIAccessor;
 import org.bonitasoft.engine.api.IdentityAPI;
 import org.bonitasoft.engine.api.ProcessAPI;
 import org.bonitasoft.engine.bpm.process.ProcessInstance;
 import org.bonitasoft.engine.connector.ConnectorValidationException;
 import org.bonitasoft.engine.connector.EngineExecutionContext;
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
 
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Arrays;
 import java.util.logging.Logger;
 
 import static com.bonitasoft.processbuilder.filter.ActionBasedTaskAssignationFilter.USERS_INPUT;
 import static org.junit.jupiter.api.Assertions.*;
 import static org.mockito.ArgumentMatchers.any;
 import static org.mockito.ArgumentMatchers.anyLong;
 import static org.mockito.Mockito.*;
 
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
 
     @BeforeEach
     void setUp() {
         when(apiAccessor.getProcessAPI()).thenReturn(processApi);
         when(apiAccessor.getIdentityAPI()).thenReturn(identityAPI);
     }
 
     // --- Validation Tests ---
 
     @Test
     void validateInputParameters_should_succeed_with_valid_json() throws ConnectorValidationException {
         Map<String, Object> parameters = new HashMap<>();
         parameters.put(USERS_INPUT, "{\"initiator\": true, \"users\": [1, 2], \"memberShips\": [{\"groupId\": 10, \"roleId\": 20}]}");
         filter.setInputParameters(parameters);
         assertDoesNotThrow(() -> filter.validateInputParameters());
     }
 
     @Test
     void validateInputParameters_should_throw_exception_if_input_is_null() {
         Map<String, Object> parameters = new HashMap<>();
         filter.setInputParameters(parameters);
         assertThrows(ConnectorValidationException.class, () -> filter.validateInputParameters());
     }
 
     @Test
     void validateInputParameters_should_throw_exception_if_input_is_not_a_string() {
         Map<String, Object> parameters = new HashMap<>();
         parameters.put(USERS_INPUT, 12345L);
         filter.setInputParameters(parameters);
         assertThrows(ClassCastException.class, () -> filter.validateInputParameters());
     }
 
     @Test
     void validateInputParameters_should_throw_exception_if_input_is_malformed_json() {
         Map<String, Object> parameters = new HashMap<>();
         parameters.put(USERS_INPUT, "invalid json");
         filter.setInputParameters(parameters);
         assertThrows(ConnectorValidationException.class, () -> filter.validateInputParameters());
     }
 
     @Test
     void validateInputParameters_should_throw_exception_if_json_is_not_an_object() {
         Map<String, Object> parameters = new HashMap<>();
         parameters.put(USERS_INPUT, "[]");
         filter.setInputParameters(parameters);
         assertThrows(ConnectorValidationException.class, () -> filter.validateInputParameters());
     }
 
     @Test
     void validateInputParameters_should_throw_exception_if_initiator_field_is_missing() {
         Map<String, Object> parameters = new HashMap<>();
         parameters.put(USERS_INPUT, "{\"users\": [], \"memberShips\": []}");
         filter.setInputParameters(parameters);
         ConnectorValidationException thrown = assertThrows(ConnectorValidationException.class, () -> filter.validateInputParameters());
         assertNotNull(thrown.getMessage());
         assertTrue(thrown.getMessage().contains("initiator"));
     }
 
     @Test
     void validateInputParameters_should_throw_exception_if_users_field_is_not_an_array() {
         Map<String, Object> parameters = new HashMap<>();
         parameters.put(USERS_INPUT, "{\"initiator\": true, \"users\": \"not an array\", \"memberShips\": []}");
         filter.setInputParameters(parameters);
         ConnectorValidationException thrown = assertThrows(ConnectorValidationException.class, () -> filter.validateInputParameters());
         assertNotNull(thrown.getMessage());
         assertTrue(thrown.getMessage().contains("users"));
     }
 
     // --- Business Logic Tests ---
 
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
 
     @Test
     void parseInvolvedUsersJson_should_handle_valid_json() {
         String json = "{\"initiator\": true, \"users\": [1, 2], \"memberShips\": [{\"groupId\": 10, \"roleId\": 20}]}";
         ActionBasedTaskAssignationFilter.InvolvedUsersData data = filter.parseInvolvedUsersJson(json);
         assertTrue(data.initiator());
         assertEquals(2, data.users().size());
         assertEquals(1, data.memberships().size());
         assertEquals(10L, data.memberships().get(0).groupId());
     }
 }
package com.bonitasoft.processbuilder.filter;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessInstanceNotFoundException;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.bonitasoft.engine.filter.AbstractUserFilter;
import org.bonitasoft.engine.filter.UserFilterException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.identity.UserSearchDescriptor;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


public class ActionBasedTaskAssignationFilter extends AbstractUserFilter {

    private static final Logger LOGGER = Logger.getLogger(ActionBasedTaskAssignationFilter.class.getName());

    static final String USERS_INPUT = "users";

    /**
     * Perform validation on the inputs defined on the actorfilter definition (src/main/resources/filter-action-based-task-assignment.def)
     * You should: 
     * - validate that mandatory inputs are presents
     * - validate that the content of the inputs is coherent with your use case (e.g: validate that a date is / isn't in the past ...)
     */
    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        validateUsersJson(USERS_INPUT);
    }

    protected void checkPositiveIntegerInput(String inputName) throws ConnectorValidationException {
        try {
            Integer value = (Integer) getInputParameter(inputName);
            if (value == null || value <= 0) {
                throw new ConnectorValidationException(String.format("Mandatory parameter '%s' must be a positive integer but is '%s'.", inputName, value));
            }
        } catch (ClassCastException e) {
            throw new ConnectorValidationException(String.format("'%s' parameter must be an Integer", inputName));
        }
    }

    protected void checkPositiveLongInput(String inputName) throws ConnectorValidationException {
        try {
            Long value = (Long) getInputParameter(inputName);
            if (value == null || value <= 0) {
                throw new ConnectorValidationException(String.format("Mandatory parameter '%s' must be a positive long but is '%s'.", inputName, value));
            }
        } catch (ClassCastException e) {
            throw new ConnectorValidationException(String.format("'%s' parameter must be an Long", inputName));
        }
    }

    /**
     * Validates an 'users' JSON to ensure it has the correct structure and data types.
     *
     * @param inputName The name of the input parameter containing the JSON string.
     * @throws ConnectorValidationException If the parameter is not a String, the JSON is malformed,
     * or it does not comply with the expected structure.
     */
    public void validateUsersJson(String inputName) throws ConnectorValidationException {
        String jsonString = (String) getInputParameter(inputName);

        // 1. Check if the input is null or an empty string.
        if (jsonString == null || jsonString.isEmpty()) {
            throw new ConnectorValidationException(String.format("Mandatory parameter '%s' must be a non-empty string.", inputName));
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode usersNode = objectMapper.readTree(jsonString);

            // 2. Check that the root node is a JSON object.
            if (!usersNode.isObject()) {
                throw new ConnectorValidationException(String.format("Parameter '%s' must be a JSON object.", inputName));
            }

            // 3. Validate the existence and type of each required field.
            validateField(usersNode, "initiator", JsonNode::isBoolean);
            validateField(usersNode, "users", JsonNode::isArray);
            validateField(usersNode, "memberShips", JsonNode::isArray);

        } catch (ClassCastException e) {
            throw new ConnectorValidationException(String.format("Parameter '%s' must be a String.", inputName));
        } catch (Exception e) {
            throw new ConnectorValidationException(String.format("Invalid JSON structure for parameter '%s'.", inputName));
        }
    }

    /**
     * A utility method to validate the existence and type of a specific field.
     * This pattern is a great way to reuse validation logic and make the code cleaner.
     */
    private void validateField(JsonNode parentNode, String fieldName, java.util.function.Predicate<JsonNode> typeCheck) throws ConnectorValidationException {
        JsonNode fieldNode = parentNode.get(fieldName);
        
        // The Optional pattern is a safe way to handle nulls in modern Java.
        Optional.ofNullable(fieldNode)
            .filter(typeCheck)
            .orElseThrow(() -> new ConnectorValidationException(
                String.format("Mandatory field '%s' is missing or has an invalid type.", fieldName)));
    }

    /**
     * @return a list of {@link User} id that are the candidates to execute the task where this filter is defined. 
     * If the result contains a unique user, the task will automatically be assigned.
     * @see AbstractUserFilter#shouldAutoAssignTaskIfSingleResult()
     */
    @Override
    public List<Long> filter(String actorName) throws UserFilterException {
        LOGGER.info(String.format("%s input = %s", USERS_INPUT, getInputParameter(USERS_INPUT)));
        
        APIAccessor apiAccessor = getAPIAccessor();
        ProcessAPI processAPI = apiAccessor.getProcessAPI();
        IdentityAPI identityAPI = apiAccessor.getIdentityAPI();

        InvolvedUsersData involvedUsersData = null;

        try {
            involvedUsersData = parseInvolvedUsersJson((String) getInputParameter(USERS_INPUT));
        } catch (final Exception e) {
            LOGGER.severe("An unexpected error occurred during API accessor or JSON parsing: " + e.getMessage());
            throw new UserFilterException("Initialization failed due to unexpected error.", e);
        }
        
        // Using a Set for efficient duplicate handling
        final Set<Long> userIds = new HashSet<>();

        // 1. Handle process initiator
        if (involvedUsersData.initiator()) {
            try {
                final ProcessInstance processInstance = processAPI.getProcessInstance(getExecutionContext().getRootProcessInstanceId());
                userIds.add(processInstance.getStartedBy());
            } catch (final ProcessInstanceNotFoundException e) {
                LOGGER.severe("Process instance not found for root process ID. Skipping initiator assignment: " + e.getMessage());
            } catch (final Exception e) {
                LOGGER.severe("An unexpected error occurred while getting the process initiator: " + e.getMessage());
            }
        }

        // 2. Add directly specified user IDs
        if (!involvedUsersData.users().isEmpty()) {
            userIds.addAll(involvedUsersData.users());
        }

        // 3. Search for users by memberships (consolidated search)
        if (!involvedUsersData.memberships().isEmpty()) {
            try {
                final SearchOptionsBuilder searchBuilder = new SearchOptionsBuilder(0, Integer.MAX_VALUE);
                searchBuilder.filter(UserSearchDescriptor.ENABLED, true);
                searchBuilder.and();
                
                // Build a single OR query for all memberships
                searchBuilder.leftParenthesis();
                boolean isFirst = true;

                for (final Membership membership : involvedUsersData.memberships()) {
                    final Long groupId = membership.groupId();
                    final Long roleId = membership.roleId();
                    // Only add an 'or' filter if it's not the first element in the loop
                    if (!isFirst) {
                        searchBuilder.or();
                    }
                    if (groupId != null && roleId != null) {
                        searchBuilder.leftParenthesis();
                        searchBuilder.filter(UserSearchDescriptor.GROUP_ID, groupId);
                        searchBuilder.and();
                        searchBuilder.filter(UserSearchDescriptor.ROLE_ID, roleId);
                        searchBuilder.rightParenthesis();
                    } else if (groupId != null) {
                        searchBuilder.filter(UserSearchDescriptor.GROUP_ID, groupId);
                    } else if (roleId != null) {
                        searchBuilder.filter(UserSearchDescriptor.ROLE_ID, roleId);
                    }
                    // Set the flag to false after the first iteration
                    isFirst = false;
                }
                searchBuilder.rightParenthesis();

                final SearchResult<User> searchResult = identityAPI.searchUsers(searchBuilder.done());
                searchResult.getResult().stream()
                    .map(User::getId)
                    .forEach(userIds::add);

                LOGGER.info(String.format("Found %d users from memberships.", searchResult.getCount()));

            } catch (final Exception e) {
                LOGGER.severe("An error occurred during user search by membership: " + e.getMessage());
            }
        }
        
        LOGGER.info(String.format("Final user list contains %d unique users.", userIds.size()));
        return new ArrayList<>(userIds);
    }


    /**
     * Parses the 'involvedUsers' JSON string and extracts initiator, users, and memberships.
     *
     * @param jsonString The JSON string to parse.
     * @return An InvolvedUsersData object containing the parsed data.
     * @throws IllegalArgumentException if the JSON string is null or has an invalid format.
     */
    public InvolvedUsersData parseInvolvedUsersJson(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            throw new IllegalArgumentException("Input JSON string cannot be null or empty.");
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode involvedUsersNode = objectMapper.readTree(jsonString);

            boolean initiator = Optional.ofNullable(involvedUsersNode.get("initiator"))
                                        .filter(JsonNode::isBoolean)
                                        .map(JsonNode::asBoolean)
                                        .orElse(false);

            List<Long> users = Optional.ofNullable(involvedUsersNode.get("users"))
                                       .filter(JsonNode::isArray)
                                       .map(jsonArray -> {
                                           List<Long> userIds = new ArrayList<>();
                                           jsonArray.forEach(node -> userIds.add(node.asLong()));
                                           return userIds;
                                       })
                                       .orElse(new ArrayList<>());
            
            List<Membership> memberships = Optional.ofNullable(involvedUsersNode.get("memberShips"))
                                                  .filter(JsonNode::isArray)
                                                  .map(jsonArray ->
                                                      StreamSupport.stream(jsonArray.spliterator(), false)
                                                              .map(node -> new Membership(
                                                                  node.has("groupId") ? node.get("groupId").asLong() : null,
                                                                  node.has("roleId") ? node.get("roleId").asLong() : null,
                                                                  node.has("memberShipsRef") ? node.get("memberShipsRef").asText() : null
                                                              ))
                                                              .filter(Objects::nonNull)
                                                              .toList()
                                                  )
                                                  .orElse(new ArrayList<>());

            return new InvolvedUsersData(initiator, users, memberships);

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JSON string. Invalid format.", e);
        }
    }

    /**
     * A record to hold the parsed data. Records are a clean way to model immutable data in Java 17.
     */
    public record InvolvedUsersData(boolean initiator, List<Long> users, List<Membership> memberships) {}

    /**
     * A record to model the membership object.
     */
    public record Membership(Long groupId, Long roleId, String memberShipsRef) {}

}


package com.bonitasoft.processbuilder.filter;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessInstanceNotFoundException;
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

/**
 * An actor filter to assign a task to a list of users based on a JSON configuration.
 * The configuration can include the process initiator, a list of user IDs, and/or
 * a list of memberships (group and role IDs) to find candidate users.
 */
public class ActionBasedTaskAssignationFilter extends AbstractUserFilter {

    /**
     * A logger for this class, used to record log messages and provide debugging information.
     */
    private static final Logger LOGGER = Logger.getLogger(ActionBasedTaskAssignationFilter.class.getName());

    /**
     * The name of the input parameter that contains the JSON string with user data.
     * This is a constant value to avoid "magic strings" in the code.
     */
    static final String USERS_INPUT = "users";

    /**
     * Performs validation on the inputs defined for this actor filter.
     * It ensures the 'users' JSON string is present and has a valid structure.
     * @throws ConnectorValidationException if the input parameters are invalid.
     */
    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        validateUsersJson(USERS_INPUT);
    }

    /**
     * Checks if a given input parameter is a positive integer.
     * @param inputName The name of the input parameter.
     * @throws ConnectorValidationException if the parameter is not a positive integer.
     */
    protected void checkPositiveIntegerInput(final String inputName) throws ConnectorValidationException {
        try {
            Integer value = (Integer) getInputParameter(inputName);
            if (value == null || value <= 0) {
                throw new ConnectorValidationException(String.format("Mandatory parameter '%s' must be a positive integer but is '%s'.", inputName, value));
            }
        } catch (ClassCastException e) {
            throw new ConnectorValidationException(String.format("'%s' parameter must be an Integer", inputName));
        }
    }

    /**
     * Checks if a given input parameter is a positive long.
     * @param inputName The name of the input parameter.
     * @throws ConnectorValidationException if the parameter is not a positive long.
     */
    protected void checkPositiveLongInput(final String inputName) throws ConnectorValidationException {
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
     * Validates the 'users' JSON input to ensure it has the correct structure and data types.
     * This method checks for the presence of mandatory fields and their correct types.
     * It also delegates a specific validation for the 'memberShips' list.
     * @param inputName The name of the input parameter containing the JSON string.
     * @throws ConnectorValidationException If the parameter is not a String, the JSON is malformed,
     * or it does not comply with the expected structure.
     */
    public void validateUsersJson(final String inputName) throws ConnectorValidationException {
        String jsonString = null;
        try {
            jsonString = (String) getInputParameter(inputName);
        }  catch (ClassCastException e) {
            throw new ConnectorValidationException(String.format("Parameter '%s' must be a String.", inputName));
        }
        if (jsonString == null || jsonString.isEmpty()) {
            throw new ConnectorValidationException(String.format("Mandatory parameter '%s' must be a non-empty string.", inputName));
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode usersNode = objectMapper.readTree(jsonString);

            if (!usersNode.isObject()) {
                throw new ConnectorValidationException(String.format("Parameter '%s' must be a JSON object.", inputName));
            }

            validateField(usersNode, "initiator", JsonNode::isBoolean, "initiator");
            validateField(usersNode, "users", JsonNode::isArray, "users");
            validateField(usersNode, "memberShips", JsonNode::isArray, "memberShips");
            JsonNode memberShipsNode = usersNode.get("memberShips");
            validateMemberships(memberShipsNode);

        } catch (Exception e) {
            throw new ConnectorValidationException(String.format("Invalid JSON structure for parameter '%s' - ERROR: '%s'.", inputName, e.getMessage()));
        }
    }

    /**
     * Validates that each object in the 'memberShips' list has at least a groupId or a roleId.
     * This method ensures that membership objects are properly formed.
     * @param memberShipsNode The JSON node containing the list of memberships.
     * @throws ConnectorValidationException if any membership is invalid.
     */
    public void validateMemberships(final JsonNode memberShipsNode) throws ConnectorValidationException {
        try {
            StreamSupport.stream(memberShipsNode.spliterator(), false)
                .forEach(membership -> {
                    if (!membership.isObject()) {
                        throw new RuntimeException("Each element in 'memberShips' array must be a JSON object.");
                    }

                    JsonNode groupIdNode = membership.get("groupId");
                    JsonNode roleIdNode = membership.get("roleId");

                    boolean hasValidGroupId = Optional.ofNullable(groupIdNode).filter(JsonNode::isNumber).isPresent();
                    boolean hasValidRoleId = Optional.ofNullable(roleIdNode).filter(JsonNode::isNumber).isPresent();

                    if (!hasValidGroupId && !hasValidRoleId) {
                        throw new RuntimeException("Each membership must have at least a groupId or a roleId.");
                    }
                });
        } catch (RuntimeException e) {
            throw new ConnectorValidationException(e.getMessage());
        }
    }

    /**
     * A utility method to validate the existence and type of a specific field.
     * @param parentNode The parent JSON node to search in.
     * @param fieldName The name of the field to validate.
     * @param typeCheck A predicate to check the type of the field.
     * @param errorMessageField The name of the field to use in the error message.
     * @throws ConnectorValidationException if the field is missing or has an invalid type.
     */
    private void validateField(final JsonNode parentNode, final  String fieldName, final  java.util.function.Predicate<JsonNode> typeCheck, final  String errorMessageField) throws ConnectorValidationException {
        JsonNode fieldNode = parentNode.get(fieldName);

        Optional.ofNullable(fieldNode)
            .filter(typeCheck)
            .orElseThrow(() -> new ConnectorValidationException(
                String.format("Mandatory field '%s' is missing or has an invalid type.", errorMessageField)));
    }

    /**
     * Filters candidate users for a task based on the provided configuration.
     * This method combines users from three sources: the process initiator, a direct list of user IDs,
     * and a list of memberships. Duplicates are automatically removed.
     * @param actorName The name of the actor.
     * @return A list of {@link User} IDs that are candidates to execute the task.
     * @throws UserFilterException if any unexpected error occurs during user filtering.
     * @see AbstractUserFilter#shouldAutoAssignTaskIfSingleResult()
     */
    @Override
    public List<Long> filter(final String actorName) throws UserFilterException {
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

                searchBuilder.leftParenthesis();
                boolean isFirst = true;

                for (final Membership membership : involvedUsersData.memberships()) {
                    final Long groupId = membership.groupId();
                    final Long roleId = membership.roleId();
                    if (!isFirst) {
                        searchBuilder.or();
                    }
                    searchBuilder.leftParenthesis();
                    if (groupId != null && roleId != null) {
                        searchBuilder.filter(UserSearchDescriptor.GROUP_ID, groupId);
                        searchBuilder.and();
                        searchBuilder.filter(UserSearchDescriptor.ROLE_ID, roleId);
                    } else if (groupId != null) {
                        searchBuilder.filter(UserSearchDescriptor.GROUP_ID, groupId);
                    } else if (roleId != null) {
                        searchBuilder.filter(UserSearchDescriptor.ROLE_ID, roleId);
                    }
                    searchBuilder.rightParenthesis();
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
    public InvolvedUsersData parseInvolvedUsersJson(final String jsonString) {
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
                        jsonArray.forEach(node -> {
                            if (!node.isNumber()) {
                                throw new IllegalArgumentException("Failed to parse JSON string. Invalid user ID format.");
                            }
                            userIds.add(node.asLong());
                        });
                        return userIds;
                    })
                    .orElse(new ArrayList<>());

            List<Membership> memberships = Optional.ofNullable(involvedUsersNode.get("memberShips"))
                    .filter(JsonNode::isArray)
                    .map(jsonArray ->
                            StreamSupport.stream(jsonArray.spliterator(), false)
                                    .map(node -> {
                                        JsonNode groupIdNode = node.get("groupId");
                                        if (groupIdNode != null && !groupIdNode.isNull() && !groupIdNode.isNumber()) {
                                            throw new IllegalArgumentException("Failed to parse JSON string. Invalid format.");
                                        }
                                        Long groupId = (groupIdNode != null && groupIdNode.isNumber()) ? groupIdNode.asLong() : null;

                                        JsonNode roleIdNode = node.get("roleId");
                                        if (roleIdNode != null && !roleIdNode.isNull() && !roleIdNode.isNumber()) {
                                            throw new IllegalArgumentException("Failed to parse JSON string. Invalid format.");
                                        }
                                        Long roleId = (roleIdNode != null && roleIdNode.isNumber()) ? roleIdNode.asLong() : null;

                                        JsonNode memberShipsRefNode = node.get("memberShipsRef");
                                        if (memberShipsRefNode != null && !memberShipsRefNode.isNull() && !memberShipsRefNode.isTextual()) {
                                            throw new IllegalArgumentException("Failed to parse JSON string. Invalid format.");
                                        }
                                        String memberShipsRef = (memberShipsRefNode != null && memberShipsRefNode.isTextual()) ? memberShipsRefNode.asText() : null;

                                        return new Membership(groupId, roleId, memberShipsRef);
                                    })
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
     * A record to hold the parsed data for involved users in a process.
     * Records are a clean way to model immutable data in Java 17.
     *
     * @param initiator a boolean indicating if the process initiator should be included
     * @param users a list of user IDs directly specified for the task
     * @param memberships a list of memberships to find candidate users
     */
    public record InvolvedUsersData(boolean initiator, List<Long> users, List<Membership> memberships) {
        /**
         * The compact constructor for this record. It creates defensive copies
         * of the mutable lists to ensure the record's immutability.
         */
        public InvolvedUsersData {
            users = new ArrayList<>(users);
            memberships = new ArrayList<>(memberships);
        }

        /**
         * Returns a defensive copy of the users list to preserve immutability.
         *
         * @return a new ArrayList containing the user IDs.
         */
        public List<Long> users() {
            return new ArrayList<>(this.users);
        }

        /**
         * Returns a defensive copy of the memberships list to preserve immutability.
         *
         * @return a new ArrayList containing the membership objects.
         */
        public List<Membership> memberships() {
            return new ArrayList<>(this.memberships);
        }
    }

    /**
     * A record to model a membership object, which can be defined by a group, a role,
     * or a reference to a membership from the process definition.
     *
     * @param groupId the ID of the group
     * @param roleId the ID of the role
     * @param memberShipsRef a string reference to a membership
     */
    public record Membership(Long groupId, Long roleId, String memberShipsRef) { }
}


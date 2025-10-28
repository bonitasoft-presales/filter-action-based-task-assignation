package com.bonitasoft.processbuilder.filter;

import java.util.List;

import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.bonitasoft.engine.filter.AbstractUserFilter;
import org.bonitasoft.engine.filter.UserFilterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A highly simplified actor filter designed to directly accept and return a non-empty List of user IDs.
 * It strictly validates that the input is a non-empty list of {@code Long}s.
 */
public class MultipleUserIdsActorFilter extends AbstractUserFilter {

    /**
     * A logger for this class, used to record log messages and provide debugging information.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MultipleUserIdsActorFilter.class);


    /**
     * The name of the input parameter that is expected to contain a **direct {@code List<Long>} of user IDs**.
     */
    static final String USERS_LIST_INPUT = "usersList";

    /**
     * Performs validation on the inputs defined for this actor filter.
     * It ensures the 'usersList' parameter is a non-null, non-empty {@code List<Long>}.
     * @throws ConnectorValidationException if the input parameter is null, empty, or not a {@code List<Long>}.
     */
    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        final Object usersListInput = getInputParameter(USERS_LIST_INPUT);
        
        // 1. Check for null
        if (usersListInput == null) {
            throw new ConnectorValidationException(
                String.format("Input parameter '%s' cannot be null. At least one user ID must be provided.", 
                        USERS_LIST_INPUT));
        } 
        
        // 2. Check for List type
        if (!(usersListInput instanceof List)) {
            throw new ConnectorValidationException(
                String.format("Input parameter '%s' must be a List<Long>. Found type %s.", 
                        USERS_LIST_INPUT, usersListInput.getClass().getSimpleName()));
        }
        
        final List<?> list = (List<?>) usersListInput;

        // 3. Check for empty list
        if (list.isEmpty()) {
            throw new ConnectorValidationException(
                String.format("Input parameter '%s' cannot be empty. At least one user ID must be provided.", 
                        USERS_LIST_INPUT));
        }
        
        // 4. Check for correct element type (Long)
        if (!(list.get(0) instanceof Long)) {
            // It's a list, but of the wrong element type.
            throw new ConnectorValidationException(
                    String.format("Input parameter '%s' must be a List<Long>. Found a List with elements of type %s.", 
                            USERS_LIST_INPUT, list.get(0).getClass().getSimpleName()));
        }
    }


    /**
     * Filters candidate users for a task by simply returning the validated list of Long user IDs
     * provided in the 'usersList' input parameter.
     * It assumes the input type and content were validated by {@code validateInputParameters()}.
     * @param actorName The name of the actor. (Ignored)
     * @return The list of {@link Long} user IDs that are candidates to execute the task.
     * @throws UserFilterException if any unexpected error occurs (e.g., failed to retrieve parameter).
     */
    @Override
    public List<Long> filter(final String actorName) throws UserFilterException {
        // We retrieve the parameter. We assume it is a non-null, non-empty List<Long> due to validation.
        final Object usersListInput = getInputParameter(USERS_LIST_INPUT);
        
        LOGGER.info(String.format("Filter called for actor '%s'. Processing input '%s'.", actorName, USERS_LIST_INPUT));

        try {
            // Direct cast is safe because validateInputParameters() was executed first.
            @SuppressWarnings("unchecked")
            List<Long> userIds = (List<Long>) usersListInput;
            
            LOGGER.info(String.format("Successfully retrieved and returned %d user IDs.", userIds.size()));
            return userIds;

        } catch (final Exception e) {
            // Catch any unexpected runtime error (e.g., if a parameter was removed between validation and filter)
            LOGGER.error("An unexpected error occurred during direct user list return.", e);
            throw new UserFilterException("Failed to process validated user list input.", e);
        }
    }
}
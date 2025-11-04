package com.bonitasoft.processbuilder.filter;

import java.util.List;

import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.bonitasoft.engine.filter.AbstractUserFilter;
import org.bonitasoft.engine.filter.UserFilterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A simplified actor filter designed to directly accept and return a non-empty List of user IDs.
 * It strictly validates that the input is a non-empty list of {@code Long}s.
 * 
 * This implementation provides:
 * - Strong input validation
 * - Detailed error messages
 * - Efficient processing
 * - Comprehensive logging
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
     * Error message template for null input.
     */
    private static final String ERR_NULL_INPUT = "Input parameter '%s' cannot be null. At least one user ID must be provided.";

    /**
     * Error message template for non-List type.
     */
    private static final String ERR_NOT_LIST = "Input parameter '%s' must be a List<Long>. Found type %s.";

    /**
     * Error message template for empty list.
     */
    private static final String ERR_EMPTY_LIST = "Input parameter '%s' cannot be empty. At least one user ID must be provided.";

    /**
     * Error message template for wrong element type.
     */
    private static final String ERR_WRONG_ELEMENT_TYPE = "Input parameter '%s' must be a List<Long>. Found a List with elements of type %s.";

    /**
     * Error message for filter processing failure.
     */
    private static final String ERR_FILTER_FAILED = "Failed to process validated user list input.";

    /**
     * Performs validation on the inputs defined for this actor filter.
     * It ensures the 'usersList' parameter is a non-null, non-empty {@code List<Long>}.
     * 
     * Validation steps:
     * 1. Check if input is null
     * 2. Check if input is a List instance
     * 3. Check if list is empty
     * 4. Check if list elements are Long type
     * 
     * @throws ConnectorValidationException if the input parameter is null, empty, or not a {@code List<Long>}.
     */
    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        final Object usersListInput = getInputParameter(USERS_LIST_INPUT);
        
        // Step 1: Check for null
        if (usersListInput == null) {
            String errorMessage = String.format(ERR_NULL_INPUT, USERS_LIST_INPUT);
            LOGGER.error(errorMessage);
            throw new ConnectorValidationException(errorMessage);
        } 
        
        // Step 2: Check for List type
        if (!(usersListInput instanceof List)) {
            String errorMessage = String.format(ERR_NOT_LIST, USERS_LIST_INPUT, usersListInput.getClass().getSimpleName());
            LOGGER.error(errorMessage);
            throw new ConnectorValidationException(errorMessage);
        }
        
        final List<?> list = (List<?>) usersListInput;

        // Step 3: Check for empty list
        if (list.isEmpty()) {
            String errorMessage = String.format(ERR_EMPTY_LIST, USERS_LIST_INPUT);
            LOGGER.error(errorMessage);
            throw new ConnectorValidationException(errorMessage);
        }
        
        // Step 4: Check for correct element type (Long) and not null
        final Object firstElement = list.get(0);
        if (firstElement == null) {
            String errorMessage = String.format(ERR_WRONG_ELEMENT_TYPE, USERS_LIST_INPUT, "null");
            LOGGER.error(errorMessage);
            throw new ConnectorValidationException(errorMessage);
        }
        
        if (!(firstElement instanceof Long)) {
            String errorMessage = String.format(ERR_WRONG_ELEMENT_TYPE, USERS_LIST_INPUT, firstElement.getClass().getSimpleName());
            LOGGER.error(errorMessage);
            throw new ConnectorValidationException(errorMessage);
        }
        
        LOGGER.debug("Input validation successful for parameter '{}'", USERS_LIST_INPUT);
    }

    /**
     * Filters candidate users for a task by simply returning the validated list of Long user IDs
     * provided in the 'usersList' input parameter.
     * It assumes the input type and content were validated by {@code validateInputParameters()}.
     * 
     * @param actorName The name of the actor. (Logged for debugging, but not used for filtering)
     * @return The list of {@link Long} user IDs that are candidates to execute the task.
     * @throws UserFilterException if any unexpected error occurs (e.g., failed to retrieve parameter).
     */
    @Override
    public List<Long> filter(final String actorName) throws UserFilterException {
        // Retrieve the parameter. We assume it is a non-null, non-empty List<Long> due to validation.
        final Object usersListInput = getInputParameter(USERS_LIST_INPUT);
        
        LOGGER.info("Filter called for actor '{}'. Processing input '{}'.", actorName, USERS_LIST_INPUT);
        try {
            // Direct cast is safe because validateInputParameters() was executed first.
            @SuppressWarnings("unchecked")
            List<Long> userIds = (List<Long>) usersListInput;
            
            LOGGER.debug("Successfully retrieved and returning {} user IDs for actor '{}'.", userIds.size(), actorName);
            return userIds;

        } catch (final Exception e) {
            // Catch any unexpected runtime error (e.g., if a parameter was removed between validation and filter)
            LOGGER.error("An unexpected error occurred during user list processing for actor '{}'.", actorName, e);
            throw new UserFilterException(ERR_FILTER_FAILED, e);
        }
    }

}
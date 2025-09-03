package com.bonitasoft.processbuilder.filter;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.bonitasoft.engine.filter.AbstractUserFilter;
import org.bonitasoft.engine.filter.UserFilterException;
import org.bonitasoft.engine.identity.User;

public class ActionBasedTaskAssignationFilter extends AbstractUserFilter {

    private static final Logger LOGGER = Logger.getLogger(ActionBasedTaskAssignationFilter.class.getName());

    static final String MAXIMUM_WORKLOAD_INPUT = "maximumWorkload";

    /**
     * Perform validation on the inputs defined on the actorfilter definition (src/main/resources/filter-action-based-task-assignment.def)
     * You should: 
     * - validate that mandatory inputs are presents
     * - validate that the content of the inputs is coherent with your use case (e.g: validate that a date is / isn't in the past ...)
     */
    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        checkPositiveIntegerInput(MAXIMUM_WORKLOAD_INPUT);
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

    /**
     * @return a list of {@link User} id that are the candidates to execute the task where this filter is defined. 
     * If the result contains a unique user, the task will automatically be assigned.
     * @see AbstractUserFilter#shouldAutoAssignTaskIfSingleResult()
     */
    @Override
    public List<Long> filter(String actorName) throws UserFilterException {
        LOGGER.info(String.format("%s input = %s", MAXIMUM_WORKLOAD_INPUT, getInputParameter(MAXIMUM_WORKLOAD_INPUT)));
        APIAccessor apiAccessor = getAPIAccessor();
        ProcessAPI processAPI = apiAccessor.getProcessAPI();
        List<Long> users = processAPI.getUserIdsForActor(getExecutionContext().getProcessDefinitionId(), actorName, 0, Integer.MAX_VALUE);
        return users.stream()
                .filter( userId -> !isOverloaded(userId, apiAccessor))
                .collect(Collectors.toList());
    }
    

    private boolean isOverloaded(Long userId, APIAccessor apiAccessor) {
        return apiAccessor.getProcessAPI().getNumberOfAssignedHumanTaskInstances(userId) >= (Integer) getInputParameter(MAXIMUM_WORKLOAD_INPUT);
    }
}


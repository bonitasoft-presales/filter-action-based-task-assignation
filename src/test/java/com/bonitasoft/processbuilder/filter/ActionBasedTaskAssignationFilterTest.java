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
import org.bonitasoft.engine.search.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static com.bonitasoft.processbuilder.filter.ActionBasedTaskAssignationFilter.USERS_INPUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActionBasedTaskAssignationFilterTest {

    @InjectMocks
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

    // --- Validation Tests ---

    @Test
    void validateInputParameters_should_throw_exception_if_input_is_null() {
        Map<String, Object> parameters = new HashMap<>();
        filter.setInputParameters(parameters);

        assertThrows(ConnectorValidationException.class, () -> filter.validateInputParameters());
    }

    @Test
    void validateInputParameters_should_throw_exception_if_input_is_malformed_json() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(USERS_INPUT, "invalid json");
        filter.setInputParameters(parameters);

        assertThrows(ConnectorValidationException.class, () -> filter.validateInputParameters());
    }

    @Test
    void validateInputParameters_should_throw_exception_if_json_structure_is_invalid() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(USERS_INPUT, "{\"initiator\": \"true\", \"users\": null, \"memberShips\": null}");
        filter.setInputParameters(parameters);

        assertThrows(ConnectorValidationException.class, () -> filter.validateInputParameters());
    }
    

}
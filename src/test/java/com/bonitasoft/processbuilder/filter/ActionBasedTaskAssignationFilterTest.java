package com.bonitasoft.processbuilder.filter;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.bonitasoft.engine.connector.EngineExecutionContext;
import org.bonitasoft.engine.filter.UserFilterException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bonitasoft.processbuilder.filter.ActionBasedTaskAssignationFilter.MAXIMUM_WORKLOAD_INPUT;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActionBasedTaskAssignationFilterTest {

    @InjectMocks
    private ActionBasedTaskAssignationFilter filter;

    @Mock(lenient = true)
    private APIAccessor apiAccessor;
    @Mock(lenient = true)
    private ProcessAPI processApi;

    @Mock(lenient = true)
    private EngineExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        when(apiAccessor.getProcessAPI()).thenReturn(processApi);
        when(executionContext.getProcessDefinitionId()).thenReturn(1L);
    }

    @Test
    public void should_throw_exception_if_mandatory_input_is_missing() {
        assertThrows(ConnectorValidationException.class, () ->
                filter.validateInputParameters()
        );
    }

    @Test
    public void should_throw_exception_if_mandatory_input_is_not_positive_integer() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(MAXIMUM_WORKLOAD_INPUT, -1);
        filter.setInputParameters(parameters);
        assertThrows(ConnectorValidationException.class, () ->
                filter.validateInputParameters()
        );
    }

    @Test
    public void should_throw_exception_if_mandatory_input_is_not_an_integer() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(MAXIMUM_WORKLOAD_INPUT, "1");
        filter.setInputParameters(parameters);
        assertThrows(ConnectorValidationException.class, () ->
                filter.validateInputParameters()
        );
    }

    @Test
    public void should_return_a_list_of_candidates() throws UserFilterException {
        // Given
        when(processApi.getUserIdsForActor(anyLong(), eq("MyActor"), eq(0), eq(Integer.MAX_VALUE)))
                .thenReturn(asList(1L, 2L, 3L));
        when(processApi.getNumberOfAssignedHumanTaskInstances(1L)).thenReturn(2L);
        when(processApi.getNumberOfAssignedHumanTaskInstances(2L)).thenReturn(3L);
        when(processApi.getNumberOfAssignedHumanTaskInstances(3L)).thenReturn(0L);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(MAXIMUM_WORKLOAD_INPUT, 3);
        filter.setInputParameters(parameters);

        // When
        List<Long> candidates = filter.filter("MyActor");

        // Then
        assertThat(candidates).as("Only users with a workload below the maximum can be candidates.")
                .containsExactly(1L, 3L);

    }

}

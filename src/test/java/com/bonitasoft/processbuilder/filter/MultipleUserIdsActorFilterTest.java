package com.bonitasoft.processbuilder.filter;

import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.bonitasoft.engine.filter.UserFilterException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for MultipleUserIdsActorFilter, ensuring maximum code coverage
 * for the simplified logic (direct List<Long> return).
 * This test uses the correct Bonita API method: setInputParameters(Map<String, Object>).
 */
public class MultipleUserIdsActorFilterTest {

    private MultipleUserIdsActorFilter filter;
    
    // Campo para almacenar el valor del parámetro "usersList" simulado
    private Object storedInputParameter; 

    /**
     * Setup a fresh filter instance before each test.
     */
    @BeforeEach
    public void setUp() {
        // Inicializar el valor almacenado antes de cada prueba
        storedInputParameter = null; 
        
        // Mocking del filtro para inyectar y obtener parámetros
        filter = new MultipleUserIdsActorFilter() {

            // 1. Override getInputParameter para devolver el valor almacenado
            @Override
            public Object getInputParameter(String parameterName) {
                if (MultipleUserIdsActorFilter.USERS_LIST_INPUT.equals(parameterName)) {
                    return storedInputParameter;
                }
                return null;
            }

            // 2. Override setInputParameters para simular el paso de parámetros desde Bonita
            @Override
            public void setInputParameters(Map<String, Object> parameters) {
                // Almacenamos el valor que está bajo la clave esperada por nuestro filtro
                storedInputParameter = parameters.get(MultipleUserIdsActorFilter.USERS_LIST_INPUT);
            }
        };
    }

    // =========================================================================
    // TESTS FOR validateInputParameters() - COVERAGE: 100% of validation paths
    // =========================================================================

    /**
     * Test case 1: Successful validation with a valid List<Long>.
     */
    @Test
    public void validateInputParameters_shouldPassWithValidListLong() {
        // Arrange
        List<Long> validList = Arrays.asList(101L, 102L, 103L);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(MultipleUserIdsActorFilter.USERS_LIST_INPUT, validList);
        
        // Simulación de inyección de parámetros (llama a setInputParameters)
        filter.setInputParameters(parameters); 

        // Act & Assert
        assertDoesNotThrow(() -> filter.validateInputParameters());
    }
    
    // --- Error Cases (Expected ConnectorValidationException) ---

    /**
     * Test case 2: Validation should fail if the input is null.
     */
    @Test
    public void validateInputParameters_shouldFailIfNull() {
        // Arrange (Al no llamar a setInputParameters, storedInputParameter es null)
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(MultipleUserIdsActorFilter.USERS_LIST_INPUT, null);
        filter.setInputParameters(parameters); 
        
        // Act & Assert (Covers: Check for null)
        ConnectorValidationException e = assertThrows(ConnectorValidationException.class, 
            () -> filter.validateInputParameters());
        
        assertTrue(e.getMessage().contains("cannot be null"), "Expected failure for null input.");
    }

    /**
     * Test case 3: Validation should fail if the input is a list, but empty.
     */
    @Test
    public void validateInputParameters_shouldFailIfEmptyList() {
        // Arrange
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(MultipleUserIdsActorFilter.USERS_LIST_INPUT, Collections.emptyList());
        filter.setInputParameters(parameters);

        // Act & Assert (Covers: Check for empty list)
        ConnectorValidationException e = assertThrows(ConnectorValidationException.class, 
            () -> filter.validateInputParameters());

        assertTrue(e.getMessage().contains("cannot be empty"), "Expected failure for empty list.");
    }

    /**
     * Test case 4: Validation should fail if the input is not a List.
     */
    @Test
    public void validateInputParameters_shouldFailIfNotAList() {
        // Arrange: Usar un String en lugar de una List
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(MultipleUserIdsActorFilter.USERS_LIST_INPUT, "a string");
        filter.setInputParameters(parameters);

        // Act & Assert (Covers: Check for List type)
        ConnectorValidationException e = assertThrows(ConnectorValidationException.class, 
            () -> filter.validateInputParameters());
        
        assertTrue(e.getMessage().contains("must be a List<Long>"), "Expected failure for non-list type.");
        assertTrue(e.getMessage().contains("String"), "Expected message to show found type.");
    }
    
    /**
     * Test case 5: Validation should fail if the input is a List but contains Integers.
     */
    @Test
    public void validateInputParameters_shouldFailIfWrongListElementType() {
        // Arrange: List<Integer>
        List<Integer> wrongTypeList = Arrays.asList(1, 2, 3);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(MultipleUserIdsActorFilter.USERS_LIST_INPUT, wrongTypeList);
        filter.setInputParameters(parameters);

        // Act & Assert (Covers: Check for correct element type (Long))
        ConnectorValidationException e = assertThrows(ConnectorValidationException.class, 
            () -> filter.validateInputParameters());
        
        assertTrue(e.getMessage().contains("Found a List with elements of type Integer"), "Expected failure for List<Integer>.");
    }


    // =========================================================================
    // TESTS FOR filter() - COVERAGE: Successful path and error path
    // =========================================================================

    /**
     * Test case 6: Successful filtering where the validated List<Long> is returned.
     */
    @Test
    public void filter_shouldReturnInputListSuccessfully() throws UserFilterException {
        // Arrange (Assumption: Validation already passed)
        List<Long> expectedUserIds = Arrays.asList(400L, 500L, 600L);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(MultipleUserIdsActorFilter.USERS_LIST_INPUT, expectedUserIds);
        filter.setInputParameters(parameters);

        // Act
        List<Long> resultUserIds = filter.filter("myActor");

        // Assert (Check if the returned list is exactly the input list)
        assertNotNull(resultUserIds);
        assertEquals(expectedUserIds.size(), resultUserIds.size());
        assertEquals(expectedUserIds, resultUserIds, "The returned list must match the input list.");
    }

    /**
     * Test case 7: Test the exception handling path inside filter()
     * by simulating a scenario where the input type is corrupted (e.g., set to a String),
     * forcing the ClassCastException inside the try-catch block of filter().
     */
    @Test
    public void filter_shouldThrowUserFilterExceptionOnError() {
        // Arrange: Simular la corrupción de datos: el valor almacenado es un String.
        // NOTA: Esto simula un fallo de seguridad o de entorno, ya que la validación
        // debería haberlo impedido, forzando la ruta de error en 'filter()'.
        this.storedInputParameter = "I am a string, not a List<Long>";

        // Act & Assert (Covers: try-catch block in filter)
        UserFilterException e = assertThrows(UserFilterException.class, 
            () -> filter.filter("myActor"));

        assertTrue(e.getCause() instanceof ClassCastException, "Expected a ClassCastException as the cause of the UserFilterException.");
        assertTrue(e.getMessage().contains("Failed to process validated user list input"), "Expected UserFilterException for internal error.");
    }
}
package com.bonitasoft.presales.connector;

import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class connectorRabbitMQTest {

    connectorRabbitMQ connector;

    @BeforeEach
    public void setUp() {
        // Initialize your connector before each test
        connector = new connectorRabbitMQ();
    }

    @Test
    public void testValidateInputParameters_validInputs() {
        // Set valid input parameters using setInputParameters method
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(connectorRabbitMQ.HOST_INPUT_PARAMETER, "localhost");
        parameters.put(connectorRabbitMQ.QUEUENAME_INPUT_PARAMETER, "testQueue");
        parameters.put(connectorRabbitMQ.MESSAGE_INPUT_PARAMETER, "Hello, World!");
        connector.setInputParameters(parameters);

        // Validate inputs
        try {

            connector.validateInputParameters(); // Should pass without exception
        } catch (ConnectorValidationException e) {
            fail("Validation failed for valid inputs");
        }
    }

    @Test
    public void testValidateInputParameters_invalidHost() {
        // Set valid input parameters using setInputParameters method
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(connectorRabbitMQ.HOST_INPUT_PARAMETER, 123); // This should be a string
        parameters.put(connectorRabbitMQ.QUEUENAME_INPUT_PARAMETER, "testQueue");
        parameters.put(connectorRabbitMQ.MESSAGE_INPUT_PARAMETER, "Hello, World!");
        connector.setInputParameters(parameters);

        // Validate inputs and expect an exception
        assertThrows(ConnectorValidationException.class, () -> {
            connector.validateInputParameters();
        });
    }

    
}

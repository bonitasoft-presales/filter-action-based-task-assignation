package com.bonitasoft.presales.connector;

import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        parameters.put(connectorRabbitMQ.QUEUENAME_INPUT_PARAMETER, "BonitaQueue");
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
        parameters.put(connectorRabbitMQ.QUEUENAME_INPUT_PARAMETER, "BonitaQueue");
        parameters.put(connectorRabbitMQ.MESSAGE_INPUT_PARAMETER, "Hello, World!");
        connector.setInputParameters(parameters);

        // Validate inputs and expect an exception
        assertThrows(ConnectorValidationException.class, () -> {
            connector.validateInputParameters();
        });
    }

    @Test
    public void testValidateInputParameters_invalidQueueName() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(RabbitMQConstants.HOST_INPUT_PARAMETER, "localhost");
        parameters.put(RabbitMQConstants.QUEUENAME_INPUT_PARAMETER, 456); // Invalid queueName type
        parameters.put(RabbitMQConstants.MESSAGE_INPUT_PARAMETER, "Hello, World!");
        connector.setInputParameters(parameters);

        assertThrows(ConnectorValidationException.class, () -> connector.validateInputParameters());
    }

    @Test
    public void testValidateInputParameters_invalidMessage() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(RabbitMQConstants.HOST_INPUT_PARAMETER, "localhost");
        parameters.put(RabbitMQConstants.QUEUENAME_INPUT_PARAMETER, "BonitaQueue");
        parameters.put(RabbitMQConstants.MESSAGE_INPUT_PARAMETER, true); // Invalid message type
        connector.setInputParameters(parameters);

        assertThrows(ConnectorValidationException.class, () -> connector.validateInputParameters());
    }

    @Test
    public void testExecuteBusinessLogic() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(RabbitMQConstants.HOST_INPUT_PARAMETER, "localhost");
        parameters.put(RabbitMQConstants.QUEUENAME_INPUT_PARAMETER, "BonitaQueue");
        parameters.put(RabbitMQConstants.MESSAGE_INPUT_PARAMETER, "Test Bonita Message");
        parameters.put(RabbitMQConstants.USERNAME_INPUT_PARAMETER, "admin"); 
        parameters.put(RabbitMQConstants.PASSWORD_INPUT_PARAMETER, "password"); 

        connector.setInputParameters(parameters);
        try {
           
            Map<String, Object> outputs = connector.execute();
            assertThat(outputs)
            .containsKey(RabbitMQConstants.RECEIVEDMESSAGE_OUTPUT_PARAMETER)
            .satisfies(map -> assertThat(map.get(RabbitMQConstants.RECEIVEDMESSAGE_OUTPUT_PARAMETER)).isNotNull());
        } catch (ConnectorException e) {
            fail("Execution failed: " + e.getMessage());
        }
    }

    
}

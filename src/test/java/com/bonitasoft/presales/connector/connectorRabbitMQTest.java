package com.bonitasoft.presales.connector;

import com.rabbitmq.client.*;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class connectorRabbitMQTest {

    private connectorRabbitMQ connector;

    @Mock
    private ConnectionFactory connectionFactory;

    @Mock
    private Connection connection;

    @Mock
    private Channel channel;

    @BeforeEach
    public void setUp() throws IOException, TimeoutException {
        MockitoAnnotations.openMocks(this);
        connector = new connectorRabbitMQ();

        when(connectionFactory.newConnection()).thenReturn(connection);
        when(connection.createChannel()).thenReturn(channel);
    }

    @Test
    public void testValidateInputParameters_invalidHost() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(connectorRabbitMQ.HOST_INPUT_PARAMETER, 123);
        parameters.put(connectorRabbitMQ.QUEUENAME_INPUT_PARAMETER, "BonitaQueue");
        parameters.put(connectorRabbitMQ.MESSAGE_INPUT_PARAMETER, "Hello, World!");
        connector.setInputParameters(parameters);

        assertThrows(ConnectorValidationException.class, () -> connector.validateInputParameters());
    }

    @Test
    public void testValidateInputParameters_invalidQueueName() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(RabbitMQConstants.HOST_INPUT_PARAMETER, "localhost");
        parameters.put(RabbitMQConstants.QUEUENAME_INPUT_PARAMETER, 456); 
        parameters.put(RabbitMQConstants.MESSAGE_INPUT_PARAMETER, "Hello, World!");
        connector.setInputParameters(parameters);

        assertThrows(ConnectorValidationException.class, () -> connector.validateInputParameters());
    }

    @Test
    public void testValidateInputParameters_invalidMessage() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(RabbitMQConstants.HOST_INPUT_PARAMETER, "localhost");
        parameters.put(RabbitMQConstants.QUEUENAME_INPUT_PARAMETER, "BonitaQueue");
        parameters.put(RabbitMQConstants.MESSAGE_INPUT_PARAMETER, true); 
        connector.setInputParameters(parameters);

        assertThrows(ConnectorValidationException.class, () -> connector.validateInputParameters());
    }

    @Test
    public void testValidateInputParameters_missingHost() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(RabbitMQConstants.QUEUENAME_INPUT_PARAMETER, "BonitaQueue");
        parameters.put(RabbitMQConstants.MESSAGE_INPUT_PARAMETER, "Test Message");
        connector.setInputParameters(parameters);
    
        assertThrows(ConnectorValidationException.class, () -> connector.validateInputParameters());
    }
    
    @Test
    public void testValidateInputParameters_missingQueueName() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(RabbitMQConstants.HOST_INPUT_PARAMETER, "localhost");
        parameters.put(RabbitMQConstants.MESSAGE_INPUT_PARAMETER, "Test Message");
        connector.setInputParameters(parameters);
    
        assertThrows(ConnectorValidationException.class, () -> connector.validateInputParameters());
    }
    
    @Test
    public void testValidateInputParameters_missingMessage() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(RabbitMQConstants.HOST_INPUT_PARAMETER, "localhost");
        parameters.put(RabbitMQConstants.QUEUENAME_INPUT_PARAMETER, "BonitaQueue");
        connector.setInputParameters(parameters);
    
        assertThrows(ConnectorValidationException.class, () -> connector.validateInputParameters());
    }
    
    @Test
    public void testValidateInputParameters_nullValues() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(RabbitMQConstants.HOST_INPUT_PARAMETER, null);
        parameters.put(RabbitMQConstants.QUEUENAME_INPUT_PARAMETER, null);
        parameters.put(RabbitMQConstants.MESSAGE_INPUT_PARAMETER, null);
        connector.setInputParameters(parameters);
    
        assertThrows(ConnectorValidationException.class, () -> connector.validateInputParameters());
    }

/*
    @Test
    public void testExecuteBusinessLogic_messageFound() throws ConnectorException, IOException, TimeoutException {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(connectorRabbitMQ.HOST_INPUT_PARAMETER, "localhost");
        parameters.put(connectorRabbitMQ.QUEUENAME_INPUT_PARAMETER, "BonitaQueue");
        parameters.put(connectorRabbitMQ.MESSAGE_INPUT_PARAMETER, "Test Bonita Message");
        parameters.put(connectorRabbitMQ.USERNAME_INPUT_PARAMETER, "admin");
        parameters.put(connectorRabbitMQ.PASSWORD_INPUT_PARAMETER, "password");

        connector.setInputParameters(parameters);

        // Simular la recepción de un mensaje
        doAnswer(invocation -> {
            DeliverCallback deliverCallback = invocation.getArgument(2);
            String message = "Test Bonita Message";
            Delivery delivery = new Delivery(new Envelope(1L, false, "exchange", "routingKey"), new AMQP.BasicProperties(), message.getBytes(StandardCharsets.UTF_8));
            deliverCallback.handle(null, delivery);
            return null;
        }).when(channel).basicConsume(eq("BonitaQueue"), eq(false), any(DeliverCallback.class), any(CancelCallback.class));

        Map<String, Object> outputs = connector.execute();

        assertThat(outputs)
                .containsKey(connectorRabbitMQ.RECEIVEDMESSAGE_OUTPUT_PARAMETER)
                .satisfies(map -> assertThat(map.get(connectorRabbitMQ.RECEIVEDMESSAGE_OUTPUT_PARAMETER)).isEqualTo("Test Bonita Message"));

        verify(channel).queueDeclare("BonitaQueue", true, false, false, null);
        verify(channel).basicConsume(eq("BonitaQueue"), eq(false), any(DeliverCallback.class), any(CancelCallback.class));
        verify(channel).basicAck(1L, false);
    }
*/
/*
    @Test
    public void testExecuteBusinessLogic_messageNotFound() throws ConnectorException, IOException, TimeoutException {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(connectorRabbitMQ.HOST_INPUT_PARAMETER, "localhost");
        parameters.put(connectorRabbitMQ.QUEUENAME_INPUT_PARAMETER, "BonitaQueue");
        parameters.put(connectorRabbitMQ.MESSAGE_INPUT_PARAMETER, "Mensaje Especial");
        parameters.put(connectorRabbitMQ.USERNAME_INPUT_PARAMETER, "admin");
        parameters.put(connectorRabbitMQ.PASSWORD_INPUT_PARAMETER, "password");

        connector.setInputParameters(parameters);

        // Simular la recepción de un mensaje que no coincide
        doAnswer(invocation -> {
            DeliverCallback deliverCallback = invocation.getArgument(2);
            String message = "Test Bonita Message"; // Simula que siempre llega el mismo mensaje
            Delivery delivery = new Delivery(new Envelope(1L, false, "exchange", "routingKey"), new AMQP.BasicProperties(), message.getBytes(StandardCharsets.UTF_8));
            deliverCallback.handle(null, delivery);
            return null;
        }).when(channel).basicConsume(eq("BonitaQueue"), eq(false), any(DeliverCallback.class), any(CancelCallback.class));

        Map<String, Object> outputs = connector.execute();

        assertThat(outputs)
                .containsKey(connectorRabbitMQ.RECEIVEDMESSAGE_OUTPUT_PARAMETER)
                .satisfies(map -> assertThat(map.get(connectorRabbitMQ.RECEIVEDMESSAGE_OUTPUT_PARAMETER)).isNull());

        verify(channel).queueDeclare("BonitaQueue", true, false, false, null);
        verify(channel).basicConsume(eq("BonitaQueue"), eq(false), any(DeliverCallback.class), any(CancelCallback.class));
        verify(channel).basicAck(1L, false);
    }*/
}
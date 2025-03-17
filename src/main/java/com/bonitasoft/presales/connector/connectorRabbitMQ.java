package com.bonitasoft.presales.connector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.ShutdownSignalException;

public class connectorRabbitMQ extends AbstractConnector implements RabbitMQConstants{

    private static final Logger LOGGER = Logger.getLogger(connectorRabbitMQ.class.getName());

    protected final java.lang.String getHost() {
        return (java.lang.String) getInputParameter(HOST_INPUT_PARAMETER);
    }

    protected final java.lang.String getQueueName() {
        return (java.lang.String) getInputParameter(QUEUENAME_INPUT_PARAMETER);
    }

    protected final java.lang.String getMessage() {
        return (java.lang.String) getInputParameter(MESSAGE_INPUT_PARAMETER);
    }
    
    protected final java.lang.String getUsername() {
        return (java.lang.String) getInputParameter(USERNAME_INPUT_PARAMETER);
    }
    
    protected final java.lang.String getPassword() {
        return (java.lang.String) getInputParameter(PASSWORD_INPUT_PARAMETER);
    }

    protected final void setReceivedMessage(java.lang.String receivedMessage) {
        setOutputParameter(RECEIVEDMESSAGE_OUTPUT_PARAMETER, receivedMessage);
    }


    @Override
    public void executeBusinessLogic() throws ConnectorException {
        LOGGER.info("Starting RabbitMQ message consumption.");
        try {
            ConnectionFactory factory = createConnectionFactory();
            try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
                declareQueue(channel);
                consumeAndFindMessage(channel);
            } catch (IOException | TimeoutException | ShutdownSignalException e) {
                handleException(e, "Error during message consumption.");
            } catch (Exception e) {
                handleException(e, "Unexpected error during message consumption.");
            }
        } catch (Exception e) {
            handleException(e, "Error creating RabbitMQ connection factory.");
        }
    }

    private ConnectionFactory createConnectionFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        LOGGER.info(String.format("createConnectionFactory - Username: (%s) - Password: (%s) - Host: (%s)",getUsername(), getPassword(), getHost()));
        factory.setHost(getHost());
        factory.setUsername(getUsername());
        factory.setPassword(getPassword());
        factory.setVirtualHost("/");
        LOGGER.info("ConnectionFactory created and configured.");
        return factory;
    }

    private void declareQueue(Channel channel) throws IOException, ConnectorException {
        channel.queueDeclare(getQueueName(), true, false, false, null);
        LOGGER.info("Queue declared: " + getQueueName());
    }

    private void consumeAndFindMessage(Channel channel) throws IOException, ConnectorException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            LOGGER.info(" [x] Received message: " + message);
    
            if (message.contains(getMessage())) {
                LOGGER.info(" [!] Found message: " + message);
                setReceivedMessage(message);
                LOGGER.info(" [!] setReceivedMessage called with: " + message); // Log agregado
                try {
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error acknowledging message", e);
                }
                try {
                    channel.close();
                } catch (IOException | TimeoutException e) {
                    LOGGER.log(Level.SEVERE, "Error closing channel", e);
                }
            } else {
                try {
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error acknowledging message", e);
                }
            }
        };
    
        try {
            channel.basicConsume(getQueueName(), false, deliverCallback, consumerTag -> {
            });
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error consuming message", e);
            throw new ConnectorException("Error consuming message", e);
        }
    }

    private void handleException(Exception e, String message) throws ConnectorException {
        LOGGER.log(Level.SEVERE, message, e);
        throw new ConnectorException(message, e);
    }


    /**
     * Validates input parameters to ensure they are not null and have the correct type.
     * If any validation fails, a ConnectorValidationException is thrown with detailed error messages.
     * The validated values are logged for reference.
     * 
     * @throws ConnectorValidationException if any of the input parameters are missing or of an invalid type.
     */
    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        // StringBuilder to accumulate error messages
        StringBuilder errors = new StringBuilder();
        try {
            // List of input parameters and their names
            Object[] inputParams = {getHost(), getQueueName(), getMessage(), getUsername(), getPassword()};
            String[] paramNames = {"host", "queueName", "message", "username", "password"};

        
            // Validate null values and types using streams
            IntStream.range(0, inputParams.length).forEach(i -> {
                // Check if the parameter is null
                if (inputParams[i] == null) {
                    errors.append(paramNames[i]).append(" is missing\n");
                } 
                // Check if the parameter is not a String
                else if (!(inputParams[i] instanceof String)) {
                    errors.append(paramNames[i]).append(" should be a String but was ").append(inputParams[i].getClass().getSimpleName()).append("\n");
                }
            });
            
            // If there are any validation errors, throw an exception with all the accumulated error messages
            if (errors.length() > 0) {
                throw new ConnectorValidationException(errors.toString().trim());
            }
    
        } catch (ClassCastException e) {
            // Capture ClassCastException and throw a ConnectorValidationException
            throw new ConnectorValidationException("Invalid type encountered during validation: " + e.getMessage());
        }

        // Log the validated parameters for debugging purposes
        LOGGER.info(String.format("Input parameters validated - Retrieved host: (%s), queueName: (%s), message: (%s), username: (%s), password: (%s)",
            getHost(), getQueueName(), getMessage(), getUsername(), getPassword()));
    }

}


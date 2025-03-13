package com.bonitasoft.presales.connector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
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
        LOGGER.info("Starting RabbitMQ message publication.");
        try {
            validateInputs();
            ConnectionFactory factory = createConnectionFactory();
            try (Connection connection = factory.newConnection();
                 Channel channel = connection.createChannel()) {
                declareQueue(channel);
                publishMessage(channel);
                setReceivedMessageOutput();

            } catch (IOException | TimeoutException e) {
                handleConnectionOrChannelException(e);
            } catch (ShutdownSignalException e) {
                handlePublishShutdownException(e);
            } catch (Exception e) {
                handlePublishGeneralException(e);
            }

        } catch (ClassCastException e) {
            handleClassCastException(e);
        } catch (Exception e) {
            handleGeneralException(e);
        }
    }

    private void validateInputs() throws ConnectorException {
        if (getHost() == null || getQueueName() == null || getMessage() == null || getUsername() == null || getPassword() == null) {
            throw new ConnectorException("Required input parameters are missing.");
        }
        LOGGER.info(String.format("Input parameters validated - Retrieved host: %s, queueName: %s, message: %s, username: %s, password: ********",
        getHost(), getQueueName(), getMessage(), getUsername()));
    }

    private ConnectionFactory createConnectionFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(getHost());
        factory.setUsername(getUsername());
        factory.setPassword(getPassword());
        LOGGER.info("ConnectionFactory created and configured.");
        return factory;
    }

    private void declareQueue(Channel channel) throws IOException, ConnectorException {
        channel.queueDeclare(getQueueName(), true, false, false, null);
        LOGGER.info("Queue declared: " + getQueueName());
    }

    private void publishMessage(Channel channel) throws IOException, ConnectorException {
        channel.basicPublish("", getQueueName(), null, getMessage().getBytes(StandardCharsets.UTF_8));
        LOGGER.info("Message published to queue: " + getQueueName());
    }

    private void setReceivedMessageOutput() {
        String receivedMessage = "Message '" + getMessage() + "' sent to queue " + getQueueName() + " on host " + getHost();
        setReceivedMessage(receivedMessage);
        LOGGER.info("Received message output set.");
    }

    private void handleConnectionOrChannelException(Exception e) throws ConnectorException {
        LOGGER.log(Level.SEVERE, "Error establishing connection or channel to RabbitMQ on host " + getHost(), e);
        throw new ConnectorException("Error connecting to RabbitMQ", e);
    }

    private void handlePublishShutdownException(ShutdownSignalException e) throws ConnectorException {
        LOGGER.log(Level.SEVERE, "Channel shutdown while publishing message to queue " + getQueueName() + " on host " + getHost(), e);
        throw new ConnectorException("Error publishing message due to channel shutdown", e);
    }

    private void handlePublishGeneralException(Exception e) throws ConnectorException {
        LOGGER.log(Level.SEVERE, "Error publishing message to queue " + getQueueName() + " on host " + getHost(), e);
        throw new ConnectorException("Error publishing message", e);
    }

    private void handleClassCastException(ClassCastException e) throws ConnectorException {
        LOGGER.log(Level.SEVERE, "Error casting input parameters", e);
        throw new ConnectorException("Error casting input parameters", e);
    }

    private void handleGeneralException(Exception e) throws ConnectorException {
        LOGGER.log(Level.SEVERE, "Unexpected error in RabbitMQ connector", e);
        throw new ConnectorException("Unexpected error", e);
    }

    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        try {
            getHost();
        } catch (ClassCastException cce) {
            throw new ConnectorValidationException("host type is invalid");
        }
        try {
            getQueueName();
        } catch (ClassCastException cce) {
            throw new ConnectorValidationException("queueName type is invalid");
        }
        try {
            getMessage();
        } catch (ClassCastException cce) {
            throw new ConnectorValidationException("message type is invalid");
        }
        try {
            getUsername();
        } catch (ClassCastException cce) {
            throw new ConnectorValidationException("username type is invalid");
        }
        try {
            getPassword();
        } catch (ClassCastException cce) {
            throw new ConnectorValidationException("password type is invalid");
        }
    }
}

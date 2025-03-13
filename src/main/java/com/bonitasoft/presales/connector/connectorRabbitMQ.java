package com.bonitasoft.presales.connector;

import java.util.logging.Logger;

import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;

import com.bonitasoft.presales.connector.RabbitMQConstants;

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

    protected final void setReceivedMessage(java.lang.String receivedMessage) {
        setOutputParameter(RECEIVEDMESSAGE_OUTPUT_PARAMETER, receivedMessage);
    }

    @Override
    public void executeBusinessLogic() throws ConnectorException {
        try {
            String host = getHost();
            String queueName = getQueueName();
            String message = getMessage();

            String receivedMessage = "Message received from queue " + queueName + " on host " + host;

            setReceivedMessage(receivedMessage);

            LOGGER.info("Successfully processed the message from RabbitMQ");

        } catch (Exception e) {
            throw new ConnectorException("Error executing RabbitMQ connector", e);
        }
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
    }
}

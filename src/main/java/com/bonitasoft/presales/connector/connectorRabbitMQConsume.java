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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.ShutdownSignalException;

public class connectorRabbitMQConsume extends AbstractConnector implements RabbitMQConstants{

    private static final Logger LOGGER = Logger.getLogger(connectorRabbitMQConsume.class.getName());

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
        Connection connection = null;
        Channel channel = null;
        try {
            ConnectionFactory factory = createConnectionFactory();
            connection = factory.newConnection();
            channel = connection.createChannel();
            boolean hayMensajes = hayMensajesEnCola(channel);
            if (hayMensajes) {
                LOGGER.info("La cola tiene mensajes, procesando...");
                consumeMenssages(channel);
            } else {
                LOGGER.info("No hay más mensajes en la cola.");
            }
            declareQueue(channel);
            consumeAndFindMessage(channel);
        } catch (IOException | TimeoutException | ShutdownSignalException e) {
            handleException(e, "Error during message consumption.");
        } catch (Exception e) {
            handleException(e, "Unexpected error during message consumption.");
        } finally {
            LOGGER.log(Level.INFO, "Closing connection or channel...");
            try {
                if (channel != null && channel.isOpen()) {
                    channel.close();
                }
                if (connection != null && connection.isOpen()) {
                    connection.close();
                }
            } catch (IOException | TimeoutException e) {
                LOGGER.log(Level.SEVERE, "Error closing connection or channel", e);
            }
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

    private void consumeMenssages(Channel channel) throws IOException {
        GetResponse response;
        Integer index = 0;
        while ((response = channel.basicGet(getQueueName(), false)) != null) {
            String message = new String(response.getBody(), "UTF-8");
            LOGGER.info((index++).toString()+" - Mensaje recibido: " + message);

            // Procesar mensaje aquí...

            // Confirmar procesamiento
            channel.basicAck(response.getEnvelope().getDeliveryTag(), false);

        }
        LOGGER.info("Todos los mensajes han sido procesados.");
    }

    private void consumeAndFindMessage(Channel channel) throws IOException, ConnectorException {
        String consumerTag = null; // Para almacenar el consumerTag
    
        DeliverCallback deliverCallback = (tag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            LOGGER.info(" [x] Received message: " + message);
            Boolean existPersistenceId = false;
            try {
                existPersistenceId = checkPersistenceId(message);
                LOGGER.info(" [!] existPersistenceId: " + existPersistenceId.toString());
            } catch (ConnectorException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
            LOGGER.info(" [!] check if existPersistenceId: ");
            if (existPersistenceId) {
                LOGGER.info(" [!] Found message: " + message);
                try {
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    LOGGER.info(" [!] Message acknowledged successfully (existPersistenceId).");
                    channel.basicCancel(tag); // Cancelar el consumo
                    LOGGER.info(" [!] Consumer cancelled after finding message.");
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error acknowledging or cancelling", e);
                }
            } else {
                LOGGER.info(" [!] Message does not match search criteria. Acknowledging message.");
                try {
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    LOGGER.info(" [!] Message acknowledged successfully. (!existPersistenceId)");
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error acknowledging message", e);
                }
            }
            setReceivedMessage(message);
            LOGGER.info(" [!] setReceivedMessage called with: " + message);
        };
    
        try {
            consumerTag = channel.basicConsume(getQueueName(), false, deliverCallback, consumerTagValue -> {});
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error consuming message", e);
            throw new ConnectorException("Error consuming message", e);
        }
    }

    private void handleException(Exception e, String message) throws ConnectorException {
        LOGGER.log(Level.SEVERE, message, e);
        throw new ConnectorException(message, e);
    }

    private boolean hayMensajesEnCola(Channel channel) throws IOException {
        AMQP.Queue.DeclareOk queueInfo = channel.queueDeclarePassive(getQueueName());
        return queueInfo.getMessageCount() > 0;
    }

    private Boolean checkPersistenceId(String jsonString) throws ConnectorException {
        Boolean result = false;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                JsonNode jsonNode = objectMapper.readTree(jsonString);

                if (jsonNode.has("persistenceId")) { //Verifica que exista la propiedad.
                    Long persistenceId = jsonNode.get("persistenceId").asLong();
                    Long mensaje = Long.valueOf(getMessage());
                    LOGGER.info("El valor de persistenceId (" + persistenceId.toString() + ") - getMensaje() (" + getMessage() + ")");

                    if (persistenceId.equals(mensaje)) { //Usar equals para comparar Longs
                        LOGGER.info("El valor de persistenceId (" + persistenceId + ") es igual a getMensaje() (" + getMessage() + ")");
                        result = true;
                    }
                } else{
                    LOGGER.info("JSON no contiene persistenceId");
                }

            } catch (JsonParseException e) {
                // No es JSON válido, realizar búsqueda de texto
                LOGGER.info("Cadena no es JSON, realizando búsqueda de texto.");
                if (jsonString.contains(getMessage())) {
                    LOGGER.info("Cadena contiene: " + getMessage());
                    result = true;
                }
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error checking persistenceId", e);
            throw new ConnectorException("Error checking persistenceId", e);
        } catch (NumberFormatException e){
            LOGGER.log(Level.SEVERE, "Error al convertir getMessage() a Long", e);
            throw new ConnectorException("Error al convertir getMessage() a Long", e);
        }
        return result;
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


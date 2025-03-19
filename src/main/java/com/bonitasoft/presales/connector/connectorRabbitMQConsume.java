package com.bonitasoft.presales.connector;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;
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
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.ShutdownSignalException;

public class connectorRabbitMQConsume extends AbstractConnector implements RabbitMQConstants{

    private static final Logger LOGGER = Logger.getLogger(connectorRabbitMQConsume.class.getName());

    @FunctionalInterface
    interface ValidationStrategy {
        void validate(Object paramValue, String paramName, StringBuilder errors);
    }

    // Map para los getters de parámetros usando Supplier
    private final Map<String, Supplier<Object>> paramGetters = Map.of(
        HOST_INPUT_PARAMETER, this::getHost,
        QUEUENAME_INPUT_PARAMETER, this::getQueueName,
        PERSISTENCE_ID_INPUT_PARAMETER, this::getPersistenceId,
        USERNAME_INPUT_PARAMETER, this::getUsername,
        PASSWORD_INPUT_PARAMETER, this::getPassword,
        DURABLE_INPUT_PARAMETER, this::getDurable,
        EXCLUSIVE_INPUT_PARAMETER, this::getExclusive,
        AUTODELETE_INPUT_PARAMETER, this::getAutoDelete,
        ARGUMENTS_INPUT_PARAMETER, this::getArguments
    );

    // Map para las estrategias de validación
    private final Map<String, ValidationStrategy> validationStrategies = Map.of(
        HOST_INPUT_PARAMETER, this::validateStringParam,
        QUEUENAME_INPUT_PARAMETER, this::validateStringParam,
        PERSISTENCE_ID_INPUT_PARAMETER, this::validateStringParam,
        USERNAME_INPUT_PARAMETER, this::validateStringParam,
        PASSWORD_INPUT_PARAMETER, this::validateStringParam,
        DURABLE_INPUT_PARAMETER, this::validateBooleanParam,
        EXCLUSIVE_INPUT_PARAMETER, this::validateBooleanParam,
        AUTODELETE_INPUT_PARAMETER, this::validateBooleanParam,
        ARGUMENTS_INPUT_PARAMETER, this::validateMapParam
    );

    protected final java.lang.String getHost() {
        return (java.lang.String) getInputParameter(HOST_INPUT_PARAMETER);
    }

    protected final java.lang.String getQueueName() {
        return (java.lang.String) getInputParameter(QUEUENAME_INPUT_PARAMETER);
    }

    protected final java.lang.String getPersistenceId() {
        return (java.lang.String) getInputParameter(PERSISTENCE_ID_INPUT_PARAMETER);
    }
    
    protected final java.lang.String getUsername() {
        return (java.lang.String) getInputParameter(USERNAME_INPUT_PARAMETER);
    }
    
    protected final java.lang.String getPassword() {
        return (java.lang.String) getInputParameter(PASSWORD_INPUT_PARAMETER);
    }

    protected final java.lang.Boolean getDurable() {
        return (java.lang.Boolean) getInputParameter(DURABLE_INPUT_PARAMETER);
    }
    
    protected final java.lang.Boolean getExclusive() {
        return (java.lang.Boolean) getInputParameter(EXCLUSIVE_INPUT_PARAMETER);
    }
    
    protected final java.lang.Boolean getAutoDelete() {
        return (java.lang.Boolean) getInputParameter(AUTODELETE_INPUT_PARAMETER);
    }
    
    protected final java.util.Map<java.lang.String, java.lang.Object> getArguments() {
        return (java.util.Map<java.lang.String, java.lang.Object>) getInputParameter(ARGUMENTS_INPUT_PARAMETER);
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
                LOGGER.info(" [!] La cola tiene mensajes, procesando...");
                consumeMenssages(channel);
            } else {
                LOGGER.info(" [!] No hay más mensajes en la cola.");
            }
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
        LOGGER.info(String.format("createConnectionFactory - Username: (%s) - Password: (*****) - Host: (%s)",getUsername(), getPassword(), getHost()));
        factory.setHost(getHost());
        factory.setUsername(getUsername());
        factory.setPassword(getPassword());
        factory.setVirtualHost("/");
        LOGGER.info("ConnectionFactory created and configured.");
        return factory;
    }
/* 
    private void declareQueue(Channel channel) throws IOException, ConnectorException {
        channel.queueDeclare(getQueueName(), true, false, false, null);
        LOGGER.info("Queue declared: " + getQueueName());
    }*/

    private void consumeMenssages(Channel channel) throws IOException {
        LOGGER.info(" [!] begin consumeMenssages: ");
        GetResponse response;
        Integer index = 0;
        setReceivedMessage(null);
        while ((response = channel.basicGet(getQueueName(), false)) != null) {
            String message = new String(response.getBody(), "UTF-8");
            LOGGER.info((index++).toString()+" - Mensaje recibido: " + message);

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
                    LOGGER.info(" [!]  Message does match search criteria. Acknowledging message. (existPersistenceId).");
                    setReceivedMessage(message);
                    // Confirmar procesamiento
                    channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
                    LOGGER.info(" [!] Message acknowledged successfully.");
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error acknowledging or cancelling", e);
                }
            } else {
                LOGGER.info(" [!] Message does not match search criteria. Not acknowledging message. (!existPersistenceId)");
            }
        }
        LOGGER.info(" [!] Todos los mensajes han sido procesados.");
    }
/*
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
*/
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
                    Long mensaje = Long.valueOf(getPersistenceId());
                    LOGGER.info("El valor de persistenceId (" + persistenceId.toString() + ") - getPersistenceId() (" + getPersistenceId() + ")");

                    if (persistenceId.equals(mensaje)) { //Usar equals para comparar Longs
                        LOGGER.info("El valor de persistenceId (" + persistenceId + ") es igual a getPersistenceId() (" + getPersistenceId() + ")");
                        result = true;
                    }
                } else{
                    LOGGER.info("JSON no contiene persistenceId");
                }

            } catch (JsonParseException e) {
                // No es JSON válido, realizar búsqueda de texto
                LOGGER.info("Cadena no es JSON, realizando búsqueda de texto.");
                if (jsonString.contains(getPersistenceId())) {
                    LOGGER.info("Cadena contiene: " + getPersistenceId());
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

    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        StringBuilder errors = new StringBuilder();

        try {
            // Validación dinámica de parámetros
            validationStrategies.forEach((paramName, validate) -> {
                Object paramValue = paramGetters.get(paramName).get(); // Usar el Supplier para obtener el valor
                validate.validate(paramValue, paramName, errors); // Validar el parámetro
            });

            // Si hay errores, lanzar una excepción
            if (errors.length() > 0) {
                throw new ConnectorValidationException(errors.toString().trim());
            }

        } catch (ClassCastException e) {
            throw new ConnectorValidationException("Invalid type encountered during validation: " + e.getMessage());
        }

        logValidatedParameters();
    }

    private void logValidatedParameters() {
        Map<String, Object> params = new HashMap<>();
        paramGetters.forEach((paramName, getter) -> {
            params.put(paramName, getter.get()); // Obtener y loguear cada parámetro
        });

        StringBuilder logMessage = new StringBuilder("Input parameters validated - ");
        params.forEach((key, value) -> 
            logMessage.append(String.format("%s: (%s), ", key, value))
        );

        if (logMessage.length() > 0) {
            logMessage.setLength(logMessage.length() - 2); // Eliminar la última coma
        }

        LOGGER.info(logMessage.toString());
    }



    private void validateStringParam(Object param, String paramName, StringBuilder errors) {
        if (param == null) {
            errors.append(paramName).append(" is missing\n");
        } else if (!(param instanceof String)) {
            errors.append(paramName).append(" should be a String but was ").append(param.getClass().getSimpleName()).append("\n");
        }
    }

    private void validateBooleanParam(Object param, String paramName, StringBuilder errors) {
        if (param == null) {
            errors.append(paramName).append(" is missing\n");
        } else if (!(param instanceof Boolean)) {
            errors.append(paramName).append(" should be a Boolean but was ").append(param.getClass().getSimpleName()).append("\n");
        }
    }

    private void validateMapParam(Object param, String paramName, StringBuilder errors) {
        if (param == null) {
            errors.append(paramName).append(" is missing\n");
        } else if (!(param instanceof Map)) {
            errors.append(paramName).append(" should be a Map<String, Object> but was ").append(param.getClass().getSimpleName()).append("\n");
        }
    }

}


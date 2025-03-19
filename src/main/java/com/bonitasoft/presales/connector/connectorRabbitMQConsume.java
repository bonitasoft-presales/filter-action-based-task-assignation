package com.bonitasoft.presales.connector;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import com.rabbitmq.client.Method;
import com.rabbitmq.client.RpcClient.Response;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.AMQP.BasicProperties;

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
        PASSWORD_INPUT_PARAMETER, this::getPassword
    );

    // Map para indicar si los parámetros son opcionales
    private final Map<String, Boolean> optionalParameters = Map.of(
        HOST_INPUT_PARAMETER, false,
        QUEUENAME_INPUT_PARAMETER, false,
        PERSISTENCE_ID_INPUT_PARAMETER, false,
        USERNAME_INPUT_PARAMETER, false,
        PASSWORD_INPUT_PARAMETER, false
    );

    // Map para las estrategias de validación (se construye dinámicamente)
    private final Map<String, ValidationStrategy> validationStrategies;

    // Map para obtener las validaciones opcionales y obligatorias
    private final Map<String, BiFunction<connectorRabbitMQConsume, String, ValidationStrategy>> validationStrategiesMap = Map.of(
        HOST_INPUT_PARAMETER, (instance, paramName) -> instance::validateStringParam,
        QUEUENAME_INPUT_PARAMETER, (instance, paramName) -> instance::validateStringParam,
        PERSISTENCE_ID_INPUT_PARAMETER, (instance, paramName) -> instance::validateStringParam,
        USERNAME_INPUT_PARAMETER, (instance, paramName) -> instance::validateStringParam,
        PASSWORD_INPUT_PARAMETER, (instance, paramName) -> instance::validateStringParam
    );

    private final Map<String, BiFunction<connectorRabbitMQConsume, String, ValidationStrategy>> optionalValidationStrategiesMap = Map.of(
        HOST_INPUT_PARAMETER, (instance, paramName) -> instance::validateOptionalStringParam,
        QUEUENAME_INPUT_PARAMETER, (instance, paramName) -> instance::validateOptionalStringParam,
        PERSISTENCE_ID_INPUT_PARAMETER, (instance, paramName) -> instance::validateOptionalStringParam,
        USERNAME_INPUT_PARAMETER, (instance, paramName) -> instance::validateOptionalStringParam,
        PASSWORD_INPUT_PARAMETER, (instance, paramName) -> instance::validateOptionalStringParam
    );

    public connectorRabbitMQConsume() {
        validationStrategies = buildValidationStrategies();
    }

    private Map<String, ValidationStrategy> buildValidationStrategies() {
        Map<String, ValidationStrategy> strategies = new HashMap<>();

        optionalParameters.forEach((paramName, isOptional) -> {
            if (isOptional) {
                strategies.put(paramName, optionalValidationStrategiesMap.getOrDefault(paramName, (instance, name) -> instance::validateStringParam).apply(this, paramName));
            } else {
                strategies.put(paramName, validationStrategiesMap.getOrDefault(paramName, (instance, name) -> instance::validateStringParam).apply(this, paramName));
            }
        });

        return strategies;
    }

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

    protected final void setBody(java.lang.String body) {
        setOutputParameter(BODY_OUTPUT_PARAMETER, body);
    }

    protected final void setHeaders(java.util.Map headers) {
        setOutputParameter(HEADERS_OUTPUT_PARAMETER, headers);
    }


    @Override
    public void executeBusinessLogic() throws ConnectorException {
        LOGGER.info("Starting RabbitMQ message consumption.");
        Connection connection = null;
        Channel channel = null;
        setBody(null);
        setHeaders(null);
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
        while ((response = channel.basicGet(getQueueName(), false)) != null) {
            BasicProperties basicProperties = response.getProps();
            Map<String, Object> headers = basicProperties.getHeaders();
            String body = new String(response.getBody(), "UTF-8");

            LOGGER.info((index++).toString()+" - Mensaje recibido: " + body);

            Boolean existPersistenceId = false;
            try {
                existPersistenceId = validateMessage(body, headers);
                LOGGER.info(" [!] existPersistenceId: " + existPersistenceId.toString());
            } catch (ConnectorException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
            LOGGER.info(" [!] check if existPersistenceId ");
            if (existPersistenceId) {
                LOGGER.info(" [!] Found message: " + body);
                try {
                    LOGGER.info(" [!]  Message does match search criteria. Acknowledging message. (existPersistenceId).");
                    setBody(body);
                    setHeaders(headers);
                    // Confirmar procesamiento
                    channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
                    LOGGER.info(" [!] Message acknowledged successfully.");
                } catch (IOException e) {
                    LOGGER.severe("Error acknowledging or cancelling" + e);
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
        LOGGER.log(Level.INFO, "[!] hayMensajesEnCola");
        try {
            AMQP.Queue.DeclareOk queueInfo = channel.queueDeclarePassive(getQueueName());
            return queueInfo.getMessageCount() > 0;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[ERROR] IOException - {0}", e.getMessage());
    
            Throwable cause = e.getCause();  // Obtener la causa raíz
            if (cause instanceof ShutdownSignalException shutdownSignalException) {
                LOGGER.log(Level.INFO, "[ERROR] ShutdownSignalException: {0}", shutdownSignalException.getMessage());
    
                if (shutdownSignalException.getReason() instanceof AMQP.Channel.Close close) {
                    if (close.getReplyCode() == 404) {
                        // La cola no existe (NOT_FOUND)
                        LOGGER.log(Level.INFO, "La cola {0} no existe.", getQueueName());
                        return false;
                    }
                }
            }
            throw e;  // Relanzar IOException si no es un error manejable
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[ERROR] Unexpected Exception - {0}", e.getMessage());
            return false;
        }
    }
    
    

    private Boolean validateMessage(String body, Map<String, Object> headers) throws ConnectorException {
        Boolean checkPersistenceIsInBody = checkPersistenceIsInBody(body);
        Boolean checkPersistenceIsInHeaders = checkPersistenceIsInHeaders(headers);
        Boolean checkIfItIsResponseTypeInHeaders = checkIfItIsResponseTypeInHeaders(headers);
        LOGGER.log(Level.INFO, "validateMessage: checkPersistenceIsInBody: {} - checkPersistenceIsInHeaders: {} - checkIfItIsResponseTypeInHeaders: {}",  new Object[] {checkPersistenceIsInBody, checkPersistenceIsInHeaders, checkIfItIsResponseTypeInHeaders});
        return (checkPersistenceIsInBody || checkPersistenceIsInHeaders(headers)) && checkIfItIsResponseTypeInHeaders(headers);
    }
    
    private Boolean checkPersistenceIsInBody(String body) throws ConnectorException {
        Boolean result = false;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                JsonNode jsonNode = objectMapper.readTree(body);
                if (jsonNode.has("persistenceId")) { //Verifica que exista la propiedad.
                    Long persistenceId = jsonNode.get("persistenceId").asLong();
                    Long mensaje = Long.valueOf(getPersistenceId());
                    LOGGER.info("El valor de persistenceId (" + persistenceId.toString() + ") - getPersistenceId() (" + getPersistenceId() + ")");
                    result = persistenceId.equals(mensaje);
                } else{
                    LOGGER.info("JSON no contiene persistenceId");
                }
            } catch (JsonParseException e) {
                // No es JSON válido, realizar búsqueda de texto
                LOGGER.info("Cadena no es JSON, realizando búsqueda de texto.");
                if (body.contains(getPersistenceId())) {
                    LOGGER.info("Cadena contiene: " + getPersistenceId());
                    result = true;
                }
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error checking persistenceId in body", e);
            throw new ConnectorException("Error checking persistenceId in body", e);
        } catch (NumberFormatException e){
            LOGGER.log(Level.SEVERE, "Error al convertir getMessage() a Long", e);
            throw new ConnectorException("Error al convertir getMessage() a Long", e);
        }
        return result;
    }
    
    private Boolean checkPersistenceIsInHeaders(Map<String, Object> headers)  throws ConnectorException  {
        try {
            if (headers != null) {
                if (headers.containsKey("persistenceId")) {
                    Object persistenceIdValue = headers.get("persistenceId");
                    if (persistenceIdValue != null) {
                            String persistenceId = persistenceIdValue.toString();
                            LOGGER.log(Level.INFO, "persistenceId from Headers: {} - getPersistenceId(): {}",  new Object[] {persistenceId, getPersistenceId()});
                            return persistenceId.equals(getPersistenceId());
                    } else {
                        LOGGER.info("persistenceId value in Headers is null.");
                        return false;
                    }
                } else {
                    LOGGER.info("Map does not contain persistenceId.");
                    return false;
                }
            } else {
                LOGGER.info("Does not contain the headers map");
                return false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking persistenceId in headers", e);
            throw new ConnectorException("Error checking persistenceId in headers", e);
        }
    }

    private Boolean checkIfItIsResponseTypeInHeaders(Map<String, Object> headers)  throws ConnectorException  {
        try {
            if (headers != null) {
                if (headers.containsKey("type")) {
                    Object type = headers.get("type");
                    if (type != null) {
                            String typeStr = type.toString();
                            LOGGER.log(Level.INFO, "typeStr from Headers: {} - TYPE_RESPONSE(): {}",  new Object[] {typeStr.toUpperCase(), TYPE_RESPONSE.toUpperCase()});
                            return (typeStr.toUpperCase()).equals(TYPE_RESPONSE.toUpperCase());
                    } else {
                        LOGGER.info("type value in Headers is null.");
                        return false;
                    }
                } else {
                    LOGGER.info("Map does not contain type.");
                    return false;
                }
            } else {
                LOGGER.info("Does not contain the headers map");
                return false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking type in headers", e);
            throw new ConnectorException("Error checking type in headers", e);
        }
    }

    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        StringBuilder errors = new StringBuilder();

        try {
            validationStrategies.forEach((paramName, validate) -> {
                Object paramValue = paramGetters.get(paramName).get(); // Usar el Supplier para obtener el valor
                if (optionalParameters.get(paramName)) {
                    validateOptionalParam(paramValue, paramName, errors);
                } else {
                    validate.validate(paramValue, paramName, errors);
                }
            });

            if (errors.length() > 0) {
                throw new ConnectorValidationException(errors.toString().trim());
            }

        } catch (ClassCastException e) {
            throw new ConnectorValidationException("Invalid type encountered during validation: " + e.getMessage());
        }

        logValidatedParameters();
    }

    private void validateOptionalParam(Object param, String paramName, StringBuilder errors) {
        if (param != null) {
            validationStrategies.get(paramName).validate(param, paramName, errors);
        }
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

    private void validateOptionalStringParam(Object param, String paramName, StringBuilder errors) {
        if (param != null && !(param instanceof String)) {
            errors.append(paramName).append(" should be a String or null but was ").append(param.getClass().getSimpleName()).append("\n");
        }
    }

    private void validateBooleanParam(Object param, String paramName, StringBuilder errors) {
        if (param == null) {
            errors.append(paramName).append(" is missing\n");
        } else if (!(param instanceof Boolean)) {
            errors.append(paramName).append(" should be a Boolean but was ").append(param.getClass().getSimpleName()).append("\n");
        }
    }

    private void validateOptionalBooleanParam(Object param, String paramName, StringBuilder errors) {
        if (param != null && !(param instanceof Boolean)) {
            errors.append(paramName).append(" should be a Boolean or null but was ").append(param.getClass().getSimpleName()).append("\n");
        }
    }

    private void validateMapParam(Object param, String paramName, StringBuilder errors) {
        if (param == null) {
            errors.append(paramName).append(" is missing\n");
        } else if (!(param instanceof Map)) {
            errors.append(paramName).append(" should be a Map<String, Object> but was ").append(param.getClass().getSimpleName()).append("\n");
        }
    }

    private void validateOptionalMapParam(Object param, String paramName, StringBuilder errors) {
        if (param != null && !(param instanceof Map)) {
            errors.append(paramName).append(" should be a Map<String, Object> or null but was ").append(param.getClass().getSimpleName()).append("\n");
        }
    }

}


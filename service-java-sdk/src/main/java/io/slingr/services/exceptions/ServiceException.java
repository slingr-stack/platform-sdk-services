package io.slingr.services.exceptions;

import io.slingr.services.services.exchange.Parameter;
import io.slingr.services.utils.Json;
import io.slingr.services.utils.converters.JsonSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * This is a generic service exception you can use to handle custom errors and so you don't need
 * to create new exceptions.
 *
 * <p>Created by dgaviola on 21/05/15.
 */
public abstract class ServiceException extends RuntimeException implements JsonSource {
    private static final Logger logger = LoggerFactory.getLogger(ServiceException.class);

    public static final String REST_CODE_EXCEPTION = "Invalid response code ";

    private static final String PARSE_ADDITIONAL_INFO = "additionalInfo";
    private static final String PARSE_ORIGINAL_MESSAGE = "originalMessage";
    private static final String PARSE_MESSAGE = "message";
    private static final String PARSE_DETAILS = "details";
    private static final String PARSE_BODY = "body";

    private final ErrorCode code;
    private final Json additionalInfo;
    private int returnCode = 500;

    ServiceException(ErrorCode code, String message, Json additionalInfo, Throwable cause) {
        super(StringUtils.defaultIfEmpty(message, ""), cause);
        this.code = code != null ? code : ErrorCode.GENERAL;
        this.additionalInfo = additionalInfo != null ? additionalInfo : Json.map();
    }

    /**
     * Returns true if the exception is retryable. An exception is retryable because the action that throws the
     * exception can be completed using the same parameters in the future (i.e. temporal network issues)
     *
     * @return true if the exception is retryable
     */
    public boolean isRetryable(){
        return false;
    }

    /**
     * Returns true if the exception is permanent. An exception is permanent because the action always throws the same
     * exception on the same conditions (i.e. 'invalid token')
     *
     * @return true if the exception is permanent
     */
    public final boolean isPermanent(){
        return !isRetryable();
    }

    /**
     * Gets the type of error that can happened
     * @return the type of error that can happened
     */
    public ErrorCode getCode() {
        return code;
    }

    /**
     * Gets the json that includes information related to the error
     *
     * @return the json that includes information related to the error
     */
    public Json getAdditionalInfo() {
        return additionalInfo;
    }

    /**
     * Adds additional data about the exception
     *
     * @param key key of the data to include
     * @param value value of the data to include
     */
    public void addData(String key, Object value){
        additionalInfo.set(key, value);
    }

    /**
     * Returns the status code of the response. If the request was not made or timed out, it will return -1.
     *
     * @return the status code of the response or -1 if there is no response
     */
    public int getHttpStatusCode() {
        if (getCause() instanceof WebApplicationException) {
            WebApplicationException wae = (WebApplicationException) getCause();
            Response r = wae.getResponse();
            return r.getStatus();
        }
        return -1;
    }

    /**
     * Gets the return code to response to HTTP clients with this exception
     *
     * @return http code
     */
    public int getReturnCode() {
        return returnCode;
    }

    /**
     * Sets the return code to response to HTTP clients with this exception
     *
     * @param returnCode http code
     */
    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    /**
     * Sets the return code to response to HTTP clients with this exception and returns the exception
     *
     * @param returnCode http code
     */
    public ServiceException returnCode(int returnCode) {
        setReturnCode(returnCode);
        return this;
    }

    /**
     * Generates a json representation of a service exception with the given parameters
     *
     * @param error type of error that can happened
     * @param description error message
     * @return service exception
     */
    public static Json json(ErrorCode error, Object description){
        return newException(error, description, null, true, null).toJson();
    }

    /**
     * Generates a json representation of a service exception with the given parameters
     *
     * @param error type of error that can happened
     * @param description error message
     * @param additionalInfo json that permits to include information related to the error
     * @return service exception
     */
    public static Json json(ErrorCode error, Object description, Json additionalInfo){
        return newException(error, description, additionalInfo, true, null).toJson();
    }

    /**
     * Generates a json representation of a service exception with the given parameters
     *
     * @param error type of error that can happened
     * @param description error message
     * @param cause exception that generates this exception
     * @return service exception
     */
    public static Json json(ErrorCode error, Object description, Throwable cause){
        return newException(error, description, null, true, cause).toJson();
    }

    /**
     * Generates a json representation of a service exception with the given parameters
     *
     * @param error type of error that can happened
     * @param description error message
     * @param additionalInfo json that permits to include information related to the error
     * @param cause exception that generates this exception
     * @return service exception
     */
    public static Json json(ErrorCode error, Object description, Json additionalInfo, Throwable cause){
        return newException(error, description, additionalInfo, true, cause).toJson();
    }

    /**
     * Generates a json representation of a service exception with the given parameters
     *
     * @param includeFlag true if the flag must be included on the Json
     * @param error type of error that can happened
     * @param description error message
     * @return service exception
     */
    public static Json json(boolean includeFlag, ErrorCode error, Object description){
        return newException(error, description, null, true, null).toJson(includeFlag);
    }

    /**
     * Generates a json representation of a service exception with the given parameters
     *
     * @param includeFlag true if the flag must be included on the Json
     * @param error type of error that can happened
     * @param description error message
     * @param additionalInfo json that permits to include information related to the error
     * @return service exception
     */
    public static Json json(boolean includeFlag, ErrorCode error, Object description, Json additionalInfo){
        return newException(error, description, additionalInfo, true, null).toJson(includeFlag);
    }

    /**
     * Generates a json representation of a service exception with the given parameters
     *
     * @param includeFlag true if the flag must be included on the Json
     * @param error type of error that can happened
     * @param description error message
     * @param cause exception that generates this exception
     * @return service exception
     */
    public static Json json(boolean includeFlag, ErrorCode error, Object description, Throwable cause){
        return newException(error, description, null, true, cause).toJson(includeFlag);
    }

    /**
     * Generates a json representation of a service exception with the given parameters
     *
     * @param includeFlag true if the flag must be included on the Json
     * @param error type of error that can happened
     * @param description error message
     * @param additionalInfo json that permits to include information related to the error
     * @param cause exception that generates this exception
     * @return service exception
     */
    public static Json json(boolean includeFlag, ErrorCode error, Object description, Json additionalInfo, Throwable cause){
        return newException(error, description, additionalInfo, true, cause).toJson(includeFlag);
    }

    /**
     * Generates a new retryable service exception with the given parameters. This exception is retryable because the
     * action that throws this exception can be completed using the same parameters in the future (i.e. temporal network
     * issues)
     *
     * @param error type of error that can happened
     * @param description error message
     * @return service exception
     */
    public static RetryableException retryable(ErrorCode error, Object description){
        return (RetryableException) newException(error, description, null, false, null);
    }

    /**
     * Generates a new retryable service exception with the given parameters. This exception is retryable because the
     * action that throws this exception can be completed using the same parameters in the future (i.e. temporal network
     * issues)
     *
     * @param error type of error that can happened
     * @param description error message
     * @param additionalInfo json that permits to include information related to the error
     * @return service exception
     */
    public static RetryableException retryable(ErrorCode error, Object description, Json additionalInfo){
        return (RetryableException) newException(error, description, additionalInfo, false, null);
    }

    /**
     * Generates a new retryable service exception with the given parameters. This exception is retryable because the
     * action that throws this exception can be completed using the same parameters in the future (i.e. temporal network
     * issues)
     *
     * @param error type of error that can happened
     * @param description error message
     * @param cause exception that generates this exception
     * @return service exception
     */
    public static RetryableException retryable(ErrorCode error, Object description,Throwable cause){
        return (RetryableException) newException(error, description, null, false, cause);
    }

    /**
     * Generates a new retryable service exception with the given parameters. This exception is retryable because the
     * action that throws this exception can be completed using the same parameters in the future (i.e. temporal network
     * issues)
     *
     * @param error type of error that can happened
     * @param description error message
     * @param additionalInfo json that permits to include information related to the error
     * @param cause exception that generates this exception
     * @return service exception
     */
    public static RetryableException retryable(ErrorCode error, Object description, Json additionalInfo, Throwable cause){
        return (RetryableException) newException(error, description, additionalInfo, false, cause);
    }

    /**
     * Generates a new permanent service exception with the given parameters. This exception is permanent because the
     * action always throws this exception on the same conditions (i.e. 'invalid token')
     *
     * @param error type of error that can happened
     * @param description error message
     * @return service exception
     */
    public static PermanentException permanent(ErrorCode error, Object description){
        return (PermanentException) newException(error, description, null, true, null);
    }

    /**
     * Generates a new permanent service exception with the given parameters. This exception is permanent because the
     * action always throws this exception on the same conditions (i.e. 'invalid token')
     *
     * @param error type of error that can happened
     * @param description error message
     * @param additionalInfo json that permits to include information related to the error
     * @return service exception
     */
    public static PermanentException permanent(ErrorCode error, Object description, Json additionalInfo){
        return (PermanentException) newException(error, description, additionalInfo, true, null);
    }

    /**
     * Generates a new permanent service exception with the given parameters. This exception is permanent because the
     * action always throws this exception on the same conditions (i.e. 'invalid token')
     *
     * @param error type of error that can happened
     * @param description error message
     * @param cause exception that generates this exception
     * @return service exception
     */
    public static PermanentException permanent(ErrorCode error, Object description, Throwable cause){
        return (PermanentException) newException(error, description, null, true, cause);
    }

    /**
     * Generates a new permanent service exception with the given parameters. This exception is permanent because the
     * action always throws this exception on the same conditions (i.e. 'invalid token')
     *
     * @param error type of error that can happened
     * @param description error message
     * @param additionalInfo json that permits to include information related to the error
     * @param cause exception that generates this exception
     * @return service exception
     */
    public static PermanentException permanent(ErrorCode error, Object description, Json additionalInfo, Throwable cause){
        return (PermanentException) newException(error, description, additionalInfo, true, cause);
    }

    /**
     * Generates a new service exception with the given parameters
     *
     * @param error type of error that can happened
     * @param description error message
     * @param permanent true if the error is permanent (the action always throws this exception on the same conditions,
     *                 i.e. 'invalid token')
     * @return service exception
     */
    public static ServiceException exception(ErrorCode error, Object description, boolean permanent){
        return newException(error, description, null, permanent, null);
    }

    /**
     * Generates a new service exception with the given parameters
     *
     * @param error type of error that can happened
     * @param description error message
     * @param additionalInfo json that permits to include information related to the error
     * @param permanent true if the error is permanent (the action always throws this exception on the same conditions,
     *                 i.e. 'invalid token')
     * @return service exception
     */
    public static ServiceException exception(ErrorCode error, Object description, Json additionalInfo, boolean permanent){
        return newException(error, description, additionalInfo, permanent, null);
    }

    /**
     * Generates a new service exception with the given parameters
     *
     * @param error type of error that can happened
     * @param description error message
     * @param permanent true if the error is permanent (the action always throws this exception on the same conditions,
     *                 i.e. 'invalid token')
     * @param cause exception that generates this exception
     * @return service exception
     */
    public static ServiceException exception(ErrorCode error, Object description, boolean permanent, Throwable cause){
        return newException(error, description, null, permanent, cause);
    }

    /**
     * Generates a new service exception with the given parameters
     *
     * @param error type of error that can happened
     * @param description error message
     * @param additionalInfo json that permits to include information related to the error
     * @param permanent true if the error is permanent (the action always throws this exception on the same conditions,
     *                 i.e. 'invalid token')
     * @param cause exception that generates this exception
     * @return service exception
     */
    public static ServiceException exception(ErrorCode error, Object description, Json additionalInfo, boolean permanent, Throwable cause){
        return newException(error, description, additionalInfo, permanent, cause);
    }

    /**
     * Generates a new service exception with the given parameters
     *
     * @param error type of error that can happened
     * @param description error message
     * @param additionalInfo json that permits to include information related to the error
     * @param permanent true if the error is permanent (the action always throws this exception on the same conditions,
     *                 i.e. 'invalid token')
     * @param cause exception that generates this exception
     * @return service exception
     */
    private static ServiceException newException(ErrorCode error, Object description, Json additionalInfo, boolean permanent, Throwable cause){
        String finalDescription = "Exception";
        ErrorCode finalErrorCode = ErrorCode.GENERAL;

        if(description instanceof ServiceException){
            finalErrorCode = ((ServiceException) description).getCode();
            finalDescription = ((ServiceException) description).getMessage();
        } else {
            if(error != null) {
                finalErrorCode = error;
            }
            if(description instanceof Exception){
                if(StringUtils.isNotBlank(((Exception) description).getMessage())) {
                    finalDescription = ((Exception) description).getMessage();
                } else {
                    finalDescription = description.toString();
                }
            } else if (description != null){
                final String d = description.toString();
                if(StringUtils.isNotBlank(d)){
                    finalDescription = d;
                }
            }
        }

        additionalInfo = additionalInfo != null ? additionalInfo : Json.map();
        if(cause != null){
            if (cause instanceof WebApplicationException) {
                // we try to find more information about the issue in the response
                final WebApplicationException wae = (WebApplicationException) cause;
                final Response r = wae.getResponse();
                if (r.hasEntity()) {
                    final String entity = r.readEntity(String.class);
                    if (r.getHeaderString(Parameter.CONTENT_TYPE) != null && r.getHeaderString(Parameter.CONTENT_TYPE).contains(ContentType.APPLICATION_JSON.getMimeType())) {
                        try {
                            additionalInfo.set("details", Json.parse(entity));
                        } catch (Exception e) {
                            logger.warn(String.format("There was a problem trying to parse error body [%s] - exception: %s", entity, e.getMessage()), e);
                        }
                    }

                    if(StringUtils.isBlank(additionalInfo.string("details"))){
                        additionalInfo.set("details", entity);
                    }
                }
                Json headers = Json.map();
                if(r.getHeaders() != null && !r.getHeaders().isEmpty()){
                    for (String k : r.getHeaders().keySet()) {
                        headers.set(k, r.getHeaderString(k));
                    }
                }
                additionalInfo.setIfNotEmpty("headers", headers);
            } else if (cause instanceof ProcessingException) {
                additionalInfo.set("causeMessage", getProcessingExceptionMessage((ProcessingException) cause));
            } else if (cause instanceof SAXException) {
                final SAXException saxException = (SAXException) cause;
                additionalInfo.set("causeMessage",  saxException.getMessage());

                if(saxException instanceof SAXParseException) {
                    final SAXParseException saxParseException = (SAXParseException) saxException;

                    additionalInfo.setIfNotEmpty("columnNumber", saxParseException.getColumnNumber());
                    additionalInfo.setIfNotEmpty("lineNumber", saxParseException.getLineNumber());
                    additionalInfo.setIfNotEmpty("publicId", saxParseException.getPublicId());
                    additionalInfo.setIfNotEmpty("systemId", saxParseException.getSystemId());
                }
            } else if (cause instanceof InvocationTargetException){
                return newException(error, description, additionalInfo, permanent, ((InvocationTargetException) cause).getTargetException());
            } else if (cause instanceof ServiceException){
                additionalInfo.set("causeMessage", cause.getMessage());
                additionalInfo.set("cause", ((ServiceException) cause).toJson(false).json(Parameter.EXCEPTION_ADDITIONAL_INFO));
            } else {
                additionalInfo.set("causeMessage", cause.getMessage());
                additionalInfo.set("causeType", cause.getClass().getCanonicalName());
            }
        }

        if(permanent) {
            return new PermanentException(finalErrorCode, finalDescription, additionalInfo, cause);
        } else {
            return new RetryableException(finalErrorCode, finalDescription, additionalInfo, cause);
        }
    }

    /**
     * Exceptions is converted to a Json object
     *
     * @return Json object
     */
    @Override
    public Json toJson(){
        return toJson(true);
    }

    /**
     * Exceptions is converted to a Json object
     *
     * @param includeFlag true if the flag must be included on the Json
     * @return Json object
     */
    public Json toJson(boolean includeFlag){
        final Json json = Json.map()
                .setIfNotEmpty(Parameter.EXCEPTION_CODE, code.toJson())
                .setIfNotEmpty(Parameter.EXCEPTION_MESSAGE, getMessage())
                .setIfNotEmpty(Parameter.EXCEPTION_ADDITIONAL_INFO, additionalInfo);

        if(includeFlag){
            json.set(Parameter.EXCEPTION_FLAG, true);
            json.set(Parameter.EXCEPTION_RETRYABLE, isRetryable());
        }
        return json;
    }

    /**
     * Exceptions is converted to a Json string
     *
     * @param includeFlag true if the flag must be included on the Json
     * @return Json string
     */
    public String getJson(boolean includeFlag){
        return toJson(includeFlag).toString();
    }

    /**
     * Exceptions is converted to a Json map
     *
     * @return Json map
     */
    public Map<String, Object> toMap() {
        return toJson(true).toMap();
    }

    /**
     * Exceptions is converted to a Json map
     *
     * @param includeFlag true if the flag must be included on the Json
     * @return Json map
     */
    public Map<String, Object> toMap(boolean includeFlag) {
        return toJson(includeFlag).toMap();
    }

    @Override
    public String toString() {
        return getJson(false);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ServiceException && toJson(true).equals(((ServiceException) o).toJson(true));
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Generates a ServiceException from a HTTP exception
     *
     * @param exception HTTP exception
     * @param detailMessage message
     * @param code HTTP error code
     * @return service exception
     */
    public static ServiceException parseHTTPExceptions(Exception exception, String detailMessage, String code) {
        if(exception instanceof ServiceException) {
            final Json ex = ((ServiceException) exception).toJson(true);
            if ((ex.string(PARSE_MESSAGE).startsWith("HTTP ") || ex.string(PARSE_MESSAGE).startsWith(REST_CODE_EXCEPTION)) && ex.contains(PARSE_ADDITIONAL_INFO)) {
                final Json ai = ex.json(PARSE_ADDITIONAL_INFO);
                Object odt = ai.object(PARSE_DETAILS);
                if(odt == null){
                    odt = ai.object(PARSE_BODY);
                }
                if (odt != null) {
                    try {
                        final Json dt = Json.fromObject(odt);

                        if(dt.contains(detailMessage) && StringUtils.isNotBlank(dt.string(detailMessage))){
                            ai.set(PARSE_ORIGINAL_MESSAGE, ex.string(PARSE_MESSAGE));

                            final String msg;
                            if(dt.contains(code) && StringUtils.isNotBlank(dt.string(code))){
                                msg = String.format("%s [code: %s]", dt.string(detailMessage), dt.string(code));
                            } else {
                                msg = dt.string(detailMessage);
                            }

                            return new PermanentException(((ServiceException) exception).getCode(), msg, ai, exception);
                        }
                    } catch (Exception pdEx){
                        ai.set(PARSE_ORIGINAL_MESSAGE, ex.string(PARSE_MESSAGE));

                        final String msg = ex.string(PARSE_MESSAGE).replaceFirst("HTTP\\s+\\d+\\s*", "");
                        return new PermanentException(((ServiceException) exception).getCode(), msg, ai, exception);
                    }
                }
            }
            return (ServiceException) exception;
        } else {
            return permanent(ErrorCode.API, exception.getMessage(), exception);
        }
    }

    /**
     * Extracts from a ProcessingException the message
     *
     * @param cause exception
     * @return error message
     */
    public static String getProcessingExceptionMessage(ProcessingException cause) {
        Throwable processingException = cause;
        String cm = processingException.getMessage();
        String lastMessage = cm != null ? cm : "";
        int tries = 10;
        while(tries > 0 && (cm == null || cm.startsWith("org.apache.http")) && processingException.getCause() != null) {
            processingException = processingException.getCause();
            cm = processingException.getMessage();
            if(StringUtils.isNotBlank(cm)){
                lastMessage = cm;
            }
            tries--;
        }
        return lastMessage;
    }
}

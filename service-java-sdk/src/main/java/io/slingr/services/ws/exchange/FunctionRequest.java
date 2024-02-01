package io.slingr.services.ws.exchange;

import io.slingr.services.services.exchange.Parameter;
import io.slingr.services.utils.Json;
import io.slingr.services.utils.converters.JsonSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Function request received from the Extension Broker and used to be executed on service
 *
 */
public class FunctionRequest implements JsonSource {
    private static final Logger logger = LoggerFactory.getLogger(FunctionRequest.class);

    private final Json request;
    private final boolean redelivered;
    private final int redeliveredCounter;
    private final int redeliveredMaxCounter;

    /**
     * Instances the function request using the Json request received from the Extension Broker app
     *
     * @param request request received from the Extension Broker app
     */
    public FunctionRequest(Object request) {
        this(request, false, 1, 1);
    }

    /**
     * Instances the function request using the Json request received from the Extension Broker app
     *
     * @param request request received from the Extension Broker app
     * @param redelivered true if the request is a retry from a previous failed one.
     * @param redeliveredCounter number of retry
     * @param redeliveredMaxCounter max number of retries
     */
    public FunctionRequest(Object request, boolean redelivered, int redeliveredCounter, int redeliveredMaxCounter) {
        Json body = null;
        if(request != null){
            body = convertRequest(request, false);
            if (body != null && body.isMap() && body.contains(Parameter.REQUEST_WRAPPED)) {
                body = convertRequest(body.object(Parameter.REQUEST_WRAPPED), true);
            }
        }
        if(body == null){
            body = Json.map();
        }
        this.request = body;
        this.redelivered = redelivered;
        this.redeliveredCounter = redeliveredCounter < 0 ? 0 : redeliveredCounter;
        this.redeliveredMaxCounter = redeliveredMaxCounter < 0 ? 0 : redeliveredMaxCounter;
    }

    /**
     * Helper to convert the request object in a {@link Json} object
     *
     * @param request request to convert
     * @param isWrapped true if the request is the content included on a {@code Parameter.REQUEST_WRAPPED} parameter
     * @return the converted request
     */
    private static Json convertRequest(Object request, boolean isWrapped) {
        Json body = null;
        if (request instanceof Json) {
            body = (Json) request;
        } else if (request instanceof JsonSource) {
            body = ((JsonSource) request).toJson();
        } else {
            try {
                body = Json.fromObject(request);
            } catch (Exception ex) {
                if(isWrapped) {
                    logger.info(String.format("Wrapped request of function is not a Json object [%s]: %s", request.toString(), ex.getMessage()));
                } else {
                    logger.info(String.format("Body of function request is not a Json object [%s]: %s", request.toString(), ex.getMessage()));
                }
            }
        }
        return body;
    }

    /**
     * Gets the internal request object
     *
     * @return internal request object
     */
    public Json getRequest() {
        return request;
    }

    /**
     * Gets the information related to the function that the application sent to the service as parameters of the
     * function
     *
     * @return parameters of function
     */
    public Object getParams() {
        return request.object(Parameter.PARAMS);
    }

    /**
     * Gets the information related to the function that the application sent to the service as parameters of the
     * function
     *
     * @return parameters of function
     */
    public Json getJsonParams() {
        final Object bd = getParams();
        if(bd == null){
            return Json.map();
        } else if(bd instanceof Json){
            return (Json) bd;
        }
        return Json.fromObject(bd);
    }

    /**
     * Gets the timestamp that represents the moment when the function request was generated.  It takes the value of
     * the milliseconds from ‘01/01/1970 12:00 AM’. Per example 1465928711524 is ‘06/14/2016 6:25:11 PM’
     *
     * @return timestamp that represents the moment when the function request was generated.
     */
    public long getDate() {
        Long value = request.longInteger(Parameter.DATE);
        if(value == null){
            value = 0L;
        }
        return value;
    }

    /**
     * Gets the name of the function. This must be a valid function name (this must be declared on the appService.json file)
     *
     * @return name of the function
     */
    public String getFunctionName() {
        return request.string(Parameter.FUNCTION_NAME);
    }

    /**
     * Gets the id of a function. The service must be sent this id on the function response message and in all the
     * events related to this function call (id is sent on the fromFunction parameter of the event message)
     *
     * @return id of a function
     */
    public String getFunctionId() {
        return request.string(Parameter.FUNCTION_ID);
    }

    /**
     * Gets the id of the user that calls to the function. This is the preferred method for identifying a user.
     *
     * @return id of the user
     */
    public String getUserId() {
        return request.string(Parameter.USER_ID);
    }

    /**
     * Gets the email of the user that calls to the function. This is an alternative method for identifying a user.
     * The application will send both if knows both (id and email).
     *
     * @return email of the user
     */
    public String getUserEmail() {
        return request.string(Parameter.USER_EMAIL);
    }

    /**
     * Returns true if the request is a retry from a previous failed one.
     *
     * @return true if the request is a retry from a previous failed one.
     */
    public boolean isRedelivered() {
        return redelivered;
    }

    /**
     * Gets the number of retry
     *
     * @return number of retry
     */
    public int getRedeliveredCounter() {
        return redeliveredCounter;
    }

    /**
     * Gets the max number of retries
     *
     * @return max number of retries
     */
    public int getRedeliveredMaxCounter() {
        return redeliveredMaxCounter;
    }

    /**
     * Gets the current app. This is used in shared services.
     *
     * @return name of the app
     */
    public String getApp() {
        return request.string(Parameter.APP);
    }

    /**
     * Gets the current environment. This is used in shared services.
     *
     * @return name of the environment
     */
    public String getEnv() {
        return request.string(Parameter.ENV);
    }

    @Override
    public Json toJson(){
        return Json.map()
                .set(Parameter.DATE, getDate())
                .setIfNotEmpty(Parameter.FUNCTION_NAME, getFunctionName())
                .setIfNotEmpty(Parameter.FUNCTION_ID, getFunctionId())
                .setIfNotEmpty(Parameter.PARAMS, getParams())
                .setIfNotEmpty(Parameter.USER_ID, getUserId())
                .setIfNotEmpty(Parameter.USER_EMAIL, getUserEmail());
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}